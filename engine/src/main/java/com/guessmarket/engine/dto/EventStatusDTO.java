package com.guessmarket.engine.dto;

import java.util.List;


public record EventStatusDTO(
        String eventId,
        String title,
        String status,
        List<OutcomeDTO> outcomes,
        int totalSharesInEvent
) {
    
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }
}