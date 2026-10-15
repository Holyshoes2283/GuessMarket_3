package com.guessmarket.engine.strategy;

import com.guessmarket.engine.domain.MarketEvent;
import com.guessmarket.engine.dto.PurchaseReceiptDTO;
import com.guessmarket.engine.exception.InvalidPurchaseException;


public interface IMarketTradingStrategy {

    
    PurchaseReceiptDTO executeTrade(MarketEvent event, String outcomeId, int amount, double userBalance)
            throws InvalidPurchaseException;
}