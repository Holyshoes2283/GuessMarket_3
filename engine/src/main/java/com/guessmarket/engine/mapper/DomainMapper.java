package com.guessmarket.engine.mapper;

import com.guessmarket.engine.domain.Account;
import com.guessmarket.engine.domain.MarketEvent;
import com.guessmarket.engine.dto.EventSummaryDTO;
import com.guessmarket.engine.dto.UserDTO;


public final class DomainMapper {

    
    private DomainMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    
    public static EventSummaryDTO toEventSummaryDTO(MarketEvent event) {
        return new EventSummaryDTO(
                event.getId(),
                event.getTitle(),
                event.isActive() ? "Active" : "Closed",
                event.getMarketMakerUsername()
        );
    }

    
    public static UserDTO toUserDTO(Account account, String defaultUsername) {
        if (account == null) {
            return null;
        }

        return new UserDTO(
                defaultUsername,
                account.getBalance()
        );
    }
}