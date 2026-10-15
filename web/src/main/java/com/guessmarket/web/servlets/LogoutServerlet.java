package com.guessmarket.web.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServerlet extends BaseServlet {

    private static final Logger LOGGER = Logger.getLogger(LogoutServerlet.class.getName());
    private static final String ACTIVE_USERS_ATTR = "activeUsers";

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            LogoutRequest logoutReq = parseRequestBody(req, LogoutRequest.class);
            
            if (logoutReq != null && logoutReq.username != null && !logoutReq.username.trim().isEmpty()) {
                String cleanUsername = logoutReq.username.trim().toLowerCase();
                
                Set<String> activeUsers = (Set<String>) getServletContext().getAttribute(ACTIVE_USERS_ATTR);
                if (activeUsers != null) {
                    activeUsers.remove(cleanUsername);
                    LOGGER.info("Session cleanly released for user: " + cleanUsername);
                }
            }
            
            sendJsonResponse(resp, HttpServletResponse.SC_OK, new SuccessMessage("Session terminated."));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Logout failure", e);
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error processing logout.");
        }
    }

    private static class LogoutRequest { 
        String username; 
    }
    
    private static class SuccessMessage { 
        String status; 
        SuccessMessage(String status) { this.status = status; } 
    }
}