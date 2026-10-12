package com.healthkart;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.stereotype.Component;

/** Equivalent of Node's io.emit(...): broadcast an event to every connected client. */
@Component
public class Realtime {
    private final SocketIOServer io;

    public Realtime(SocketIOServer io) {
        this.io = io;
    }

    public void emit(String event, Object payload) {
        io.getBroadcastOperations().sendEvent(event, payload);
    }
}
