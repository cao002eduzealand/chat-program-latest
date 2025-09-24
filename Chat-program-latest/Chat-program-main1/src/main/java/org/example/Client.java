package org.example;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.*;

public class Client {
    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 5001;

        try {
            Socket socket = new Socket(hostname, port);
            Scanner scanner = new Scanner(System.in);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            String clientId = "c" + socket.getLocalPort();
            System.out.println("Connected to " + hostname + ":" + port);
            System.out.println("Your client ID: " + clientId);
            System.out.println("Commands:");
            System.out.println("  /login <username> [password] - Login with protocol");
            System.out.println("  /join <room> - Join room with protocol");
            System.out.println("  /pm <user> <message> - Private message with protocol");
            System.out.println("  :emoji: - Send emoji");
            System.out.println("  filename.ext - Send as file transfer");
            System.out.println("  Or use traditional /commands");

            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = reader.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (SocketException e) {
                    System.err.println("Connection lost");
                } catch (IOException e) {
                    System.err.println("I/O error: " + e.getMessage());
                }
            });
            receiveThread.start();



            String input;

            while ((input = scanner.nextLine()) != null) {

                if (input.startsWith("/login ")) {

                    // /login bob hunter2

                    String[] toks = input.split("\\s+", 3);

                    String user = toks.length > 1 ? toks[1] : "";

                    String pass = toks.length > 2 ? toks[2] : "";

                    String line = Message.ofNow(clientId, Message.MessageType.LOGIN, user, pass).toLine();

                    writer.println(line);



                } else if (input.startsWith("/join ")) {

                    String room = input.substring(6).trim();

                    String line = Message.ofNow(clientId, Message.MessageType.JOIN_ROOM, room).toLine();

                    writer.println(line);



                } else {

                    // almindelig tekst

                    String line = Message.ofNow(clientId, Message.MessageType.TEXT, input).toLine();

                    writer.println(line);

                }

            }

        } catch (UnknownHostException e) {
            System.err.println("Can't find server at " + hostname);
        } catch (ConnectException e) {
            System.err.println("Can't connect to " + hostname + ":" + port);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }

    private static boolean isFileMessage(String text) {
        String[] fileExtensions = {".pdf", ".jpg", ".png", ".gif", ".txt", ".doc", ".docx", ".zip", ".mp3", ".mp4"};
        String lowerText = text.toLowerCase();
        for (String ext : fileExtensions) {
            if (lowerText.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}