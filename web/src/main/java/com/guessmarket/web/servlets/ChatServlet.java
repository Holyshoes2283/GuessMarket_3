package com.guessmarket.web.servlets;

import com.guessmarket.web.listener.WebContextListener.ChatMessageDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.guessmarket.web.listener.WebContextListener;
import java.io.IOException;
import java.util.List;


@WebServlet("/chat")
public class ChatServlet extends BaseServlet {

    private static final int MAX_CHAT_HISTORY = 100;

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<ChatMessageDTO> chatHistory = (List<ChatMessageDTO>) getServletContext().getAttribute(WebContextListener.CHAT_HISTORY_ATTRIBUTE);
            sendJsonResponse(resp, HttpServletResponse.SC_OK, chatHistory);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve chat: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ChatMessageDTO incomingMsg = parseRequestBody(req, ChatMessageDTO.class);
            if (incomingMsg == null || incomingMsg.sender == null || incomingMsg.message == null) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed chat message.");
                return;
            }

            incomingMsg.timestamp = System.currentTimeMillis();
            List<ChatMessageDTO> chatHistory = (List<ChatMessageDTO>) getServletContext().getAttribute(WebContextListener.CHAT_HISTORY_ATTRIBUTE);

            chatHistory.add(incomingMsg);

            if (chatHistory.size() > MAX_CHAT_HISTORY) {
                chatHistory.remove(0);
            }

            sendJsonResponse(resp, HttpServletResponse.SC_OK, incomingMsg);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Failed to post message: " + e.getMessage());
        }
    }
}