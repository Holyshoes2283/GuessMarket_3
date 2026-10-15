package com.guessmarket.engine.loader;

import com.guessmarket.engine.exceptions.InvalidDataLoadException;
import java.io.File;


public interface IMarketDataLoader {

    
    void loadMarketData(File xmlFile) throws InvalidDataLoadException;
}