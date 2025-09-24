package org.example;



import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.util.*;



/**

 * Linjeformat:

 * clientId|yyyy-MM-dd HH:mm:ss|TYPE|payload|payload2|...

 *

 * Eksempler:

 * c42|2025-09-23 12:00:00|TEXT|Hello

 * c42|2025-09-23 12:01:00|LOGIN|bob|hunter2

 * c42|2025-09-23 12:02:00|JOIN_ROOM|Lobby

 */

public class Message {



    public enum MessageType {

        TEXT, EMOJI, FILE_TRANSFER, LOGIN, JOIN_ROOM, PRIVATE;



        // Tåler gamle/andre navne ("Text" -> TEXT, "Emoji" -> EMOJI)

        static MessageType fromStringSafe(String s) {

            String u = s == null ? "" : s.trim().toUpperCase(Locale.ROOT);

            switch (u) {

                case "TEXT": case "TXT": case "MESSAGE": return TEXT;

                case "EMOJI": case "EMOJIS": return EMOJI;

                case "FILE_TRANSFER": case "FILE": case "SEND_FILE": return FILE_TRANSFER;

                case "LOGIN": case "AUTH": return LOGIN;

                case "JOIN_ROOM": case "JOIN": return JOIN_ROOM;

                case "PRIVATE": case "WHISPER": case "DM": return PRIVATE;

                default: throw new IllegalArgumentException("Unknown message type: " + s);

            }

        }

    }



    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");



    private final String clientId;

    private final MessageType type;

    private final LocalDateTime timestamp;

    private final List<String> payloadParts;



    // --- ctor ---

    public Message(String clientId, MessageType type, LocalDateTime timestamp, List<String> payloadParts) {

        this.clientId = Objects.requireNonNull(clientId, "clientId");

        this.type = Objects.requireNonNull(type, "type");

        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");

        this.payloadParts = Collections.unmodifiableList(new ArrayList<>(payloadParts == null ? List.of() : payloadParts));

    }



    // --- convenience factories ---

    public static Message ofNow(String clientId, MessageType type, String... payload) {

        return new Message(clientId, type, LocalDateTime.now(), payload == null ? List.of() : Arrays.asList(payload));

    }



    // --- PARSE: clientId|timestamp|TYPE|payload... ---

    public static Message parse(String raw) {

        if (raw == null || raw.isBlank())

            throw new IllegalArgumentException("Empty message");



        // brug limit=-1 så tomme felter bevares (fx ved "PRIVATE|bob|")

        String[] parts = raw.split("\\|", -1);

        if (parts.length < 3)

            throw new IllegalArgumentException("Malformed message (need at least 3 parts): " + raw);



        String clientId = parts[0].trim();

        if (clientId.isEmpty())

            throw new IllegalArgumentException("Missing clientId");



        LocalDateTime ts;

        try {

            ts = LocalDateTime.parse(parts[1].trim(), FMT);

        } catch (Exception e) {

            throw new IllegalArgumentException("Bad timestamp, expected yyyy-MM-dd HH:mm:ss: " + parts[1]);

        }



        MessageType type = MessageType.fromStringSafe(parts[2]);



        List<String> payload = new ArrayList<>();

        if (parts.length > 3) {

            for (int i = 3; i < parts.length; i++) payload.add(parts[i]);

        }



        return new Message(clientId, type, ts, payload);

    }



    // --- SERIALIZE to line ---

    public String toLine() {

        StringBuilder sb = new StringBuilder();

        sb.append(clientId).append("|")

                .append(timestamp.format(FMT)).append("|")

                .append(type.name());

        for (String p : payloadParts) sb.append("|").append(p == null ? "" : p);

        return sb.toString();

    }



    // --- getters ---

    public String getClientId() { return clientId; }

    public MessageType getType() { return type; }

    public LocalDateTime getTimestamp() { return timestamp; }

    public List<String> getPayloadParts() { return payloadParts; }

    public String firstPayload() { return payloadParts.isEmpty() ? "" : payloadParts.get(0); }

}