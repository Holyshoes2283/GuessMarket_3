package com.guessmarket.web.listener;

import com.guessmarket.engine.IGuessMarketEngine;
import com.guessmarket.engine.core.GuessMarketEngineImpl;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@WebListener
public class WebContextListener implements ServletContextListener {

    public static final String ENGINE_ATTRIBUTE = "guessMarketEngine";
    public static final String CHAT_HISTORY_ATTRIBUTE = "chatHistory";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        
        IGuessMarketEngine engine = new GuessMarketEngineImpl();
        context.setAttribute(ENGINE_ATTRIBUTE, engine);

        
        List<ChatMessageDTO> chatHistory = new CopyOnWriteArrayList<>();
        context.setAttribute(CHAT_HISTORY_ATTRIBUTE, chatHistory);

        System.out.println("WebContextListener: Engine and Chat History successfully injected into ServletContext.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        IGuessMarketEngine engine = (IGuessMarketEngine) context.getAttribute(ENGINE_ATTRIBUTE);
        if (engine != null) {
            engine.shutdown();
        }
        System.out.println("WebContextListener: Engine shut down gracefully.");
    }

    
    public static class ChatMessageDTO {
        public String sender;
        public String message;
        public long timestamp;

        public ChatMessageDTO(String sender, String message, long timestamp) {
            this.sender = sender;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}