package com.guessmarket.engine.dto;


public record OutcomeDTO(
        String eventId, 
        String outcomeId,
        String description,
        double currentPrice,
        int sharesPurchased,
        double lastPrice,
        double bidPrice,
        double askPrice,
        double midPrice,
        double spread
) {
    
    public String getEventId() { return eventId; }
    public String getOutcomeId() { return outcomeId; }
    public String getDescription() { return description; }
    public double getCurrentPrice() { return currentPrice; }
    public int getSharesPurchased() { return sharesPurchased; }
}