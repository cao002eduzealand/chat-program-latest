package org.example;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private String clientId;
    private BufferedReader reader;
    private PrintWriter writer;
    private Server server;
    private String username;
    private Map<String, Runnable> commandMap;
    private RoomManager roomManager;
    private Room currentRoom;
    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.clientId = "Client-"+socket.getPort();
        this.roomManager = server.getRoomManager();

        initializeCommandMap();
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (
                IOException e) {
            System.err.println("Failed to get streams for " + clientId + ": " + e.getMessage());
        }
    }
    private void initializeCommandMap(){
        commandMap = new HashMap<>();
        commandMap.put("/help", this::sendHelpMessage);
        commandMap.put("/rooms", this::listRooms);
        commandMap.put("/leave", this::leaveRoom);
        commandMap.put("/who", this::showWhoInRoom);
        commandMap.put("/quit", this::quitClient);
        commandMap.put("/exit", this::quitClient);

    }

    public void sendMessage(String message) {
        if (writer != null){
            writer.println(message);
        }
    }

    public String getUsername() {
        return username != null ? username : clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
    }

    private void handleParsedMessage(Message msg) {

        switch (msg.getType()) {

            case LOGIN: {

                String user = msg.getPayloadParts().size() >= 1 ? msg.getPayloadParts().get(0) : "";

                this.username = (user == null || user.isBlank()) ? this.getClientId() : user;

                sendMessage("Hello " + username + "! You are now connected.");

                if (roomManager.joinRoom(this, "Lobby")) {

                    this.setCurrentRoom(roomManager.findRoomByName("Lobby"));

                    sendMessage("You automatically joined the Lobby room!");

                }

                break;

            }

            case JOIN_ROOM: {

                String roomName = msg.firstPayload();

                if (roomName == null || roomName.isBlank()) {

                    sendMessage("Usage: JOIN_ROOM requires a room name");

                    return;

                }

                joinRoom(roomName);

                break;

            }

            case TEXT: {

                if (getCurrentRoom() == null) {

                    sendMessage("You are not in any room. Use JOIN_ROOM first.");

                    return;

                }

                String content = String.join("|", msg.getPayloadParts()); // bevar '|' i tekst

                String formatted = getUsername() + ": " + content;

                getCurrentRoom().broadcastToRoom(formatted, this);

                sendMessage("[You]: " + content);

                break;

            }

            // nemt at udvide med:

            // case EMOJI: ...

            // case PRIVATE: payload: target | message...

            // case FILE_TRANSFER: payload: filename | size -> Læs 'size' raw bytes efter denne linje

            default:

                sendMessage("Type not implemented yet: " + msg.getType());

        }

    }

