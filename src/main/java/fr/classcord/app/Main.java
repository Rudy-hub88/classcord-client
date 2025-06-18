package fr.classcord.app;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.json.JSONObject;

public class Main {

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static String username;
    private static JTextArea chatArea;

    public static void main(String[] args) {
        try {
            String serverAddress = JOptionPane.showInputDialog("Adresse IP du serveur :");
            if (serverAddress == null || serverAddress.trim().isEmpty()) {
                System.exit(0);
            }
            
            String portStr = JOptionPane.showInputDialog("Port :");
            if (portStr == null || portStr.trim().isEmpty()) {
                System.exit(0);
            }
            int port = Integer.parseInt(portStr);
            
            String inputUsername = JOptionPane.showInputDialog("Pseudo :");
            if (inputUsername == null || inputUsername.trim().isEmpty()) {
                System.exit(0);
            }
            username = inputUsername;

            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            showLoginWindow();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Connexion impossible : " + e.getMessage());
            System.exit(1);
        }
    }

    private static void showLoginWindow() {
        JFrame frame = new JFrame("Connexion ClassCord");
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2));

        JTextField usernameField = new JTextField(username);
        JPasswordField passwordField = new JPasswordField();

        JButton loginButton = new JButton("Se connecter");
        JButton registerButton = new JButton("S'inscrire");

        panel.add(new JLabel("Username :"));
        panel.add(usernameField);
        panel.add(new JLabel("Password :"));
        panel.add(passwordField);
        panel.add(loginButton);
        panel.add(registerButton);

        frame.add(panel);
        frame.setVisible(true);

        loginButton.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            sendAuthRequest("login", user, pass, frame);
        });

        registerButton.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            sendAuthRequest("register", user, pass, frame);
        });

        // Permettre d'appuyer sur Entrée pour se connecter
        passwordField.addActionListener(e -> loginButton.doClick());
    }

    private static void sendAuthRequest(String type, String user, String pass, JFrame frame) {
        try {
            JSONObject request = new JSONObject();
            request.put("type", type);
            request.put("username", user);
            request.put("password", pass);
            
            System.out.println("Envoi de la requête d'authentification : " + request.toString());
            out.println(request.toString());

            String response = in.readLine();
            System.out.println("Réponse du serveur : " + response);
            
            if (response == null) {
                JOptionPane.showMessageDialog(null, "Aucune réponse du serveur", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JSONObject jsonResponse = new JSONObject(response);

            if (jsonResponse.has("type")) {
                String responseType = jsonResponse.getString("type");

                switch (responseType) {
                    case "login":
                    case "register":
                        if (jsonResponse.has("status") && jsonResponse.getString("status").equalsIgnoreCase("ok")) {
                            username = user;
                            JOptionPane.showMessageDialog(null, "Bienvenue " + username + " !");
                            frame.dispose();
                            openChatWindow();
                        } else {
                            String message = jsonResponse.has("message") ? jsonResponse.getString("message") : "Erreur inconnue.";
                            JOptionPane.showMessageDialog(null, message, "Erreur", JOptionPane.ERROR_MESSAGE);
                        }
                        break;

                    case "loginfailed":
                    case "registerfailed":
                        String errorMessage = jsonResponse.has("message") ? jsonResponse.getString("message") : "Échec.";
                        JOptionPane.showMessageDialog(null, errorMessage, "Erreur", JOptionPane.ERROR_MESSAGE);
                        break;

                    default:
                        JOptionPane.showMessageDialog(null, "Réponse inattendue : " + jsonResponse.toString(), "Erreur serveur", JOptionPane.ERROR_MESSAGE);
                        break;
                }
            } else {
                JOptionPane.showMessageDialog(null, "Réponse malformée : " + jsonResponse.toString(), "Erreur serveur", JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException | org.json.JSONException ex) {
            JOptionPane.showMessageDialog(null, "Erreur lors de l'envoi : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void openChatWindow() {
        JFrame chatFrame = new JFrame("ClassCord - Connecté en tant que " + username);
        chatFrame.setSize(500, 400);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JTextField messageField = new JTextField();
        JButton sendButton = new JButton("Envoyer");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatFrame.add(scrollPane, BorderLayout.CENTER);
        chatFrame.add(inputPanel, BorderLayout.SOUTH);

        chatFrame.setVisible(true);

        // Ajouter message de bienvenue
        appendToChatArea("=== Connecté au chat ClassCord ===\n");

        // Thread de réception des messages
        Thread messageReceiver = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("Reçu du serveur : " + line);
                    
                    try {
                        JSONObject msgJson = new JSONObject(line);
                        String messageType = msgJson.optString("type", "");
                        
                        switch (messageType) {
                            case "message":
                                String sender = msgJson.optString("from", "Inconnu");
                                String content = msgJson.optString("content", "");
                                appendToChatArea(sender + " : " + content + "\n");
                                break;
                                
                            case "notification":
                                String notification = msgJson.optString("message", "");
                                appendToChatArea("*** " + notification + " ***\n");
                                break;
                                
                            case "userlist":
                                // Gestion de la liste des utilisateurs si le serveur l'envoie
                                appendToChatArea("=== Utilisateurs connectés ===\n");
                                break;
                                
                            default:
                                appendToChatArea("Serveur : " + line + "\n");
                                break;
                        }
                    } catch (org.json.JSONException jsonEx) {
                        appendToChatArea("Message du serveur : " + line + "\n");
                        System.err.println("Erreur de parsing JSON : " + jsonEx.getMessage());
                    }
                }
            } catch (IOException e) {
                appendToChatArea("*** Déconnecté du serveur ***\n");
                System.err.println("Connexion fermée : " + e.getMessage());
            }
        });
        messageReceiver.setDaemon(true);
        messageReceiver.start();

        // Envoi des messages
        Runnable sendMessage = () -> {
            String msg = messageField.getText().trim();
            if (!msg.isEmpty()) {
                try {
                    // Afficher immédiatement notre propre message
                    appendToChatArea("Vous : " + msg + "\n");
                    
                    // Envoyer au serveur
                    JSONObject messageJson = new JSONObject();
                    messageJson.put("type", "message");
                    messageJson.put("from", username);
                    messageJson.put("content", msg);

                    System.out.println("Envoi du message : " + messageJson.toString());
                    out.println(messageJson.toString());
                    messageField.setText("");
                    
                } catch (Exception ex) {
                    appendToChatArea("*** Erreur lors de l'envoi du message ***\n");
                    System.err.println("Erreur d'envoi : " + ex.getMessage());
                }
            }
        };

        sendButton.addActionListener(e -> sendMessage.run());
        messageField.addActionListener(e -> sendMessage.run());

        // Focus sur le champ de message
        messageField.requestFocus();

        // Gérer la fermeture de la fenêtre
        chatFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        // Envoyer un message de déconnexion si nécessaire
                        JSONObject disconnectMsg = new JSONObject();
                        disconnectMsg.put("type", "disconnect");
                        disconnectMsg.put("from", username);
                        out.println(disconnectMsg.toString());
                        
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Erreur lors de la fermeture : " + e.getMessage());
                }
                System.exit(0);
            }
        });
    }

    // Méthode pour ajouter du texte au chat de manière thread-safe
    private static void appendToChatArea(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text);
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
}