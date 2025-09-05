package com.maslonka.open.rooms.application.core;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public record Room(String id, LocalDateTime createdAt, Duration ttl, // time to live
                   Set<WebSocketSession> participants, int maxParticipants) {

    public static Room create(String id) {
        return new Room(id, LocalDateTime.now(), Duration.ofHours(1), ConcurrentHashMap.newKeySet(), 10);
    }

    @Override
    @NonNull
    public String toString() {
        return "Room{" + "id='" + id + '\'' + ", createdAt=" + createdAt + ", ttl=" + ttl + ", participants=" + participants +
                ", maxParticipants=" + maxParticipants + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Room room = (Room) o;
        return maxParticipants == room.maxParticipants && Objects.equals(id, room.id) && Objects.equals(ttl, room.ttl) &&
                Objects.equals(participants, room.participants);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(createdAt);
        result = 31 * result + Objects.hashCode(ttl);
        result = 31 * result + Objects.hashCode(participants);
        result = 31 * result + maxParticipants;
        return result;
    }
}
