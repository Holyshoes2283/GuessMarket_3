package com.guessmarket.engine.validation;

import com.guessmarket.engine.dto.IStagingMarketData;
import com.guessmarket.engine.dto.UserDTO;
import com.guessmarket.engine.exceptions.InvalidDataLoadException;

import java.util.HashSet;
import java.util.Set;


public class UniqueUsernameValidator implements IMarketDataValidator {

    @Override
    public void validate(IStagingMarketData stagingData) throws InvalidDataLoadException {
        
        Set<String> seenUsernames = new HashSet<>();

        for (UserDTO user : stagingData.getUsers()) {
            if (!seenUsernames.add(user.getUsername())) {
                throw new InvalidDataLoadException(
                        "Validation Failed: Username is not unique -> '" + user.getUsername() + "'"
                );
            }
        }
    }
}