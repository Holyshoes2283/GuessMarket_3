package com.guessmarket.engine.internal;

import java.util.Comparator;
import java.util.PriorityQueue;

public class OrderBookTradingStrategy {

    public enum OrderType { BUY, SELL }

    public static class Order {
        public final String orderId;
        public final String userId;
        public final String outcomeId;
        public final OrderType type;
        public int amount;
        public final double price;
        public final long timestamp;

        public Order(String orderId, String userId, String outcomeId, OrderType type, int amount, double price) {
            this.orderId = orderId;
            this.userId = userId;
            this.outcomeId = outcomeId;
            this.type = type;
            this.amount = amount;
            this.price = price;
            this.timestamp = System.nanoTime(); 
        }
    }

    
    private final PriorityQueue<Order> bids = new PriorityQueue<>(
            Comparator.<Order>comparingDouble(o -> o.price).reversed()
                    .thenComparingLong(o -> o.timestamp)
    );

    
    private final PriorityQueue<Order> asks = new PriorityQueue<>(
            Comparator.<Order>comparingDouble(o -> o.price)
                    .thenComparingLong(o -> o.timestamp)
    );

    private double lastExecutedPrice = 0.0;
    private final double eventBasePayout; 
    private final boolean allowsMinting;

    public OrderBookTradingStrategy(double eventBasePayout, boolean allowsMinting) {
        this.eventBasePayout = eventBasePayout;
        this.allowsMinting = allowsMinting;
    }

    public void processOrder(Order newOrder) throws Exception {
        if (newOrder.price > eventBasePayout - 0.01) {
            throw new Exception("Price exceeds maximum allowed limit (d - 0.01)");
        }

        if (newOrder.type == OrderType.BUY) {
            bids.add(newOrder);
        } else {
            asks.add(newOrder);
        }

        executeMatchingAlgorithm();
    }

    private void executeMatchingAlgorithm() {
        while (!bids.isEmpty() && !asks.isEmpty()) {
            Order highestBid = bids.peek();
            Order lowestAsk = asks.peek();

            
            if (allowsMinting && (highestBid.price + lowestAsk.price >= eventBasePayout)) {
                executeMinting(highestBid, lowestAsk);
            }
            
            else if (highestBid.price >= lowestAsk.price) {
                executeStandardMatch(highestBid, lowestAsk);
            }
            else {
                break; 
            }

            
            if (highestBid.amount == 0) bids.poll();
            if (lowestAsk.amount == 0) asks.poll();
        }
    }

    private void executeMinting(Order bid, Order ask) {
        int fillAmount = Math.min(bid.amount, ask.amount);
        double combinedPayment = (bid.price + ask.price) * fillAmount;

        
        System.out.printf("MINT: %d shares. Event collects %.2f.%n", fillAmount, combinedPayment);

        bid.amount -= fillAmount;
        ask.amount -= fillAmount;
        lastExecutedPrice = bid.price;
    }

    private void executeStandardMatch(Order bid, Order ask) {
        int fillAmount = Math.min(bid.amount, ask.amount);

        
        System.out.printf("MATCH: %d shares cleared at %.2f.%n", fillAmount, ask.price);

        bid.amount -= fillAmount;
        ask.amount -= fillAmount;
        lastExecutedPrice = ask.price;
    }

    
    public double[] getStatistics() {
        double currentBid = bids.isEmpty() ? 0.0 : bids.peek().price;
        double currentAsk = asks.isEmpty() ? 0.0 : asks.peek().price;
        double mid = (currentBid > 0 && currentAsk > 0) ? (currentBid + currentAsk) / 2.0 : 0.0;
        double spread = (currentBid > 0 && currentAsk > 0) ? (currentAsk - currentBid) : 0.0;

        return new double[]{ lastExecutedPrice, currentBid, currentAsk, mid, spread };
    }
}