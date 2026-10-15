package com.guessmarket.engine.dto;


public class UserDTO {

    
    private final String username;
    private final double balance;

    
    public UserDTO(String username, double balance) {
        this.username = username;
        this.balance = balance;
    }

    
    public String getUsername() {
        return username;
    }

    
    public double getBalance() {
        return balance;
    }

    @Override
    public String toString() {
        return String.format("UserDTO{username='%s', balance=%.2f}", username, balance);
    }
}