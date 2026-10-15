package com.guessmarket.web.servlets;

import com.guessmarket.engine.exceptions.InvalidDataLoadException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;


@WebServlet(name = "UploadServlet", urlPatterns = "/upload")
public class UploadServlet extends BaseServlet {

    private static final Logger LOGGER = Logger.getLogger(UploadServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uploaderUsername = req.getParameter("username");

        if (uploaderUsername == null || uploaderUsername.trim().isEmpty()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "A valid username must be provided.");
            return;
        }

        try (InputStream xmlStream = req.getInputStream()) {
            getEngine().loadEventsFromXml(xmlStream, uploaderUsername.trim());
            sendJsonResponse(resp, HttpServletResponse.SC_OK, new SuccessMessage("XML successfully loaded."));

        } catch (InvalidDataLoadException e) {
            LOGGER.log(Level.WARNING, "XML Validation Failure: " + e.getMessage());
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid XML structure: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected engine failure during XML upload", e);
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Engine failure: " + e.getMessage());
        }
    }

    private static class SuccessMessage {
        String status;
        SuccessMessage(String status) { this.status = status; }
    }
}