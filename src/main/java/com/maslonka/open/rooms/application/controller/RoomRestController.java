package com.maslonka.open.rooms.application.controller;

import com.maslonka.open.rooms.application.core.Room;
import com.maslonka.open.rooms.application.core.RoomStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomRestController {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RoomRestController.class);

    private final RoomStore store;
    private static final SecureRandom random = new SecureRandom();

    public RoomRestController(RoomStore store) {
        this.store = store;
    }

    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public record CreateReq(String roomId, String verifierBase64) {}
    public record CreateResp(String roomId, String joinUrl) {}

    @PostMapping
    public CreateResp create(@RequestBody CreateReq req, @RequestHeader("Host") String host) {
        if (req == null || req.verifierBase64() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verifierBase64 required");
        }
        String roomId = (req.roomId() != null && !req.roomId().isBlank()) ? req.roomId() : UUID.randomUUID().toString().replace("-", "");
        byte[] verifier = Base64.getDecoder().decode(req.verifierBase64());

        Room room = Room.create(roomId, verifier);
        store.create(room);

        String joinUrl = "https://" + host + "/room/" + roomId;
        CreateResp createResp = new CreateResp(roomId, joinUrl);
        LOG.info("Room created: {}", roomId);
        return createResp;
    }

    @GetMapping("/{roomId}")
    public Map<String, Object> get(@PathVariable String roomId) {
        Room r = store.get(roomId);
        if (r == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return Map.of(
                "roomId", r.id(),
                "online", r.participants().size()
        );
    }

    @GetMapping
    public Map<String, Object> info() {
        return Map.of(
                "rooms", store.count(),
                "activeRooms", store.activeCount()
        );
    }
}
