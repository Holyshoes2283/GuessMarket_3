package com.guessmarket.web.servlets;

import com.guessmarket.engine.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends BaseServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private static final String ACTIVE_USERS_ATTR = "activeUsers";

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = null;
        Set<String> activeUsers = null;

        try {
            
            LoginRequest loginReq = parseRequestBody(req, LoginRequest.class);

            if (loginReq == null || loginReq.username == null || loginReq.username.trim().isEmpty()) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Username cannot be empty.");
                return;
            }

            
            username = loginReq.username.trim().toLowerCase();

            
            activeUsers = (Set<String>) getServletContext().getAttribute(ACTIVE_USERS_ATTR);
            if (activeUsers == null) {
                synchronized (getServletContext()) {
                    activeUsers = (Set<String>) getServletContext().getAttribute(ACTIVE_USERS_ATTR);
                    if (activeUsers == null) {
                        activeUsers = ConcurrentHashMap.newKeySet();
                        getServletContext().setAttribute(ACTIVE_USERS_ATTR, activeUsers);
                    }
                }
            }

            
            
            if (!activeUsers.add(username)) {
                sendJsonError(resp, HttpServletResponse.SC_CONFLICT, "The user '" + loginReq.username.trim() + "' is currently logged in elsewhere.");
                return;
            }

            
            
            UserDTO user = getEngine().registerUser(username);

            
            sendJsonResponse(resp, HttpServletResponse.SC_OK, new SuccessResponse("Login successful", user));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login processing failed", e);

            
            
            if (username != null && activeUsers != null) {
                activeUsers.remove(username);
                LOGGER.info("Rolled back session lock for user due to server error: " + username);
            }

            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    
    private static class LoginRequest {
        String username;
    }

    
    private static class SuccessResponse {
        String status;
        UserDTO user;

        SuccessResponse(String status, UserDTO user) {
            this.status = status;
            this.user = user;
        }
    }
}