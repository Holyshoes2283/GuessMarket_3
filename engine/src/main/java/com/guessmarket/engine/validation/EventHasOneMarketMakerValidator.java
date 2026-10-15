package com.guessmarket.engine.validation;

import com.guessmarket.engine.dto.IStagingMarketData;
import com.guessmarket.engine.dto.EventDTO;
import com.guessmarket.engine.dto.MarketMakerDTO;
import com.guessmarket.engine.exceptions.InvalidDataLoadException;

import java.util.Map;
import java.util.stream.Collectors;


public class EventHasOneMarketMakerValidator implements IMarketDataValidator {

    @Override
    public void validate(IStagingMarketData stagingData) throws InvalidDataLoadException {

        
        Map<String, Long> marketMakerCountsByEventId = stagingData.getMarketMakers().stream()
                .collect(Collectors.groupingBy(
                        mm -> normalizeId(mm.getEventId()),
                        Collectors.counting()
                ));

        for (EventDTO event : stagingData.getEvents()) {
            String normalizedEventId = normalizeId(event.getId());
            long mmCount = marketMakerCountsByEventId.getOrDefault(normalizedEventId, 0L);

            if (mmCount != 1) {
                throw new InvalidDataLoadException(
                        "Validation Failed: Event '" + event.getId() + "' must have exactly ONE Market Maker. "
                                + "Found: " + mmCount
                );
            }
        }
    }

    
    private String normalizeId(String id) {
        if (id == null) return "";
        return id.trim().toLowerCase();
    }
}