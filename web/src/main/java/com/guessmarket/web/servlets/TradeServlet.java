package com.guessmarket.web.servlets;

import com.guessmarket.engine.dto.PurchaseReceiptDTO;
import com.guessmarket.engine.exception.EventNotFoundException;
import com.guessmarket.engine.exception.InvalidPurchaseException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


@WebServlet(name = "TradeServlet", urlPatterns = "/trade")
public class TradeServlet extends BaseServlet {

    private static final Logger LOGGER = Logger.getLogger(TradeServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            TradeRequest tr = parseRequestBody(req, TradeRequest.class);

            if (tr == null) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Empty or malformed JSON payload.");
                return;
            }

            String resolvedOutcome = tr.getActualOutcome();
            int resolvedAmount = tr.getActualAmount();
            String resolvedAction = (tr.action != null && !tr.action.trim().isEmpty()) ? tr.action.trim().toUpperCase() : "BUY";

            if (tr.eventId == null || tr.eventId.trim().isEmpty() ||
                    tr.username == null || tr.username.trim().isEmpty() ||
                    resolvedOutcome == null || resolvedOutcome.trim().isEmpty() ||
                    resolvedAmount <= 0) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid trade parameters.");
                return;
            }

            PurchaseReceiptDTO receipt = getEngine().executeTrade(
                    tr.eventId.trim(),
                    tr.username.trim(),
                    resolvedAction,
                    resolvedOutcome.trim(),
                    resolvedAmount
            );

            sendJsonResponse(resp, HttpServletResponse.SC_OK, receipt);

        } catch (EventNotFoundException | InvalidPurchaseException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during trade execution", e);
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private static class TradeRequest {
        String eventId;
        String username;
        String action;
        String outcome;
        String outcomeId;
        Double amount;
        Double quantity;

        public String getActualOutcome() {
            if (outcomeId != null && !outcomeId.trim().isEmpty()) return outcomeId;
            return outcome;
        }

        public int getActualAmount() {
            if (quantity != null) return quantity.intValue();
            if (amount != null) return amount.intValue();
            return 0;
        }
    }
}