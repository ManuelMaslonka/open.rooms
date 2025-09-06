package com.maslonka.open.rooms.application.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.socket.WebSocketSession;

import static com.maslonka.open.rooms.application.handler.ChatWsHandlerConstants.ATTR_ROOM_ID;

public final class MessageProccessor {

    private final WebSocketSession session;
    private final JsonNode node;
    private final String roomId;

    public MessageProccessor(WebSocketSession session, JsonNode node) {
        this.session = session;
        this.node = node;
        this.roomId = (String) session.getAttributes().get(ATTR_ROOM_ID);
    }

    public String process() {
        processNameOfUser();
        processMessage();

        return roomId;
    }

    private void processNameOfUser() {
        final String userNameField = "nameOfUser";
        String name = node.path(userNameField).asText(null);

        if (name != null && !name.isBlank()) {
            return;
        }

        String generatedName = "DefaultUser";
        if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode objectNode) {
            objectNode.put(userNameField, generatedName);
        } else {
            throw new IllegalStateException("JsonNode is not an ObjectNode, cannot set user name.");
        }
    }

    private void processMessage() {

    }
}
