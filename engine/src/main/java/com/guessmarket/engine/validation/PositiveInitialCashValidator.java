package com.guessmarket.engine.validation;

import com.guessmarket.engine.dto.IStagingMarketData;
import com.guessmarket.engine.dto.UserDTO;
import com.guessmarket.engine.exceptions.InvalidDataLoadException;


public class PositiveInitialCashValidator implements IMarketDataValidator {

    @Override
    public void validate(IStagingMarketData stagingData) throws InvalidDataLoadException {

        
        for (UserDTO user : stagingData.getUsers()) {

            
            if (user.getBalance() <= 0) {
                throw new InvalidDataLoadException(
                        "Validation Failed: Initial cash balance must be > 0. User '"
                                + user.getUsername() + "' has a balance of " + user.getBalance()
                );
            }
        }
    }
}