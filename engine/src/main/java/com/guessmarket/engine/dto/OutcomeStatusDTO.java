package com.guessmarket.engine.dto;


public class OutcomeStatusDTO {

    private final String id;
    private final String description;
    private final double currentPrice;
    private final int sharesPurchased;

    
    public OutcomeStatusDTO(String id, String description, double currentPrice, int sharesPurchased) {
        this.id = id;
        this.description = description;
        this.currentPrice = currentPrice;
        this.sharesPurchased = sharesPurchased;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public int getSharesPurchased() {
        return sharesPurchased;
    }
}