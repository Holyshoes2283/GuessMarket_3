package com.guessmarket.engine.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Account {
    private final String accountId;
    private double balance;
    private final Map<String, Integer> portfolio;

    public Account(String accountId, double initialBalance) {
        this.accountId = accountId;
        this.balance = initialBalance;
        this.portfolio = new ConcurrentHashMap<>();
    }

    public double getBalance() { 
        return balance; 
    }

    public synchronized void deductFunds(double amount) {
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient funds.");
        }
        this.balance -= amount;
    }

    public synchronized void addFunds(double amount) { 
        this.balance += amount; 
    }

    public synchronized void addShares(String eventId, String outcomeId, int shares) {
        String key = eventId + "_" + outcomeId;
        portfolio.put(key, portfolio.getOrDefault(key, 0) + shares);
    }
}