package com.guessmarket.engine.dto;

public record PurchaseReceiptDTO(String transactionId, String eventId, String outcomeId, int sharesBought, double totalCost, double newOutcomePrice) {}