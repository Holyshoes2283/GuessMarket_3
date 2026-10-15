package com.guessmarket.engine.dto;

import java.time.LocalDateTime;


public record ParticipationDTO(
        String transactionId,
        String eventId,
        String eventName,
        String outcomeId,
        String outcomeName,
        String username,
        int quantityPurchased,
        double totalCost,
        LocalDateTime timestamp
) {
    
    
    
}