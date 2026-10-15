package com.guessmarket.engine.dto;


public record EventClosureDTO(

        
        String eventId,

        
        String winningOutcomeId,

        
        double payoutPerShare,

        
        boolean wasAlreadyClosed

) {}