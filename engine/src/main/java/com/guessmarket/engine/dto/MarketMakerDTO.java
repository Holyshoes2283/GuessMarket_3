package com.guessmarket.engine.dto;

import java.util.Objects;


public record MarketMakerDTO(String eventId) {

    
    public MarketMakerDTO {
        Objects.requireNonNull(eventId, "MarketMakerDTO cannot be instantiated with a null eventId.");
    }

    
    public String getEventId() {
        return this.eventId();
    }
}