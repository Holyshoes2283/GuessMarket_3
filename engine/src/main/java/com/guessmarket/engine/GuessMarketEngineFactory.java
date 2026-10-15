package com.guessmarket.engine;


import com.guessmarket.engine.core.GuessMarketEngineImpl;


public final class GuessMarketEngineFactory {

    
    private static IGuessMarketEngine engineInstance;

    
    private GuessMarketEngineFactory() {
        throw new UnsupportedOperationException("GuessMarketEngineFactory is a static utility class and cannot be instantiated.");
    }

    
    public static synchronized IGuessMarketEngine createEngine() {
        if (engineInstance == null) {
            
            engineInstance = new GuessMarketEngineImpl();
        }
        return engineInstance;
    }
}