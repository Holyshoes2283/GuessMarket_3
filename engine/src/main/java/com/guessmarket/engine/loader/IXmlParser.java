package com.guessmarket.engine.loader;

import com.guessmarket.engine.dto.IStagingMarketData;

import java.io.File;
import java.io.InputStream;


public interface IXmlParser {

    
    IStagingMarketData parseToStaging(File xmlFile) throws Exception;

    
    IStagingMarketData parseToStaging(InputStream xmlStream, String uploaderUsername) throws Exception;
}