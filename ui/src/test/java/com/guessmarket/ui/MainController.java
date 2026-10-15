package com.guessmarket.ui;




import com.guessmarket.engine.dto.*;
import com.guessmarket.ui.network.HttpClientAdapter;




import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;




import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;




import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    
    
    
    @FXML private Label filePathLabel;
    @FXML private Button loadXmlButton;

    
    
    
    @FXML private ComboBox<String> filterEventMethodComboBox;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private ComboBox<String> filterCommissionComboBox;

    @FXML private TableView<EventSummaryDTO> eventsTableView;
    @FXML private TableColumn<EventSummaryDTO, String> colEventId;
    @FXML private TableColumn<EventSummaryDTO, String> colEventName;
    @FXML private TableColumn<EventSummaryDTO, String> colEventStatus;
    @FXML private TableColumn<EventSummaryDTO, String> colMarketMaker;

    @FXML private Label lblOption1OB;
    @FXML private TableView<OutcomeDTO> option1OrderBookTableView;
    @FXML private TableColumn<OutcomeDTO, Double> opt1BidCol;
    @FXML private TableColumn<OutcomeDTO, Double> opt1AskCol;
    @FXML private TableColumn<OutcomeDTO, Double> opt1LastCol;
    @FXML private TableColumn<OutcomeDTO, Double> opt1MidCol;
    @FXML private TableColumn<OutcomeDTO, Double> opt1SpreadCol;

    @FXML private Label lblOption2OB;
    @FXML private TableView<OutcomeDTO> option2OrderBookTableView;
    @FXML private TableColumn<OutcomeDTO, Double> opt2BidCol;
    @FXML private TableColumn<OutcomeDTO, Double> opt2AskCol;
    @FXML private TableColumn<OutcomeDTO, Double> opt2LastCol;
    @FXML private TableColumn<OutcomeDTO, Double> opt2MidCol;
    @FXML private TableColumn<OutcomeDTO, Double> opt2SpreadCol;

    @FXML private TableView<ParticipationDTO> participationsInfoTableView;

    
    
    
    @FXML private TableView<UserDTO> usersTableView;
    @FXML private TableColumn<UserDTO, String> colUserName;
    @FXML private TableColumn<UserDTO, String> colUserBalance;

    @FXML private Label accountBalanceLabel;
    @FXML private Button btnLoadFunds;

    @FXML private TableView<ParticipationDTO> eventsParticipationTableView;
    @FXML private TableColumn<ParticipationDTO, String> colParticipationEventName;

    @FXML private VBox singleEventTradePanel;
    @FXML private Label lblActiveUser;
    @FXML private Label lblActiveEvent;

    @FXML private TableView<OutcomeDTO> outcomesTableView;
    @FXML private TableColumn<OutcomeDTO, String> colOutcomeDesc;
    @FXML private TableColumn<OutcomeDTO, String> colOutcomePrice;

    @FXML private TextField txtSharesAmount;
    @FXML private Button btnExecuteTrade;

    @FXML private HBox mmControlsBox;
    @FXML private Button btnStartEvent;
    @FXML private Button btnCloseEvent;

    
    
    
    private String currentUsername = "SystemUser";
    private ScheduledExecutorService pollingExecutor;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    private ObservableList<EventSummaryDTO> masterEventData;
    private FilteredList<EventSummaryDTO> filteredEvents;
    private ObservableList<OutcomeDTO> option1OutcomesData;
    private ObservableList<OutcomeDTO> option2OutcomesData;
    private ObservableList<OutcomeDTO> activeOutcomesData;
    private ObservableList<UserDTO> usersData;
    private ObservableList<ParticipationDTO> userParticipationsData;
    private ObservableList<ParticipationDTO> eventParticipationsData;

    private EventSummaryDTO selectedEvent;
    private UserDTO selectedUser;
    private ServerStateWrapper lastKnownState;

    
    
    

    @FXML
    public void initialize() {
        masterEventData = FXCollections.observableArrayList();
        filteredEvents = new FilteredList<>(masterEventData, p -> true);

        option1OutcomesData = FXCollections.observableArrayList();
        option2OutcomesData = FXCollections.observableArrayList();
        activeOutcomesData = FXCollections.observableArrayList();
        usersData = FXCollections.observableArrayList();
        userParticipationsData = FXCollections.observableArrayList();
        eventParticipationsData = FXCollections.observableArrayList();

        setupEventsTable();
        setupUsersTable();
        setupOutcomesTable();
        setupParticipationsTables();
        setupOrderBookTables();

        if (filterStatusComboBox != null) {
            filterStatusComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }

    public void initData(String username) {
        this.currentUsername = username;
        if (lblActiveUser != null) {
            lblActiveUser.setText(username);
        }
        startPolling();
    }

    private void startPolling() {
        pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        pollingExecutor.scheduleAtFixedRate(() -> {
            HttpClientAdapter.getInstance().getEvents()
                    .thenAccept(jsonResponse -> Platform.runLater(() -> processServerState(jsonResponse)))
                    .exceptionally(ex -> {
                        LOGGER.log(Level.WARNING, "Polling interval failed: " + ex.getMessage());
                        return null;
                    });
        }, 0, 1500, TimeUnit.MILLISECONDS);
    }

    private void processServerState(String jsonResponse) {
        try {
            ServerStateWrapper state = gson.fromJson(jsonResponse, ServerStateWrapper.class);
            this.lastKnownState = state;

            if (state.events != null) {
                String selectedEventId = selectedEvent != null ? selectedEvent.eventId() : null;
                masterEventData.setAll(state.events);

                if (selectedEventId != null) {
                    masterEventData.stream()
                            .filter(e -> e.eventId().equals(selectedEventId))
                            .findFirst()
                            .ifPresent(e -> eventsTableView.getSelectionModel().select(e));
                }
            }

            if (state.users != null) {
                String selectedUserName = selectedUser != null ? selectedUser.getUsername() : null;
                usersData.setAll(state.users);

                if (selectedUserName != null) {
                    usersData.stream()
                            .filter(u -> u.getUsername().equals(selectedUserName))
                            .findFirst()
                            .ifPresent(u -> usersTableView.getSelectionModel().select(u));
                }
            }

            if (selectedEvent != null) {
                loadEventOrderBooksAndOutcomes(selectedEvent.eventId());
                loadEventParticipations(selectedEvent.eventId());
            }

            if (selectedUser != null) {
                loadUserParticipations(selectedUser.getUsername());
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse JSON server state: " + e.getMessage());
        }
    }

    
    
    

    private void setupEventsTable() {
        if (eventsTableView == null) return;
        eventsTableView.setItems(filteredEvents);

        if (colEventId != null) colEventId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventId()));
        if (colEventName != null) colEventName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventName()));
        if (colEventStatus != null) colEventStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status()));
        if (colMarketMaker != null) colMarketMaker.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().marketMakerName()));

        eventsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            this.selectedEvent = newVal;
            onSelectionStateChanged();
        });
    }

    private void setupUsersTable() {
        if (usersTableView == null) return;
        usersTableView.setItems(usersData);

        if (colUserName != null) colUserName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        if (colUserBalance != null) colUserBalance.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getBalance())));

        usersTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            this.selectedUser = newVal;
            if (newVal != null && accountBalanceLabel != null) {
                accountBalanceLabel.setText(String.format("$%.2f", newVal.getBalance()));
            }
            onSelectionStateChanged();
        });
    }

    private void setupOutcomesTable() {
        if (outcomesTableView == null) return;

        outcomesTableView.setItems(activeOutcomesData);
        outcomesTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        if (colOutcomeDesc != null) {
            colOutcomeDesc.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getDescription())
            );
        }

        if (colOutcomePrice != null) {
            colOutcomePrice.setCellValueFactory(cell ->
                    new SimpleStringProperty(String.format("$%.2f", cell.getValue().getCurrentPrice()))
            );
        }
    }

    @SuppressWarnings("unchecked")
    private void setupParticipationsTables() {
        if (eventsParticipationTableView != null) {
            eventsParticipationTableView.setItems(userParticipationsData);
            TableColumn<ParticipationDTO, String> statusCol = (TableColumn<ParticipationDTO, String>) eventsParticipationTableView.getColumns().get(1);

            if (colParticipationEventName != null) {
                colParticipationEventName.setCellValueFactory(cell -> {
                    ParticipationDTO part = cell.getValue();
                    String displayTitle = (part.eventName() != null && !part.eventName().trim().isEmpty()) ? part.eventName() : "Event ID: " + part.eventId();
                    return new SimpleStringProperty(displayTitle);
                });
            }
            statusCol.setCellValueFactory(cell -> {
                ParticipationDTO part = cell.getValue();
                String outcomeDisplay = (part.outcomeName() != null && !part.outcomeName().trim().isEmpty()) ? part.outcomeName() : part.outcomeId();
                return new SimpleStringProperty(part.quantityPurchased() + " x " + outcomeDisplay);
            });
        }

        if (participationsInfoTableView != null) {
            participationsInfoTableView.setItems(eventParticipationsData);
            TableColumn<ParticipationDTO, String> userCol = (TableColumn<ParticipationDTO, String>) participationsInfoTableView.getColumns().get(0);
            TableColumn<ParticipationDTO, String> optionCol = (TableColumn<ParticipationDTO, String>) participationsInfoTableView.getColumns().get(1);

            userCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().username()));
            optionCol.setCellValueFactory(cell -> {
                ParticipationDTO part = cell.getValue();
                String outcomeDisplay = (part.outcomeName() != null && !part.outcomeName().trim().isEmpty()) ? part.outcomeName() : part.outcomeId();
                return new SimpleStringProperty(outcomeDisplay + " (" + part.quantityPurchased() + ")");
            });
        }
    }

    private void setupOrderBookTables() {
        if (option1OrderBookTableView != null) {
            option1OrderBookTableView.setItems(option1OutcomesData);
            if (opt1BidCol != null) opt1BidCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().bidPrice()).asObject());
            if (opt1AskCol != null) opt1AskCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().askPrice()).asObject());
            if (opt1LastCol != null) opt1LastCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().lastPrice()).asObject());
            if (opt1MidCol != null) opt1MidCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().midPrice()).asObject());
            if (opt1SpreadCol != null) opt1SpreadCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().spread()).asObject());
            formatCurrencyColumn(opt1BidCol, opt1AskCol, opt1LastCol, opt1MidCol, opt1SpreadCol);
        }

        if (option2OrderBookTableView != null) {
            option2OrderBookTableView.setItems(option2OutcomesData);
            if (opt2BidCol != null) opt2BidCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().bidPrice()).asObject());
            if (opt2AskCol != null) opt2AskCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().askPrice()).asObject());
            if (opt2LastCol != null) opt2LastCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().lastPrice()).asObject());
            if (opt2MidCol != null) opt2MidCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().midPrice()).asObject());
            if (opt2SpreadCol != null) opt2SpreadCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().spread()).asObject());
            formatCurrencyColumn(opt2BidCol, opt2AskCol, opt2LastCol, opt2MidCol, opt2SpreadCol);
        }
    }

    @SafeVarargs
    private final void formatCurrencyColumn(TableColumn<OutcomeDTO, Double>... columns) {
        for (TableColumn<OutcomeDTO, Double> column : columns) {
            if (column == null) continue;
            column.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(Double price, boolean empty) {
                    super.updateItem(price, empty);
                    if (empty || price == null) {
                        setText(null);
                    } else {
                        setText(String.format("$%.2f", price));
                    }
                }
            });
        }
    }

    private void onSelectionStateChanged() {
        boolean isUserSelected = (selectedUser != null);
        boolean isEventSelected = (selectedEvent != null);

        String activeUserContext = isUserSelected ? selectedUser.getUsername() : currentUsername;

        if (lblActiveUser != null) lblActiveUser.setText(activeUserContext);
        if (lblActiveEvent != null) lblActiveEvent.setText(isEventSelected ? selectedEvent.eventName() : "None");

        if (isEventSelected) {
            loadEventOrderBooksAndOutcomes(selectedEvent.eventId());
            loadEventParticipations(selectedEvent.eventId());
        } else {
            activeOutcomesData.clear();
            eventParticipationsData.clear();
        }

        if (isUserSelected) {
            loadUserParticipations(selectedUser.getUsername());
        }

        if (singleEventTradePanel != null) {
            if (isEventSelected) {
                singleEventTradePanel.setDisable(false);
                if (outcomesTableView != null) outcomesTableView.setDisable(false);

                boolean isOpen = "ACTIVE".equalsIgnoreCase(selectedEvent.status());

                boolean isMarketMaker = selectedEvent.marketMakerName() != null
                        && selectedEvent.marketMakerName().equalsIgnoreCase(activeUserContext);

                if (btnExecuteTrade != null) {
                    if (!isOpen) {
                        btnExecuteTrade.setDisable(true);
                        btnExecuteTrade.setText("Event is Closed");
                    } else {
                        btnExecuteTrade.setDisable(false);
                        btnExecuteTrade.setText("Submit Market Order");
                    }
                }

                if (mmControlsBox != null) {
                    mmControlsBox.setVisible(true);
                    mmControlsBox.setManaged(true);

                    if (btnStartEvent != null) {
                        btnStartEvent.setDisable(isOpen);
                    }

                    if (btnCloseEvent != null) {
                        btnCloseEvent.setDisable(!(isOpen && isMarketMaker));
                    }
                }
            } else {
                singleEventTradePanel.setDisable(true);
            }
        }
    }

    
    
    

    private void loadEventOrderBooksAndOutcomes(String eventId) {
        if (lastKnownState == null || selectedEvent == null) return;

        OutcomeDTO selectedOutcome = outcomesTableView.getSelectionModel().getSelectedItem();
        String activeSelectionId = (selectedOutcome != null) ? selectedOutcome.getOutcomeId() : null;

        activeOutcomesData.clear();
        option1OutcomesData.clear();
        option2OutcomesData.clear();

        if (lastKnownState.outcomes != null) {
            List<OutcomeDTO> relevantOutcomes = lastKnownState.outcomes.stream()
                    .filter(outcome -> outcome != null && eventId.equals(outcome.getEventId()))
                    .toList();

            activeOutcomesData.setAll(relevantOutcomes);

            if (activeSelectionId != null) {
                for (OutcomeDTO outcome : activeOutcomesData) {
                    if (activeSelectionId.equals(outcome.getOutcomeId())) {
                        outcomesTableView.getSelectionModel().select(outcome);
                        break;
                    }
                }
            }

            if (relevantOutcomes.size() > 0) option1OutcomesData.add(relevantOutcomes.get(0));
            if (relevantOutcomes.size() > 1) option2OutcomesData.add(relevantOutcomes.get(1));
        }
    }

    private void loadUserParticipations(String userId) {
        if (lastKnownState == null || lastKnownState.participations == null) return;

        List<ParticipationDTO> userParts = lastKnownState.participations.stream()
                .filter(p -> p.username() != null && p.username().equals(userId))
                .toList();
        userParticipationsData.setAll(userParts);
    }

    private void loadEventParticipations(String eventId) {
        if (lastKnownState == null || lastKnownState.participations == null) return;

        List<ParticipationDTO> eventParts = lastKnownState.participations.stream()
                .filter(p -> p.eventId() != null && p.eventId().equals(eventId))
                .toList();
        eventParticipationsData.setAll(eventParts);
    }

    
    
    

    @FXML
    private void handleLoadFunds(ActionEvent event) {
        if (selectedUser == null && currentUsername == null) {
            showAlert(Alert.AlertType.WARNING, "Context Error", "Cannot load funds without an active user context.");
            return;
        }

        String targetUser = (selectedUser != null) ? selectedUser.getUsername() : currentUsername;
        showAlert(Alert.AlertType.INFORMATION, "Load Funds Stub", "Fund injection interface for " + targetUser + " would launch here.");
    }

    @FXML
    private void handleTradeExecution(ActionEvent event) {
        String activeUserContext = (selectedUser != null) ? selectedUser.getUsername() : currentUsername;

        if (selectedEvent == null || activeUserContext == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please ensure an Event and User are selected.");
            return;
        }

        OutcomeDTO selectedOutcome = outcomesTableView.getSelectionModel().getSelectedItem();
        if (selectedOutcome == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select an outcome from the table.");
            return;
        }

        try {
            double quantity = Double.parseDouble(txtSharesAmount.getText().trim());
            btnExecuteTrade.setDisable(true);

            HttpClientAdapter.getInstance().trade(selectedEvent.eventId(), activeUserContext, selectedOutcome.getOutcomeId(), quantity)
                    .thenAccept(response -> Platform.runLater(() -> {
                        btnExecuteTrade.setDisable(false);
                        txtSharesAmount.clear();
                        showAlert(Alert.AlertType.INFORMATION, "Trade Executed", "Transaction successful for " + activeUserContext + ".");
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            btnExecuteTrade.setDisable(false);
                            showAlert(Alert.AlertType.ERROR, "Execution Error", ex.getMessage());
                        });
                        return null;
                    });

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Quantity must be a valid number.");
        }
    }

    @FXML
    private void handleStartEvent(ActionEvent event) {
        if (selectedEvent == null) return;

        String activeUserContext = (selectedUser != null) ? selectedUser.getUsername() : currentUsername;

        HttpClientAdapter.getInstance().startEvent(selectedEvent.eventId(), activeUserContext)
                .thenAccept(response -> Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Event Started", "Event is now active. You are the Market Maker.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Server Error", ex.getMessage()));
                    return null;
                });
    }

    
    @FXML
    private void handleCloseEvent(ActionEvent event) {
        if (selectedEvent == null) return;

        String activeUserContext = (selectedUser != null) ? selectedUser.getUsername() : currentUsername;

        OutcomeDTO winningOutcome = outcomesTableView.getSelectionModel().getSelectedItem();
        if (winningOutcome == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Select a winning outcome first.");
            return;
        }

        
        HttpClientAdapter.getInstance().closeEvent(selectedEvent.eventId(), activeUserContext, winningOutcome.getOutcomeId())
                .thenAccept(response -> Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Event Closed", "Event closed successfully. Payouts processed.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Server Error", ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void handleLoadFile(ActionEvent event) {
        Window parentWindow = loadXmlButton.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files (*.xml)", "*.xml"));
        File selectedFile = fileChooser.showOpenDialog(parentWindow);

        if (selectedFile != null) {
            try {
                if (filePathLabel != null) filePathLabel.setText(selectedFile.getName());
                String xmlPayload = new String(Files.readAllBytes(selectedFile.toPath()), StandardCharsets.UTF_8);

                HttpClientAdapter.getInstance().uploadMarketData(xmlPayload, currentUsername)
                        .thenAccept(response -> Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Success", "XML Uploaded to Server.")))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Upload Failed", ex.getMessage()));
                            return null;
                        });
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "I/O Error", "Could not read local file: " + e.getMessage());
            }
        }
    }

    private void applyFilters() {
        filteredEvents.setPredicate(event -> filterStatusComboBox == null ||
                filterStatusComboBox.getValue() == null ||
                filterStatusComboBox.getValue().equals("All") ||
                event.status().equalsIgnoreCase(filterStatusComboBox.getValue()));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    public void shutdown() {
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            pollingExecutor.shutdownNow();
        }
    }

    public static class ServerStateWrapper {
        public List<EventSummaryDTO> events;
        public List<UserDTO> users;
        public List<OutcomeDTO> outcomes;
        public List<ParticipationDTO> participations;
    }
}