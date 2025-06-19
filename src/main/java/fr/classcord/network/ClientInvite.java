package fr.classcord.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.json.JSONObject;

public class ClientInvite {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Set<String> connectedUsers = ConcurrentHashMap.newKeySet();
    private Consumer<Set<String>> userListUpdateCallback;
    private Consumer<JSONObject> messageCallback;

    public ClientInvite(Socket socket, PrintWriter out, BufferedReader in) {
        this.socket = socket;
        this.out = out;
        this.in = in;
        startListening();
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    JSONObject json = new JSONObject(line);
                    switch (json.optString("type", "")) {
                        case "status" -> {
                            String user = json.optString("user");
                            if ("online".equalsIgnoreCase(json.optString("state")))
                                connectedUsers.add(user);
                            else
                                connectedUsers.remove(user);
                            if (userListUpdateCallback != null)
                                userListUpdateCallback.accept(Collections.unmodifiableSet(connectedUsers));
                        }
                        case "message" -> {
                            if (messageCallback != null)
                                messageCallback.accept(json);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Connexion interrompue : " + e.getMessage());
            }
        }).start();
    }

    public void setUserListUpdateCallback(Consumer<Set<String>> cb) {
        this.userListUpdateCallback = cb;
    }

    public void setMessageCallback(Consumer<JSONObject> cb) {
        this.messageCallback = cb;
    }

    public void sendMessage(JSONObject msg) {
        out.println(msg.toString());
    }
}
