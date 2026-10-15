package com.guessmarket.web.servlets;

import com.guessmarket.engine.dto.EventClosureDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


@WebServlet({"/event/start", "/event/close"})
public class EventLifecycleServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();

        try {
            LifecycleRequest lcReq = parseRequestBody(req, LifecycleRequest.class);
            if (lcReq == null || lcReq.eventId == null || lcReq.username == null) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing eventId or username.");
                return;
            }

            if (uri.endsWith("/start")) {
                getEngine().startEvent(lcReq.eventId, lcReq.username);
                sendJsonResponse(resp, HttpServletResponse.SC_OK, new StatusMsg("Event started successfully."));
            }
            else if (uri.endsWith("/close")) {
                EventClosureDTO closure = getEngine().closeEvent(lcReq.eventId, lcReq.username, lcReq.winningOutcomeId);
                sendJsonResponse(resp, HttpServletResponse.SC_OK, closure);
            }

        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Lifecycle Action Failed: " + e.getMessage());
        }
    }

    private static class LifecycleRequest {
        String eventId;
        String username;
        String winningOutcomeId;
    }

    private static class StatusMsg {
        String status;
        StatusMsg(String status) { this.status = status; }
    }
}