package com.guessmarket.ui.network;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;


public class HttpClientAdapter {

    private static volatile HttpClientAdapter instance;

    private final HttpClient httpClient;
    private final Gson gson;

    private static final String BASE_URL = "http://localhost:8080/guessmarket";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";

    private HttpClientAdapter() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.gson = new Gson();
    }

    public static HttpClientAdapter getInstance() {
        if (instance == null) {
            synchronized (HttpClientAdapter.class) {
                if (instance == null) {
                    instance = new HttpClientAdapter();
                }
            }
        }
        return instance;
    }

    
    public CompletableFuture<String> login(String username) {
        LoginRequest payload = new LoginRequest(username);
        String jsonBody = gson.toJson(payload);
        HttpRequest request = buildPostRequest("/login", jsonBody, APPLICATION_JSON);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleResponse);
    }
    
    
    public CompletableFuture<String> logout(String username) {
        String payload = "{\"username\":\"" + username + "\"}";
        HttpRequest request = buildPostRequest("/logout", payload, APPLICATION_JSON);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleResponse);
    }

    
    public CompletableFuture<String> getEvents() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/events"))
                .GET()
                .header("Accept", APPLICATION_JSON)
                .timeout(Duration.ofSeconds(3))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleResponse);
    }

    
    public CompletableFuture<String> uploadMarketData(String rawXml, String username) {
        String endpoint = "/upload?username=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
        HttpRequest request = buildPostRequest(endpoint, rawXml, APPLICATION_XML);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleResponse);
    }

    
    public CompletableFuture<String> startEvent(String eventId, String username) {
        EventActionRequest payload = new EventActionRequest(eventId, username);
        String jsonBody = gson.toJson(payload);
        HttpRequest request = buildPostRequest("/event/start", jsonBody, APPLICATION_JSON);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleResponse);
    }

    
    public CompletableFuture<String> closeEvent(String eventId, String username, String winningOutcomeId) {
        CloseEventRequest payload = new CloseEventRequest(eventId, username, winningOutcomeId);
        String jsonBody = gson.toJson(payload);
        HttpRequest request = buildPostRequest("/event/close", jsonBody, APPLICATION_JSON);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleResponse);
    }

    
    public CompletableFuture<String> trade(String eventId, String username, String outcome, double amount) {
        TradeRequest payload = new TradeRequest(eventId, username, "BUY", outcome, amount);
        String jsonBody = gson.toJson(payload);
        HttpRequest request = buildPostRequest("/trade", jsonBody, APPLICATION_JSON);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleResponse);
    }

    private HttpRequest buildPostRequest(String path, String body, String contentType) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header(HEADER_CONTENT_TYPE, contentType)
                .header("Accept", APPLICATION_JSON)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private String handleResponse(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            throw new RuntimeException("HTTP Request Failed. Status: " + response.statusCode() + ", Body: " + response.body());
        }
    }

    
    
    

    private static class LoginRequest {
        String username;
        LoginRequest(String username) { this.username = username; }
    }

    private static class EventActionRequest {
        String eventId;
        String username;
        EventActionRequest(String eventId, String username) {
            this.eventId = eventId;
            this.username = username;
        }
    }

    
    private static class CloseEventRequest {
        String eventId;
        String username;
        String winningOutcomeId;
        CloseEventRequest(String eventId, String username, String winningOutcomeId) {
            this.eventId = eventId;
            this.username = username;
            this.winningOutcomeId = winningOutcomeId;
        }
    }

    private static class TradeRequest {
        String eventId;
        String username;
        String action;
        String outcome;
        double amount;
        TradeRequest(String eventId, String username, String action, String outcome, double amount) {
            this.eventId = eventId;
            this.username = username;
            this.action = action;
            this.outcome = outcome;
            this.amount = amount;
        }
    }
}