package com.guessmarket.engine.exception;

public abstract class EngineException extends Exception {
    public EngineException(String message) { 
        super(message); 
    }
}