package com.maslonka.open.rooms.application.handler;

public class ChatWsHandlerConstants {

    // JSON fields
    static final String FIELD_TYPE = "type";
    static final String FIELD_ROOM_ID = "roomId";
    static final String FIELD_PROOF = "proof";

    // message types
    static final String TYPE_AUTH = "auth";
    static final String TYPE_MSG = "msg";
    static final String TYPE_ERROR = "error";
    static final String TYPE_AUTH_OK = "auth-ok";

    // session attributes
    static final String ATTR_ROOM_ID = "roomId";

    // error reasons
    static final String REASON_ROOM_NOT_FOUND = "room-not-found";
    static final String REASON_AUTH_FAILED = "auth-failed";
    static final String REASON_BAD_REQUEST = "bad-request";
    static final String REASON_ROOM_FULL = "room-full";


}
