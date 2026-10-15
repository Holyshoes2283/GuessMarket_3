package com.guessmarket.engine;

import com.guessmarket.engine.dto.*;
import com.guessmarket.engine.exception.*;
import com.guessmarket.engine.exceptions.InvalidDataLoadException;
import com.guessmarket.engine.loader.IMarketDataLoader;

import java.io.InputStream;
import java.util.List;


public interface IGuessMarketEngine {

    
    
    

    
    void loadEventsFromXml(InputStream xmlStream, String uploaderUsername) throws InvalidDataLoadException;

    
    void commitStagedData(IStagingMarketData data);

    IMarketDataLoader getDataLoader();

    
    
    
    
    UserDTO registerUser(String username);
    List<EventSummaryDTO> getAllEvents();
    List<UserDTO> getAllUsers();
    EventStatusDTO getEventStatus(String eventId) throws EventNotFoundException;
    List<ParticipationDTO> getEventParticipations(String eventId) throws EventNotFoundException;
    List<ParticipationDTO> getUserParticipations(String userId) throws UserNotFoundException;

    
    
    

    
    PurchaseReceiptDTO executeTrade(String eventId, String username, String action, String outcome, int amount)
            throws EventNotFoundException, InvalidPurchaseException;

    
    PurchaseReceiptDTO buyShares(String eventId, String username, String outcomeId, int quantity) throws EventNotFoundException, InvalidPurchaseException;

    
    void startEvent(String eventId, String username) throws EventNotFoundException, IllegalStateException;

    void submitLimitOrder(String eventId, String requestingUserId, String outcomeId, String orderType, int amount, double limitPrice)
            throws InvalidPurchaseException, EventNotFoundException;

    
    EventClosureDTO closeEvent(String eventId, String username, String winningOutcomeId) throws EventNotFoundException, EventAlreadyClosedException;

    
    
    

    void shutdown();
}