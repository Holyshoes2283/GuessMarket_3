package com.guessmarket.web.listener;

import com.guessmarket.engine.IGuessMarketEngine;
import com.guessmarket.engine.core.GuessMarketEngineImpl;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.logging.Logger;


@WebListener
public class EngineContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(EngineContextListener.class.getName());
    public static final String ENGINE_ATTRIBUTE_NAME = "GUESS_MARKET_ENGINE";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Tomcat Context Initializing: Bootstrapping GuessMarket Engine...");

        
        IGuessMarketEngine engine = new GuessMarketEngineImpl();

        
        ServletContext context = sce.getServletContext();
        context.setAttribute(ENGINE_ATTRIBUTE_NAME, engine);

        LOGGER.info("GuessMarket Engine successfully bound to ServletContext.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Tomcat Context Destroying: Shutting down Engine...");
        ServletContext context = sce.getServletContext();
        IGuessMarketEngine engine = (IGuessMarketEngine) context.getAttribute(ENGINE_ATTRIBUTE_NAME);

        if (engine != null) {
            engine.shutdown();
        }
    }
}