//    private void handleTraditionalCommand(String command) {
//        String[] parts = command.split("\\s+", 2);
//        String cmd = parts[0].toLowerCase();
//
//        switch (cmd) {
//            case "/help":
//                sendHelpMessage();
//                break;
//            case "/rooms":
//                listRooms();
//                break;
//            case "/who":
//                showWhoInRoom();
//                break;
//            case "/leave":
//                leaveRoom();
//                break;
//            case "/quit":
//            case "/exit":
//                quitClient();
//                break;
//            case "/join":
//                // Convert to protocol message
//                if (parts.length > 1) {
//                    Message joinMsg = Message.of(clientId, MessageType.JOIN_ROOM, parts[1]);
//                    handleParsedMessage(joinMsg);
//                } else {
//                    sendMessage("Usage: /join <roomname>");
//                }
//                break;
//            case "/pm":
//                // Convert to protocol message
//                if (parts.length > 1) {
//                    String[] pmParts = parts[1].split("\\s+", 2);
//                    if (pmParts.length == 2) {
//                        String target = pmParts[0];
//                        String content = pmParts[1];
//                        Message privateMsg = Message.of(clientId, MessageType.PRIVATE, target + "|" + content);
//                        handleParsedMessage(privateMsg);
//                    } else {
//                        sendMessage("Usage: /pm <username> <message>");
//                    }
//                } else {
//                    sendMessage("Usage: /pm <username> <message>");
//                }
//                break;
//            default:
//                sendMessage("Unknown command: " + cmd);
//                sendMessage("Type /help for available commands.");
//        }
//    }


    @Override
    public void run() {
        try {
            server.addClient(this);

            sendMessage("Welcome! Please enter your username: ");
           String usernameInput = reader.readLine();

            if (usernameInput == null || usernameInput.trim().isEmpty()) {
                username = clientId;
            }
            else username= usernameInput.trim();

            sendMessage("Hello " + username + "! You are now connected to the chat server.");

            // Automatisk join Lobby når bruger forbinder
            if (roomManager.joinRoom(this, "Lobby")) {
                this.currentRoom = roomManager.findRoomByName("Lobby");
                sendMessage("You automatically joined the Lobby room!");
            }

            sendHelpMessage();

            String raw;

            while ((raw = reader.readLine()) != null) {

                try {

                    Message msg = Message.parse(raw);

                    handleParsedMessage(msg);

                } catch (Exception e) {

                    sendMessage("Parse error: " + e.getMessage());

                }

            }

        } catch (IOException e) {
            System.err.println("Error with client " + clientId + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }


    private void sendHelpMessage() {
        sendMessage("=== CHAT COMMANDS ===\n" +
                "/join <room>  - Join a room (Lobby, testRoom1, testRoom2, testRoom3, testRoom4)\n" +
                "/leave        - Leave current room\n" +
                "/rooms        - List all rooms\n" +
                "/who          - Show users in current room\n" +
                "/help         - Show this message again\n" +
                "/quit         - Leave the chat");
    }

    private void listRooms(){
        sendMessage("-----AVAILABLE ROOMS------");
        for (Room room : roomManager.getAllRooms()) {
            int occupants= room.howManyInroom();
            sendMessage(room.getRoomName()+" "+occupants+"/"+room.getMaxCapacity());
        }
    }

    private void leaveRoom() {
        if (currentRoom==null) {
            sendMessage("You are not in a room");
            return;
        }
        String roomName = currentRoom.getRoomName();
        if (currentRoom.removeClient(this)){
            sendMessage("You have left the room "+roomName);
            currentRoom = null;
        }
    }
    private void showWhoInRoom() {
        if (currentRoom != null) {
            sendMessage("=== USERS IN " + currentRoom.getRoomName().toUpperCase() + " ===");
            for (String name : currentRoom.clientNamesInRoom()) {
                sendMessage("- " + name);
            }
        } else {
            sendMessage("You are not in any room.");
        }
    }
    private void joinRoom(String roomName) {
        if (roomManager.joinRoom(this, roomName)) {
            Room room = roomManager.findRoomByName(roomName);
            this.currentRoom = room;
            sendMessage("You joined room: " + roomName);
        } else {
            Room room = roomManager.findRoomByName(roomName);
            if (room == null) {
                sendMessage("Room '" + roomName + "' does not exist.");
                sendMessage("Available rooms: Lobby, testRoom1, testRoom2, testRoom3, testRoom4");
            } else if (room.isRoomFull()) {
                sendMessage("Room '" + roomName + "' is full!");
            } else {
                sendMessage("Could not join room: " + roomName);
            }
        }
    }
    private void quitClient() {
        sendMessage("Goodbye!");
        try {
            socket.close();
        } catch (IOException e) {
            // Handle quietly
        }
    }
    private void cleanup() {
        try {
            // Leave current room
            if (currentRoom != null) {
                currentRoom.removeClient(this);
            }

            // Remove from server
            server.removeClient(this);

            // Close streams and socket
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();

            System.out.println(username + " (" + clientId + ") disconnected");

        } catch (IOException e) {
            System.err.println("Error during cleanup for " + clientId + ": " + e.getMessage());
        }
    }

}
