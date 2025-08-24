package com.maslonka.open.rooms.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maslonka.open.rooms.application.core.Room;
import com.maslonka.open.rooms.application.core.RoomStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;

@Component
public class ChatWsHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWsHandler.class);

    // JSON fields
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_ROOM_ID = "roomId";
    private static final String FIELD_PROOF = "proof";

    // message types
    private static final String TYPE_AUTH = "auth";
    private static final String TYPE_MSG = "msg";
    private static final String TYPE_ERROR = "error";
    private static final String TYPE_AUTH_OK = "auth-ok";

    // session attributes
    private static final String ATTR_ROOM_ID = "roomId";

    // error reasons
    private static final String REASON_ROOM_NOT_FOUND = "room-not-found";
    private static final String REASON_AUTH_FAILED = "auth-failed";
    private static final String REASON_BAD_REQUEST = "bad-request";
    private static final String REASON_ROOM_FULL = "room-full";

    private final RoomStore store;
    private final ObjectMapper mapper;

    public ChatWsHandler(RoomStore store, ObjectMapper mapper) {
        this.store = Objects.requireNonNull(store, "store");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        final String payload = message.getPayload();
        JsonNode node;
        try {
            node = mapper.readTree(payload);
        } catch (Exception ex) {
            log.debug("Invalid JSON from session {}: {}", session.getId(), ex.getMessage());
            sendErrorAndMaybeClose(session, REASON_BAD_REQUEST);
            return;
        }

        String type = node.path(FIELD_TYPE).asText("");
        switch (type) {
            case TYPE_AUTH -> handleAuth(session, node);
            case TYPE_MSG -> handleMsg(session, node);
            default -> log.debug("Ignoring unsupported message type '{}' from session {}", type, session.getId());
        }
    }

    private void handleAuth(WebSocketSession session, JsonNode node) throws IOException {
        String roomId = node.path(FIELD_ROOM_ID).asText(null);
        String proofB64 = node.path(FIELD_PROOF).asText(null);
        if (roomId == null || proofB64 == null) {
            sendErrorAndMaybeClose(session, REASON_BAD_REQUEST);
            return;
        }

        Room room = store.get(roomId);
        if (room == null) {
            sendErrorAndMaybeClose(session, REASON_ROOM_NOT_FOUND);
            return;
        }

        if (room.participants().size() >= room.maxParticipants()) {
            sendErrorAndMaybeClose(session, REASON_ROOM_FULL);
            return;
        }

        byte[] proof;
        try {
            proof = Base64.getDecoder().decode(proofB64.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException _) {
            sendErrorAndMaybeClose(session, REASON_BAD_REQUEST);
            return;
        }

        boolean ok = MessageDigest.isEqual(proof, room.verifier());
        if (!ok) {
            sendErrorAndMaybeClose(session, REASON_AUTH_FAILED);
            return;
        }

        room.participants().add(session);
        session.getAttributes().put(ATTR_ROOM_ID, roomId);
        sendAuthOk(session);
        log.debug("Session {} authenticated to room {}", session.getId(), roomId);
    }

    private void handleMsg(WebSocketSession session, JsonNode node) {
        Object roomIdAttr = session.getAttributes().get(ATTR_ROOM_ID);
        if (roomIdAttr == null) {
            log.debug("Dropping message from unauthenticated session {}", session.getId());
            return;
        }
        String roomId = roomIdAttr.toString();
        Room room = store.get(roomId);
        if (room == null) {
            log.debug("Room {} not found for session {}", roomId, session.getId());
            return;
        }

        TextMessage frame = new TextMessage(node.toString());
        for (WebSocketSession s : room.participants()) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(frame);
                } catch (Exception ex) {
                    log.warn("Failed to send message to session {} in room {}: {}", s.getId(), roomId, ex.getMessage());
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,@NonNull CloseStatus status) throws Exception {
        Object roomIdAttr = session.getAttributes().get(ATTR_ROOM_ID);
        if (roomIdAttr != null) {
            Room room = store.get(roomIdAttr.toString());
            if (room != null) {
                room.participants().remove(session);
            }
        }
        super.afterConnectionClosed(session, status);
    }

    private void sendAuthOk(WebSocketSession session) throws IOException {
        ObjectNode resp = mapper.createObjectNode();
        resp.put(FIELD_TYPE, TYPE_AUTH_OK);
        session.sendMessage(new TextMessage(resp.toString()));
    }

    private void sendErrorAndMaybeClose(WebSocketSession session, String reason) throws IOException {
        ObjectNode resp = mapper.createObjectNode();
        resp.put(FIELD_TYPE, TYPE_ERROR);
        resp.put("reason", reason);
        session.sendMessage(new TextMessage(resp.toString()));
        session.close();
    }
}
