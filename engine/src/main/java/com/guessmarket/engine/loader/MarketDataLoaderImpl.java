package com.guessmarket.engine.loader;

import com.guessmarket.engine.IGuessMarketEngine;
import com.guessmarket.engine.dto.IStagingMarketData;
import com.guessmarket.engine.exceptions.InvalidDataLoadException;
import com.guessmarket.engine.validation.IMarketDataValidator;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MarketDataLoaderImpl implements IMarketDataLoader {

    private static final Logger LOGGER = Logger.getLogger(MarketDataLoaderImpl.class.getName());

    private final IGuessMarketEngine engineCore;
    private final List<IMarketDataValidator> validators;
    private final IXmlParser xmlParser; 

    public MarketDataLoaderImpl(IGuessMarketEngine engineCore,
                                List<IMarketDataValidator> validators,
                                IXmlParser xmlParser) {
        
        this.engineCore = Objects.requireNonNull(engineCore, "Engine core cannot be null");
        this.validators = Objects.requireNonNull(validators, "Validator list cannot be null");
        this.xmlParser = Objects.requireNonNull(xmlParser, "XML Parser cannot be null");
    }

    @Override
    public void loadMarketData(File xmlFile) throws InvalidDataLoadException {
        try {
            LOGGER.info("Starting Phase 8 Data Ingestion Pipeline for: " + xmlFile.getName());

            
            
            IStagingMarketData stagingData = xmlParser.parseToStaging(xmlFile);
            LOGGER.info("XML successfully parsed into Staging Area. Commencing validations...");

            
            
            for (IMarketDataValidator validator : validators) {
                LOGGER.fine("Running validator: " + validator.getClass().getSimpleName());
                validator.validate(stagingData);
            }

            LOGGER.info("All Phase 8 business validations passed successfully.");

            
            
            engineCore.commitStagedData(stagingData);
            LOGGER.info("Staging data committed to core engine repositories.");

        } catch (InvalidDataLoadException ex) {
            
            LOGGER.warning("Validation Rejected XML Load: " + ex.getMessage());
            throw ex; 
        } catch (Exception ex) {
            
            LOGGER.log(Level.SEVERE, "Critical File Parsing Error", ex);
            throw new InvalidDataLoadException("Critical File Parsing Error: " + ex.getMessage());
        }
    }
}