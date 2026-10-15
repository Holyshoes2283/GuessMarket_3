package com.guessmarket.web.servlets;

import com.guessmarket.engine.dto.EventStatusDTO;
import com.guessmarket.engine.dto.EventSummaryDTO;
import com.guessmarket.engine.dto.OutcomeDTO;
import com.guessmarket.engine.dto.ParticipationDTO;
import com.guessmarket.engine.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@WebServlet(name = "EventsServlet", urlPatterns = "/events")
public class EventsServlet extends BaseServlet {

    private static final Logger LOGGER = Logger.getLogger(EventsServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> systemState = new HashMap<>();

            
            List<EventSummaryDTO> allEvents = getEngine().getAllEvents();
            List<UserDTO> allUsers = getEngine().getAllUsers();

            systemState.put("events", allEvents);
            systemState.put("users", allUsers);

            
            List<ParticipationDTO> aggregatedParticipations = new ArrayList<>();
            List<OutcomeDTO> aggregatedOutcomes = new ArrayList<>();

            for (EventSummaryDTO event : allEvents) {
                try {
                    
                    List<ParticipationDTO> eventParts = getEngine().getEventParticipations(event.eventId());
                    if (eventParts != null) {
                        aggregatedParticipations.addAll(eventParts);
                    }

                    
                    EventStatusDTO status = getEngine().getEventStatus(event.eventId());
                    if (status != null && status.outcomes() != null) {
                        aggregatedOutcomes.addAll(status.outcomes());
                    }

                } catch (Exception e) {
                    LOGGER.warning("Could not fetch details for event: " + event.eventId());
                }
            }

            systemState.put("participations", aggregatedParticipations);
            systemState.put("outcomes", aggregatedOutcomes);

            
            sendJsonResponse(resp, HttpServletResponse.SC_OK, systemState);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to aggregate system state for polling client", e);
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to fetch system state: " + e.getMessage());
        }
    }
}