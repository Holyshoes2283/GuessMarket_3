package com.guessmarket.engine.dto;

import java.util.List;


public interface IStagingMarketData {

    
    List<UserDTO> getUsers();

    
    List<EventDTO> getEvents();

    
    List<MarketMakerDTO> getMarketMakers();
}