package com.maslonka.open.rooms.application.core;

import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public record Room(String id, byte[] salt, byte[] verifier, LocalDateTime createdAt, Duration ttl, // time to live
                   Set<WebSocketSession> participants, int maxParticipants) {

    public static Room create(String id, byte[] salt, byte[] verifier) {
        return new Room(id, salt, verifier, LocalDateTime.now(), Duration.ofHours(1), ConcurrentHashMap.newKeySet(), 10);
    }

    @Override
    public String toString() {
        return "Room{" + "id='" + id + '\'' + ", salt=" + Arrays.toString(salt) + ", verifier=" + Arrays.toString(verifier) +
                ", createdAt=" + createdAt + ", ttl=" + ttl + ", participants=" + participants + ", maxParticipants=" + maxParticipants +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Room room = (Room) o;
        return maxParticipants == room.maxParticipants && Objects.equals(id, room.id) && Arrays.equals(salt, room.salt) &&
                Objects.equals(ttl, room.ttl) && Arrays.equals(verifier, room.verifier) && Objects.equals(createdAt, room.createdAt) &&
                Objects.equals(participants, room.participants);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Arrays.hashCode(salt);
        result = 31 * result + Arrays.hashCode(verifier);
        result = 31 * result + Objects.hashCode(createdAt);
        result = 31 * result + Objects.hashCode(ttl);
        result = 31 * result + Objects.hashCode(participants);
        result = 31 * result + maxParticipants;
        return result;
    }
}
