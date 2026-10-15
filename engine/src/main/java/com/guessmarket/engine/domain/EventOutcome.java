package com.guessmarket.engine.domain;

import java.util.Objects;

public class EventOutcome {

    private final String id;
    private final String description;
    private int sharesPurchased;

    public EventOutcome(String id, String description) {
        this.id = Objects.requireNonNull(id, "Outcome ID cannot be null.");
        this.description = description != null ? description : "";
        this.sharesPurchased = 0;
    }

    public EventOutcome(String id) {
        this(id, "");
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public int getSharesPurchased() { return sharesPurchased; }

    public synchronized void addShares(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity to add must be strictly positive.");
        this.sharesPurchased += quantity;
    }

    
    public synchronized void removeShares(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity to remove must be positive.");
        if (this.sharesPurchased - quantity < 0) throw new IllegalStateException("Cannot remove more shares than exist.");
        this.sharesPurchased -= quantity;
    }
}