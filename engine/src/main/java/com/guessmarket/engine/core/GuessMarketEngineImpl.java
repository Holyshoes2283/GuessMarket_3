package com.guessmarket.engine.core;

import com.guessmarket.engine.IGuessMarketEngine;
import com.guessmarket.engine.domain.Account;
import com.guessmarket.engine.domain.EventOutcome;
import com.guessmarket.engine.domain.MarketEvent;
import com.guessmarket.engine.dto.*;
import com.guessmarket.engine.exception.*;
import com.guessmarket.engine.exceptions.InvalidDataLoadException;
import com.guessmarket.engine.loader.IMarketDataLoader;
import com.guessmarket.engine.loader.IXmlParser;
import com.guessmarket.engine.loader.MarketDataLoaderImpl;
import com.guessmarket.engine.mapper.DomainMapper;
import com.guessmarket.engine.strategy.LmsrTradingStrategyImpl;
import com.guessmarket.engine.validation.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class GuessMarketEngineImpl implements IGuessMarketEngine {

    private static final Logger LOGGER = Logger.getLogger(GuessMarketEngineImpl.class.getName());

    
    protected final Map<String, MarketEvent> eventRepository = new ConcurrentHashMap<>();
    protected final Map<String, UserDTO> userRepository = new ConcurrentHashMap<>();
    protected final List<ParticipationDTO> participations = new CopyOnWriteArrayList<>();

    
    
    
    protected enum EventState {
        UNSTARTED, ACTIVE, FINISHED
    }

    protected final Map<String, EventState> eventLifecycleState = new ConcurrentHashMap<>();

    private volatile String activeSessionUserId = "SystemUser";
    protected Account primaryAccount;
    private final IMarketDataLoader dataLoader;

    public GuessMarketEngineImpl() {
        LOGGER.info("GuessMarketEngineImpl core module initializing...");

        List<IMarketDataValidator> validators = List.of(
                new UniqueUsernameValidator(),
                new PositiveInitialCashValidator(),
                new MarketMakerEventExistsValidator(),
                new EventHasOneMarketMakerValidator()
        );

        IXmlParser xmlParser = new GuessMarketXmlParser();
        this.dataLoader = new MarketDataLoaderImpl(this, validators, xmlParser);

        LOGGER.info("GuessMarketEngineImpl Phase 8 pipeline wired successfully.");
    }

    
    
    

    @Override
    public IMarketDataLoader getDataLoader() {
        return this.dataLoader;
    }

    public void loadMarketData(File file) throws Exception {
        if (file == null) throw new IllegalArgumentException("Market data file reference cannot be null.");
        this.dataLoader.loadMarketData(file);
    }

    @Override
    public void loadEventsFromXml(InputStream xmlStream, String uploaderUsername) throws InvalidDataLoadException {
        try {
            GuessMarketXmlParser parser = new GuessMarketXmlParser();
            IStagingMarketData stagingData = parser.parseToStaging(xmlStream, uploaderUsername);

            new UniqueUsernameValidator().validate(stagingData);
            new PositiveInitialCashValidator().validate(stagingData);
            new MarketMakerEventExistsValidator().validate(stagingData);
            new EventHasOneMarketMakerValidator().validate(stagingData);

            commitStagedData(stagingData);
        } catch (InvalidDataLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidDataLoadException("Legacy stream load failed: " + e.getMessage());
        }
    }

    public void loadEventsFromXml(InputStream xmlStream) throws InvalidDataLoadException {
        loadEventsFromXml(xmlStream, "SystemUser");
    }

    @Override
    public synchronized void commitStagedData(IStagingMarketData data) {
        if (data instanceof StagingPayload payload) {
            for (Map.Entry<String, MarketEvent> entry : payload.getDomainEvents().entrySet()) {
                this.eventRepository.putIfAbsent(entry.getKey(), entry.getValue());
                this.eventLifecycleState.putIfAbsent(entry.getKey(), EventState.UNSTARTED);
            }
            for (UserDTO user : payload.getUsers()) {
                this.userRepository.putIfAbsent(user.getUsername(), user);
            }
            LOGGER.info("Staging Area Commit Successful: Engine state updated incrementally.");
        } else {
            LOGGER.severe("Invalid Staging Payload provided to commit phase.");
        }
    }

    
    
    

    @Override
    public List<EventSummaryDTO> getAllEvents() {
        return eventRepository.values().stream().map(e -> {
            EventSummaryDTO baseDto = DomainMapper.toEventSummaryDTO(e);
            EventState state = eventLifecycleState.getOrDefault(e.getId(), EventState.UNSTARTED);
            String displayStatus = (state == EventState.ACTIVE) ? "ACTIVE" : "CLOSED";
            return new EventSummaryDTO(baseDto.eventId(), baseDto.eventName(), displayStatus, baseDto.marketMakerName());
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getAllUsers() {
        if (!userRepository.isEmpty()) return new ArrayList<>(userRepository.values());
        if (primaryAccount != null) return List.of(DomainMapper.toUserDTO(primaryAccount, "Primary User"));
        return new ArrayList<>();
    }

    @Override
    public EventStatusDTO getEventStatus(String eventId) throws EventNotFoundException {
        MarketEvent event = eventRepository.get(eventId);
        if (event == null) throw new EventNotFoundException("Not found: " + eventId);

        List<OutcomeDTO> outcomeDTOs = event.getOutcomes().stream()
                .map(o -> {
                    double currentPrice = event.calculateCurrentPrice(o.getId());
                    double midPrice = currentPrice;
                    double spreadPct = 0.02;
                    double bidPrice = Math.max(0.01, currentPrice * (1.0 - spreadPct / 2.0)); 
                    double askPrice = Math.min(0.99, currentPrice * (1.0 + spreadPct / 2.0));
                    double spread = askPrice - bidPrice;

                    double lastPrice = participations.stream()
                            .filter(p -> p.eventId().equals(eventId) && p.outcomeId().equals(o.getId()))
                            .reduce((first, second) -> second)
                            .map(p -> p.quantityPurchased() > 0 ? (p.totalCost() / p.quantityPurchased()) : currentPrice)
                            .orElse(currentPrice);

                    
                    return new OutcomeDTO(
                            eventId,               
                            o.getId(),             
                            o.getDescription(),    
                            currentPrice,          
                            o.getSharesPurchased(),
                            lastPrice,             
                            bidPrice,              
                            askPrice,              
                            midPrice,              
                            spread                 
                    );
                })
                .collect(Collectors.toList()); 

        EventState state = eventLifecycleState.getOrDefault(eventId, EventState.UNSTARTED);
        String displayStatus = (state == EventState.ACTIVE) ? "ACTIVE" : "CLOSED";

        return new EventStatusDTO(event.getId(), event.getTitle(), displayStatus, outcomeDTOs, event.getTotalSharesInEvent());
    }

    @Override
    public List<ParticipationDTO> getEventParticipations(String eventId) throws EventNotFoundException {
        if (!eventRepository.containsKey(eventId)) throw new EventNotFoundException("Not found: " + eventId);
        return participations.stream().filter(p -> p.eventId().equals(eventId)).collect(Collectors.toList());
    }

    @Override
    public List<ParticipationDTO> getUserParticipations(String userId) throws UserNotFoundException {
        this.activeSessionUserId = userId;
        boolean isPrimary = (primaryAccount != null && "Primary User".equals(userId));
        if (!userRepository.containsKey(userId) && !isPrimary) throw new UserNotFoundException("Not found: " + userId);
        return participations.stream().filter(p -> p.username().equals(userId)).collect(Collectors.toList());
    }

    
    
    

    @Override
    public synchronized void startEvent(String eventId, String username) throws EventNotFoundException, IllegalStateException {
        if (!eventRepository.containsKey(eventId)) {
            throw new EventNotFoundException("Engine Error: Event ID not found: " + eventId);
        }

        EventState currentState = eventLifecycleState.getOrDefault(eventId, EventState.UNSTARTED);

        if (currentState == EventState.ACTIVE) {
            throw new IllegalStateException("Cannot start event: It is already running.");
        }
        if (currentState == EventState.FINISHED) {
            throw new IllegalStateException("Cannot start event: It has already been closed and finished.");
        }

        eventLifecycleState.put(eventId, EventState.ACTIVE);
        LOGGER.info("Event " + eventId + " has been started and is now ACTIVE.");
    }

    @Override
    public synchronized EventClosureDTO closeEvent(String eventId, String username, String winningOutcomeId)
            throws EventNotFoundException, EventAlreadyClosedException {

        MarketEvent event = eventRepository.get(eventId);
        if (event == null) {
            throw new EventNotFoundException("Engine Error: Event ID not found: " + eventId);
        }

        
        
        
        if (event.getMarketMakerUsername() == null || !event.getMarketMakerUsername().equalsIgnoreCase(username)) {
            throw new IllegalStateException("Security Violation: Only the Market Maker ("
                    + event.getMarketMakerUsername() + ") has the authority to close this event.");
        }

        EventState currentState = eventLifecycleState.getOrDefault(eventId, EventState.UNSTARTED);

        if (currentState == EventState.UNSTARTED) {
            throw new IllegalStateException("Cannot close event: It was never started.");
        }
        if (currentState == EventState.FINISHED) {
            throw new EventAlreadyClosedException("Cannot close event: It is already permanently closed.");
        }

        eventLifecycleState.put(eventId, EventState.FINISHED);

        double totalPool = participations.stream()
                .filter(p -> p.eventId().equals(eventId))
                .mapToDouble(ParticipationDTO::totalCost)
                .sum();

        double commissionRate = 0.0;
        try {
            commissionRate = event.getCommission();
        } catch (Exception ignored) {
        }

        double commissionCut = totalPool * commissionRate;
        double netRewardPool = totalPool - commissionCut;

        List<ParticipationDTO> winningParticipations = participations.stream()
                .filter(p -> p.eventId().equals(eventId) && p.outcomeId().equals(winningOutcomeId))
                .collect(Collectors.toList());

        double totalWinningShares = winningParticipations.stream()
                .mapToDouble(ParticipationDTO::quantityPurchased)
                .sum();

        double payoutPerShare = 1.00;
        double totalWinnerPayouts = totalWinningShares * payoutPerShare;

        for (ParticipationDTO participation : winningParticipations) {
            String userId = participation.username();
            UserDTO user = userRepository.get(userId);
            if (user != null) {
                double userPayout = participation.quantityPurchased() * payoutPerShare;
                userRepository.put(userId, new UserDTO(userId, user.getBalance() + userPayout));
            }
        }

        String mmUsername = event.getMarketMakerUsername();
        if (mmUsername != null && userRepository.containsKey(mmUsername)) {
            UserDTO marketMaker = userRepository.get(mmUsername);
            double lmsrSurplus = netRewardPool - totalWinnerPayouts;
            double marketMakerTotalPayout = commissionCut + lmsrSurplus;

            userRepository.put(mmUsername, new UserDTO(mmUsername, marketMaker.getBalance() + marketMakerTotalPayout));
        }

        return new EventClosureDTO(event.getId(), winningOutcomeId, event.getTotalSharesInEvent(), true);
    }

    
    @Override
    public synchronized PurchaseReceiptDTO executeTrade(String eventId, String username, String action, String outcomeId, int quantity)
            throws EventNotFoundException, InvalidPurchaseException {

        LOGGER.info("[V3_ENGINE] Initializing trade execution...");
        String normalizedAction = action != null ? action.trim().toUpperCase() : "";

        MarketEvent event = eventRepository.get(eventId);
        if (event == null) {
            throw new EventNotFoundException("[V3_ENGINE] Target event not found: " + eventId);
        }

        
        String memoryOutcomes = event.getOutcomes().stream().map(EventOutcome::getId).collect(Collectors.joining(", "));
        LOGGER.info("[V3_ENGINE] Available outcomes in Engine memory for event '" + eventId + "': [" + memoryOutcomes + "]");

        
        String resolvedOutcomeId = event.getOutcomes().stream()
                .map(EventOutcome::getId)
                .filter(id -> id.equalsIgnoreCase(outcomeId != null ? outcomeId.trim() : ""))
                .findFirst()
                .orElseThrow(() -> new InvalidPurchaseException(
                        "[V3_ENGINE] The requested outcome '" + outcomeId + "' does not match any valid outcomes. Engine memory has exactly: [" + memoryOutcomes + "]"
                ));

        LOGGER.info("[V3_ENGINE] Successfully resolved outcome matching: " + resolvedOutcomeId);

        if ("BUY".equals(normalizedAction)) {
            return buyShares(eventId, username, resolvedOutcomeId, quantity);
        } else if ("SELL".equals(normalizedAction)) {
            submitLimitOrder(eventId, username, resolvedOutcomeId, "SELL", quantity, 1.0);
            return new PurchaseReceiptDTO(
                    UUID.randomUUID().toString(),
                    eventId,
                    resolvedOutcomeId,
                    quantity,
                    quantity * 1.0,
                    1.0
            );
        } else {
            throw new InvalidPurchaseException("[V3_ENGINE] Unsupported action: " + action);
        }
    }

    @Override
    public synchronized PurchaseReceiptDTO buyShares(String eventId, String username, String outcomeId, int quantity)
            throws EventNotFoundException, InvalidPurchaseException {

        MarketEvent event = eventRepository.get(eventId);
        if (event == null) throw new EventNotFoundException("Not found: " + eventId);

        EventState state = eventLifecycleState.getOrDefault(eventId, EventState.UNSTARTED);
        if (state != EventState.ACTIVE) {
            throw new InvalidPurchaseException("Cannot trade: Event is currently " +
                    (state == EventState.UNSTARTED ? "CLOSED (Not Started)" : "CLOSED (Finished)"));
        }

        String targetUserId = (username != null) ? username : this.activeSessionUserId;
        if (!userRepository.containsKey(targetUserId)) {
            targetUserId = userRepository.keySet().stream().findFirst().orElse("SystemUser");
        }

        UserDTO user = userRepository.getOrDefault(targetUserId, new UserDTO(targetUserId, 10000.0));

        LmsrTradingStrategyImpl strategy = new LmsrTradingStrategyImpl();
        PurchaseReceiptDTO receipt = strategy.executeTrade(event, outcomeId, quantity, user.getBalance());

        userRepository.put(targetUserId, new UserDTO(targetUserId, user.getBalance() - receipt.totalCost()));

        ParticipationDTO part = new ParticipationDTO(
                receipt.transactionId(), eventId, event.getTitle(), outcomeId, outcomeId,
                targetUserId, quantity, receipt.totalCost(), LocalDateTime.now()
        );
        participations.add(part);

        return receipt;
    }

    @Override
    public UserDTO registerUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty.");
        }

        String cleanUsername = username.trim().toLowerCase();

        
        if (this.userRepository.containsKey(cleanUsername)) {
            return this.userRepository.get(cleanUsername);
        }

        double defaultBalance = 10000.0;
        UserDTO newUser = new UserDTO(cleanUsername, defaultBalance);

        this.userRepository.put(cleanUsername, newUser);
        return newUser;
    }

    @Override
    public synchronized void submitLimitOrder(String eventId, String requestingUserId, String outcomeId, String orderType, int amount, double limitPrice)
            throws InvalidPurchaseException, EventNotFoundException {

        MarketEvent event = eventRepository.get(eventId);
        if (event == null) throw new EventNotFoundException("The specified event does not exist: " + eventId);

        EventState state = eventLifecycleState.getOrDefault(eventId, EventState.UNSTARTED);
        if (state != EventState.ACTIVE) {
            throw new InvalidPurchaseException("Cannot trade: Event is currently " +
                    (state == EventState.UNSTARTED ? "CLOSED (Not Started)" : "CLOSED (Finished)"));
        }

        UserDTO user = userRepository.get(requestingUserId);
        if (user == null) throw new InvalidPurchaseException("User not found: " + requestingUserId);

        double transactionCost = amount * limitPrice;
        if ("BUY".equalsIgnoreCase(orderType)) {
            if (user.getBalance() < transactionCost) {
                throw new InvalidPurchaseException("Insufficient funds. Required: $" + transactionCost);
            }
            userRepository.put(requestingUserId, new UserDTO(requestingUserId, user.getBalance() - transactionCost));
        } else {
            userRepository.put(requestingUserId, new UserDTO(requestingUserId, user.getBalance() + transactionCost));
        }

        EventOutcome targetOutcome = event.getOutcomes().stream()
                .filter(o -> o.getId().equals(outcomeId)).findFirst()
                .orElseThrow(() -> new InvalidPurchaseException("Outcome not found"));

        targetOutcome.addShares("BUY".equalsIgnoreCase(orderType) ? amount : -amount);

        ParticipationDTO part = new ParticipationDTO(
                UUID.randomUUID().toString(), eventId, event.getTitle(), outcomeId, outcomeId,
                requestingUserId, amount, transactionCost, LocalDateTime.now()
        );
        participations.add(part);
    }

    @Override
    public void shutdown() {
        eventRepository.clear();
        userRepository.clear();
        participations.clear();
        eventLifecycleState.clear();
        LOGGER.info("GuessMarketEngineImpl shutdown gracefully.");
    }

    
    
    

    private static class StagingPayload implements IStagingMarketData {
        private final List<UserDTO> users = new ArrayList<>();
        private final List<EventDTO> events = new ArrayList<>();
        private final List<MarketMakerDTO> marketMakers = new ArrayList<>();
        private final Map<String, MarketEvent> domainEvents = new LinkedHashMap<>();

        @Override
        public List<UserDTO> getUsers() {
            return users;
        }

        @Override
        public List<EventDTO> getEvents() {
            return events;
        }

        @Override
        public List<MarketMakerDTO> getMarketMakers() {
            return marketMakers;
        }

        public Map<String, MarketEvent> getDomainEvents() {
            return domainEvents;
        }
    }

    private class GuessMarketXmlParser implements IXmlParser {

        @Override
        public IStagingMarketData parseToStaging(File xmlFile) throws Exception {
            try (InputStream is = new FileInputStream(xmlFile)) {
                return parseToStaging(is, "SystemUser");
            }
        }

        @Override
        public IStagingMarketData parseToStaging(InputStream xmlStream, String uploaderUsername) throws Exception {
            String rawContent = new String(xmlStream.readAllBytes(), StandardCharsets.UTF_8);
            int rootIndex = rawContent.indexOf("<Guess-Market");

            if (rootIndex == -1) {
                throw new InvalidDataLoadException("Critical parsing error: Root element <Guess-Market> not found.");
            }

            String sanitizedContent = rawContent.substring(rootIndex);
            InputStream sanitizedStream = new java.io.ByteArrayInputStream(sanitizedContent.getBytes(StandardCharsets.UTF_8));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(sanitizedStream);
            document.getDocumentElement().normalize();

            StagingPayload payload = new StagingPayload();

            
            NodeList eventNodes = document.getElementsByTagName("GM-event");
            for (int i = 0; i < eventNodes.getLength(); i++) {
                Element eventElement = (Element) eventNodes.item(i);

                
                String eventName = eventElement.getAttribute("name");
                String eventId = eventName; 

                
                double commission = 0.0;
                NodeList commNodes = eventElement.getElementsByTagName("commission");
                if (commNodes.getLength() > 0) {
                    Element commElement = (Element) commNodes.item(0);
                    try {
                        
                        commission = Double.parseDouble(commElement.getTextContent().trim()) / 100.0;
                    } catch (NumberFormatException ignored) {
                        LOGGER.warning("Could not parse commission integer for event: " + eventName);
                    }
                }

                
                double bParameter = 100.0; 
                NodeList bNodes = eventElement.getElementsByTagName("b");
                if (bNodes.getLength() > 0) {
                    try {
                        bParameter = Double.parseDouble(bNodes.item(0).getTextContent().trim());
                    } catch (NumberFormatException ignored) {
                        LOGGER.warning("Could not parse LMSR b-parameter for event: " + eventName);
                    }
                }

                MarketEvent marketEvent = new MarketEvent(eventId, eventName, new ArrayList<>(), commission, bParameter);

                
                marketEvent.setMarketMakerUsername(uploaderUsername);

                
                NodeList optionNodes = eventElement.getElementsByTagName("GM-option");
                for (int j = 0; j < optionNodes.getLength(); j++) {
                    String outcomeId = optionNodes.item(j).getTextContent().trim();
                    marketEvent.addOutcome(new EventOutcome(outcomeId, outcomeId));
                }

                payload.domainEvents.put(eventId, marketEvent);
                payload.events.add(new EventDTO(eventId));
                payload.marketMakers.add(new MarketMakerDTO(eventId));
            }

            
            

            return payload;
        }

        
        private String getRequiredNodeText(Element parent, String tag, String fallback) {
            NodeList nodes = parent.getElementsByTagName(tag);
            if (nodes.getLength() > 0) {
                String text = nodes.item(0).getTextContent();
                if (text != null && !text.trim().isEmpty()) return text.trim();
            }
            return fallback;
        }
    }
}