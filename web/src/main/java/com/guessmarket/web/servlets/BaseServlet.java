package com.guessmarket.web.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.guessmarket.engine.IGuessMarketEngine;
import com.guessmarket.web.listener.WebContextListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;


public abstract class BaseServlet extends HttpServlet {

    protected final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();

    
    protected IGuessMarketEngine getEngine() {
        return (IGuessMarketEngine) getServletContext().getAttribute(WebContextListener.ENGINE_ATTRIBUTE);
    }

    protected void sendJsonResponse(HttpServletResponse resp, int statusCode, Object payload) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(statusCode);
        String jsonResponse = gson.toJson(payload);
        resp.getWriter().write(jsonResponse);
    }

    protected void sendJsonError(HttpServletResponse resp, int statusCode, String errorMessage) throws IOException {
        sendJsonResponse(resp, statusCode, new ErrorResponse(errorMessage));
    }

    protected <T> T parseRequestBody(HttpServletRequest req, Class<T> classOfT) throws IOException {
        String json = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        return gson.fromJson(json, classOfT);
    }

    private static class ErrorResponse {
        String error;
        ErrorResponse(String error) { this.error = error; }
    }
}