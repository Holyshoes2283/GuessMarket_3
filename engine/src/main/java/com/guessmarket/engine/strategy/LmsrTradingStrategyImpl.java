package com.guessmarket.engine.strategy;

import com.guessmarket.engine.domain.EventOutcome;
import com.guessmarket.engine.domain.MarketEvent;
import com.guessmarket.engine.dto.PurchaseReceiptDTO;
import com.guessmarket.engine.exception.InvalidPurchaseException;

import java.util.UUID;


public class LmsrTradingStrategyImpl implements IMarketTradingStrategy {

    @Override
    public PurchaseReceiptDTO executeTrade(MarketEvent event, String outcomeId, int amount, double userBalance)
            throws InvalidPurchaseException {

        
        if (!event.isActive()) {
            throw new InvalidPurchaseException("Trade rejected. Event is currently closed.");
        }

        EventOutcome targetOutcome = event.getOutcomes().stream()
                .filter(o -> o.getId().equals(outcomeId))
                .findFirst()
                .orElseThrow(() -> new InvalidPurchaseException("Outcome not found in event."));

        
        synchronized (event) {
            double bParam = event.getLiquidityParameter();

            
            
            

            double maxCurrentQ = 0.0;
            double maxProjectedQ = 0.0;

            for (EventOutcome o : event.getOutcomes()) {
                double currentQ = o.getSharesPurchased();
                double projectedQ = currentQ + (o.getId().equals(outcomeId) ? amount : 0);

                if (currentQ > maxCurrentQ) maxCurrentQ = currentQ;
                if (projectedQ > maxProjectedQ) maxProjectedQ = projectedQ;
            }

            double currentExpSum = 0.0;
            double projectedExpSum = 0.0;

            
            for (EventOutcome o : event.getOutcomes()) {
                double currentQ = o.getSharesPurchased();
                double projectedQ = currentQ + (o.getId().equals(outcomeId) ? amount : 0);

                currentExpSum += Math.exp((currentQ - maxCurrentQ) / bParam);
                projectedExpSum += Math.exp((projectedQ - maxProjectedQ) / bParam);
            }

            
            double currentMarketCost = maxCurrentQ + (bParam * Math.log(currentExpSum));
            double projectedMarketCost = maxProjectedQ + (bParam * Math.log(projectedExpSum));

            
            double transactionCost = projectedMarketCost - currentMarketCost;

            
            if (userBalance < transactionCost) {
                throw new InvalidPurchaseException(
                        String.format("Insufficient funds. Required: $%.2f, Available: $%.2f", transactionCost, userBalance)
                );
            }

            
            targetOutcome.addShares(amount);

            
            double maxPostQ = 0.0;
            for (EventOutcome o : event.getOutcomes()) {
                if (o.getSharesPurchased() > maxPostQ) maxPostQ = o.getSharesPurchased();
            }

            double denominator = 0.0;
            for (EventOutcome o : event.getOutcomes()) {
                denominator += Math.exp((o.getSharesPurchased() - maxPostQ) / bParam);
            }

            double newMarginalPrice = Math.exp((targetOutcome.getSharesPurchased() - maxPostQ) / bParam) / denominator;

            
            return new PurchaseReceiptDTO(
                    UUID.randomUUID().toString(),
                    event.getId(),
                    outcomeId,
                    amount,
                    transactionCost,
                    newMarginalPrice
            );
        }
    }
}