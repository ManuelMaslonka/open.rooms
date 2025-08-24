package com.maslonka.open.rooms.application.core;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomStore {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public void create(Room room) {
        rooms.put(room.id(), room);
    }

    public  Room get(String id) {
        return rooms.get(id);
    }

    public void delete(String id) {
        rooms.remove(id);
    }

    public Object count() {
        return rooms.size();
    }

    public Object activeCount() {
        return rooms.values().stream().filter(r -> !r.participants().isEmpty()).count();
    }
}
