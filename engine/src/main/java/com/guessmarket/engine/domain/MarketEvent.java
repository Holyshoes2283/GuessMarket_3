package com.guessmarket.engine.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class MarketEvent {
    private String marketMakerUsername = "SYSTEM"; 
    public String getMarketMakerUsername() {
        return marketMakerUsername;
    }

    public void setMarketMakerUsername(String marketMakerUsername) {
        this.marketMakerUsername = marketMakerUsername;
    }
    private final String id;
    private final String title;

    
    private final String marketMakerName;

    
    private boolean active;

    private final List<EventOutcome> outcomes;
    private String winningOutcomeId;

    private final double commission;
    private final double liquidityParameter;

    
    public MarketEvent(String id, String title, List<EventOutcome> outcomes, double commission, double liquidityParameter) {
        this(id, title, "SYSTEM", outcomes, commission, liquidityParameter);
    }

    
    public MarketEvent(String id, String title, String marketMakerName, List<EventOutcome> outcomes, double commission, double liquidityParameter) {
        this.id = Objects.requireNonNull(id, "Event ID cannot be null.");
        this.title = title != null ? title : "";

        
        this.marketMakerName = marketMakerName != null && !marketMakerName.trim().isEmpty()
                ? marketMakerName
                : "SYSTEM";

        if (commission < 0.0 || commission > 90.0) {
            throw new IllegalArgumentException("Commission (fee) must be strictly between 0 and 90.");
        }

        if (liquidityParameter <= 0) {
            throw new IllegalArgumentException("LMSR liquidity parameter 'b' must be strictly positive.");
        }

        this.commission = commission;
        this.liquidityParameter = liquidityParameter;
        this.outcomes = outcomes != null ? new ArrayList<>(outcomes) : new ArrayList<>();

        
        this.active = true;
    }

    
    
    

    public String getId() { return id; }

    public String getTitle() { return title; }

    
    public String getMarketMakerName() { return marketMakerName; }

    
    public boolean isActive() { return active; }

    public double getCommission() { return commission; }

    public double getLiquidityParameter() { return liquidityParameter; }

    public Optional<String> getWinningOutcomeId() { return Optional.ofNullable(winningOutcomeId); }

    public List<EventOutcome> getOutcomes() {
        return Collections.unmodifiableList(outcomes);
    }

    
    public EventOutcome getOutcome(String outcomeId) {
        return outcomes.stream()
                .filter(o -> o.getId().equals(outcomeId))
                .findFirst()
                .orElse(null);
    }

    public synchronized void addOutcome(EventOutcome outcome) {
        Objects.requireNonNull(outcome, "Cannot add a null outcome.");
        this.outcomes.add(outcome);
    }

    public synchronized void closeEvent(String outcomeId) {
        if (!this.active) {
            throw new IllegalStateException("Event is already closed.");
        }
        if (outcomes.stream().noneMatch(o -> o.getId().equals(outcomeId))) {
            throw new IllegalArgumentException("Invalid outcome ID: " + outcomeId + " does not exist in this event.");
        }
        this.active = false;
        this.winningOutcomeId = outcomeId;
    }

    public int getTotalSharesInEvent() {
        return outcomes.stream().mapToInt(EventOutcome::getSharesPurchased).sum();
    }

    public synchronized double calculateCurrentPrice(String outcomeId) {
        EventOutcome target = outcomes.stream()
                .filter(o -> o.getId().equals(outcomeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Outcome ID not found: " + outcomeId));

        double denominator = outcomes.stream()
                .mapToDouble(o -> Math.exp(o.getSharesPurchased() / this.liquidityParameter))
                .sum();

        return Math.exp(target.getSharesPurchased() / this.liquidityParameter) / denominator;
    }
}