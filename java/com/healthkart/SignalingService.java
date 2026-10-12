package com.healthkart;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.healthkart.Util.m;
import static com.healthkart.Util.s;

/** Socket.IO events: notifications, call lifecycle, WebRTC signalling, in-call chat. */
@Component
public class SignalingService {
    private final SocketIOServer io;

    /** roomId -> socket ids currently in it (Node: activeRooms) */
    private final Map<String, Set<UUID>> activeRooms = new ConcurrentHashMap<>();
    /** socket id -> rooms it joined (needed because rooms are already cleared when disconnect fires) */
    private final Map<UUID, Set<String>> clientRooms = new ConcurrentHashMap<>();

    public SignalingService(SocketIOServer io) {
        this.io = io;
    }

    @FunctionalInterface
    private interface Handler {
        void handle(SocketIOClient client, Map<String, Object> data);
    }

    @SuppressWarnings("unchecked")
    private void on(String event, Handler h) {
        io.addEventListener(event, Map.class, (client, data, ack) ->
                h.handle(client, data == null ? new HashMap<>() : (Map<String, Object>) data));
    }

    /** socket.to(room).emit(...) : everyone in the room except the sender */
    private void toRoomExceptSender(SocketIOClient c, String room, String event, Object payload) {
        io.getRoomOperations(room).sendEvent(event, c, payload);
    }

    /** io.emit(...) */
    private void all(String event, Object payload) {
        io.getBroadcastOperations().sendEvent(event, payload);
    }

    @PostConstruct
    void register() {
        io.addConnectListener(c -> System.out.println("Socket connected: " + c.getSessionId()));

        on("register_user", (c, d) -> {
            String email = s(d.get("email"));
            if (email != null) c.joinRoom("user_" + email.toLowerCase());
        });

        // Doctor initiates call to patient
        on("start_doctor_call", (c, d) -> {
            System.out.println("Doctor " + d.get("doctorName") + " calling patient " + d.getOrDefault("patientName", "")
                    + " (" + d.getOrDefault("patientEmail", "") + ")");
            all("incoming_doctor_call", d);
        });

        on("accept_doctor_call", (c, d) -> {
            System.out.println("Patient accepted doctor call for room: " + d.get("roomId"));
            all("doctor_call_accepted", m("roomId", d.get("roomId"), "appointmentId", d.get("appointmentId")));
        });

        on("decline_doctor_call", (c, d) -> {
            String roomId = s(d.get("roomId"));
            System.out.println("Patient declined doctor call for room: " + roomId);
            if (roomId != null) toRoomExceptSender(c, roomId, "doctor_call_declined", m("appointmentId", d.get("appointmentId")));
            all("doctor_call_declined", m("roomId", d.get("roomId"), "appointmentId", d.get("appointmentId")));
        });

        on("join_room", (c, d) -> {
            String roomId = s(d.get("roomId"));
            if (roomId == null) return;
            c.joinRoom(roomId);

            Set<UUID> users = activeRooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet());
            users.add(c.getSessionId());
            clientRooms.computeIfAbsent(c.getSessionId(), k -> ConcurrentHashMap.newKeySet()).add(roomId);

            System.out.println("User " + d.get("userName") + " (" + d.get("role") + ") joined room: " + roomId
                    + ". Total: " + users.size());

            toRoomExceptSender(c, roomId, "peer_joined",
                    m("peerId", c.getSessionId().toString(), "role", d.get("role"), "userName", d.get("userName")));

            if (users.size() > 1) {
                io.getRoomOperations(roomId).sendEvent("room_ready", m("roomId", roomId, "totalParticipants", users.size()));
            }
        });

        // WebRTC signalling
        on("webrtc_offer", (c, d) -> relay(c, d, "webrtc_offer", m("sdp", d.get("sdp"), "senderId", c.getSessionId().toString())));
        on("webrtc_answer", (c, d) -> relay(c, d, "webrtc_answer", m("sdp", d.get("sdp"), "senderId", c.getSessionId().toString())));
        on("ice_candidate", (c, d) -> relay(c, d, "ice_candidate", m("candidate", d.get("candidate"), "senderId", c.getSessionId().toString())));

        on("in_call_chat", (c, d) -> {
            String time = s(d.get("time"));
            if (time == null) time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
            relay(c, d, "in_call_chat", m("message", d.get("message"), "senderName", d.get("senderName"),
                    "role", d.get("role"), "time", time));
        });

        on("in_call_prescription", (c, d) -> relay(c, d, "in_call_prescription", m("prescription", d.get("prescription"))));

        on("screen_share_status", (c, d) -> relay(c, d, "screen_share_status",
                m("isSharing", d.get("isSharing"), "userName", d.get("userName"))));

        on("end_call", (c, d) -> relay(c, d, "call_ended", m("senderId", c.getSessionId().toString())));

        io.addDisconnectListener(c -> {
            UUID id = c.getSessionId();
            Set<String> rooms = clientRooms.remove(id);
            if (rooms != null) {
                for (String room : rooms) {
                    Set<UUID> users = activeRooms.get(room);
                    if (users == null) continue;
                    users.remove(id);
                    if (users.isEmpty()) activeRooms.remove(room);
                    else io.getRoomOperations(room).sendEvent("peer_left", m("peerId", id.toString()));
                }
            }
            System.out.println("Socket disconnected: " + id);
        });
    }

    private void relay(SocketIOClient c, Map<String, Object> d, String event, Object payload) {
        String roomId = s(d.get("roomId"));
        if (roomId == null) return;
        toRoomExceptSender(c, roomId, event, payload);
    }
}
