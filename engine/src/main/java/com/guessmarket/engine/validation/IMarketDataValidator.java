package com.guessmarket.engine.validation;

import com.guessmarket.engine.dto.IStagingMarketData;
import com.guessmarket.engine.exceptions.InvalidDataLoadException;


public interface IMarketDataValidator {

    
    void validate(IStagingMarketData stagingData) throws InvalidDataLoadException;
}