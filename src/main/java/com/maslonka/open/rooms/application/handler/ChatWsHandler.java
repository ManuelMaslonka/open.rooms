package com.maslonka.open.rooms.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maslonka.open.rooms.application.core.Room;
import com.maslonka.open.rooms.application.core.RoomStore;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Objects;

import static com.maslonka.open.rooms.application.handler.ChatWsHandlerConstants.*;

@Component
public class ChatWsHandler extends TextWebSocketHandler {

    private final RoomStore store;
    private final ObjectMapper mapper;

    public ChatWsHandler(RoomStore store, ObjectMapper mapper) {
        this.store = Objects.requireNonNull(store);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws IOException {
        JsonNode node;
        try {
            node = mapper.readTree(message.getPayload());
        } catch (IOException ex) {
            sendErrorAndClose(session, REASON_BAD_REQUEST);
            return;
        }

        switch (node.path(FIELD_TYPE).asText("")) {
            case TYPE_AUTH -> handleAuth(session, node);
            case TYPE_MSG -> handleMsg(session, node);
            default -> {
            }
        }
    }

    private void handleAuth(WebSocketSession session, JsonNode node) throws IOException {
        String roomId = node.path(FIELD_ROOM_ID).asText(null);

        Room room = store.get(roomId);
        if (room == null) {
            sendErrorAndClose(session, REASON_ROOM_NOT_FOUND);
            return;
        }

        if (room.participants().size() >= room.maxParticipants()) {
            sendErrorAndClose(session, REASON_ROOM_FULL);
            return;
        }

        room.participants().add(session);
        session.getAttributes().put(ATTR_ROOM_ID, roomId);
        sendJson(session, TYPE_AUTH_OK, null);
    }

    private void handleMsg(WebSocketSession session, JsonNode node) {
        String roomId = (String) session.getAttributes().get(ATTR_ROOM_ID);
        if (roomId == null) {
            return;
        }

        Room room = store.get(roomId);
        if (room == null) {
            return;
        }

        TextMessage frame = new TextMessage(node.toString());
        room.participants().stream().filter(WebSocketSession::isOpen).forEach(s -> {
            try {
                s.sendMessage(frame);
            } catch (IOException e) { /* log warning */ }
        });
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        Object roomId = session.getAttributes().get(ATTR_ROOM_ID);
        if (roomId != null) {
            Room room = store.get(roomId.toString());
            if (room != null) {
                room.participants().remove(session);
            }
        }
        super.afterConnectionClosed(session, status);
    }

    private void sendJson(WebSocketSession session, String type, ObjectNode extra) throws IOException {
        ObjectNode node = mapper.createObjectNode();
        node.put(FIELD_TYPE, type);
        if (extra != null) {
            node.setAll(extra);
        }
        session.sendMessage(new TextMessage(node.toString()));
    }

    private void sendErrorAndClose(WebSocketSession session, String reason) throws IOException {
        ObjectNode extra = mapper.createObjectNode();
        extra.put("reason", reason);
        sendJson(session, TYPE_ERROR, extra);
        session.close();
    }
}
