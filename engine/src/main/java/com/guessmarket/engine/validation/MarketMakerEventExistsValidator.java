package com.guessmarket.engine.validation;

import com.guessmarket.engine.dto.IStagingMarketData;
import com.guessmarket.engine.dto.MarketMakerDTO;
import com.guessmarket.engine.dto.EventDTO;
import com.guessmarket.engine.exceptions.InvalidDataLoadException;

import java.util.Set;
import java.util.stream.Collectors;


public class MarketMakerEventExistsValidator implements IMarketDataValidator {

    @Override
    public void validate(IStagingMarketData stagingData) throws InvalidDataLoadException {
        
        Set<String> validEventIds = stagingData.getEvents().stream()
                .map(EventDTO::getId)
                .collect(Collectors.toSet());

        for (MarketMakerDTO mm : stagingData.getMarketMakers()) {
            if (!validEventIds.contains(mm.getEventId())) {
                throw new InvalidDataLoadException(
                        "Validation Failed: Market Maker references a non-existent Event ID -> '"
                                + mm.getEventId() + "'"
                );
            }
        }
    }
}