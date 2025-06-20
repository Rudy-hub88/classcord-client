package fr.classcord.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.json.JSONObject;

import fr.classcord.model.User;
import fr.classcord.network.ClientInvite;

public class ChatFrame extends JFrame {
    private String username;
    private JTextField messageField;
    private JButton sendButton;
    private JPanel messagePanel;
    private JScrollPane scrollPane;
    private ClientInvite client;

    private Set<String> onlineUsers = new HashSet<>();
    private DefaultListModel<User> userListModel = new DefaultListModel<>();
    private JList<User> userList = new JList<>(userListModel);
    private Map<String, String> userStatuses = new HashMap<>();

    private JLabel conversationLabel;
    private JComboBox<String> statusComboBox;
    private javax.swing.Timer refreshTimer;

    public ChatFrame(ClientInvite client, String username) {
        this.client = client;
        this.username = username;

        setTitle("Classcord - Connecté en tant que " + username);
        setSize(700, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(messagePanel);
        add(scrollPane, BorderLayout.CENTER);

        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setCellRenderer(new UserCellRenderer());
        JScrollPane userListScroll = new JScrollPane(userList);
        userListScroll.setPreferredSize(new Dimension(150, 0));
        add(userListScroll, BorderLayout.EAST);

        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Envoyer");
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        conversationLabel = new JLabel("Discussion : Global");
        conversationLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        String[] statuses = {"online", "away", "dnd", "invisible"};
        statusComboBox = new JComboBox<>(statuses);
        statusComboBox.setSelectedItem("online");
        statusComboBox.addActionListener(e -> {
            String selectedStatus = (String) statusComboBox.getSelectedItem();
            sendStatusChange(username, selectedStatus);
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(conversationLabel, BorderLayout.CENTER);
        topPanel.add(statusComboBox, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        userList.addListSelectionListener(e -> updateConversationLabel());

        client.addMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");

                    switch (type) {
                        case "message" -> handleMessage(json);
                        case "status" -> handleStatus(json);
                        case "users" -> handleListUsersResponse(json);
                    }
                } catch (Exception ignored) {}
            });
        });

        onlineUsers.add(username);
        refreshUserList();
        requestOnlineUsersList();

        // Timer de rafraîchissement toutes les 3 secondes
        refreshTimer = new javax.swing.Timer(3000, e -> requestOnlineUsersList());
        refreshTimer.start();

        setVisible(true);
    }

    private void requestOnlineUsersList() {
        try {
            JSONObject request = new JSONObject();
            request.put("type", "users");
            client.send(request.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleListUsersResponse(JSONObject json) {
        onlineUsers.clear();
        onlineUsers.add(username);
        for (Object obj : json.getJSONArray("users")) {
            String user = (String) obj;
            onlineUsers.add(user);
        }
        refreshUserList();
    }

    private void handleMessage(JSONObject json) {
        String from = json.getString("from");
        String content = json.getString("content");
        String subtype = json.optString("subtype", "global");

        if ("private".equals(subtype)) {
            String to = json.optString("to", "");
            if (from.equals(username) || to.equals(username)) {
                displayMessage("MP de " + from, content, "private");
            }
        } else {
            displayMessage(from, content, "global");
        }
    }

    private void handleStatus(JSONObject json) {
        String user = json.getString("user");
        String state = json.getString("state");

        userStatuses.put(user, state);
        if ("invisible".equals(state) || "offline".equals(state)) {
            onlineUsers.remove(user);
        } else {
            onlineUsers.add(user);
        }
        refreshUserList();
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        JSONObject json = new JSONObject();
        json.put("type", "message");
        json.put("from", username);
        json.put("content", text);

        User selectedUser = userList.getSelectedValue();
        String selectedUsername = selectedUser != null ? selectedUser.getUsername() : null;

        if (selectedUsername != null && !selectedUsername.equals("Global") && !selectedUsername.equals(username)) {
            json.put("subtype", "private");
            json.put("to", selectedUsername);
            displayMessage("Moi → " + selectedUsername, text, "private");
        } else {
            json.put("subtype", "global");
            displayMessage(username, text, "global");
        }

        try {
            client.send(json.toString());
            messageField.setText("");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors de l'envoi du message", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayMessage(String from, String content, String subtype) {
        JPanel messageLine = new JPanel();
        messageLine.setLayout(new BoxLayout(messageLine, BoxLayout.X_AXIS));
        messageLine.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        boolean isMine = from.startsWith("Moi") || from.equals(username);
        JLabel label;

        if ("private".equals(subtype)) {
            label = new JLabel("Message privé de " + from + ": " + content);
            label.setForeground(Color.GREEN);
            label.setFont(new Font("Arial", Font.ITALIC, 14));
        } else {
            label = new JLabel(from + " : " + content);
            label.setForeground(isMine ? Color.BLACK : Color.BLUE);
            label.setFont(new Font("Arial", Font.PLAIN, 14));
        }

        if (isMine) {
            messageLine.add(Box.createHorizontalGlue());
            messageLine.add(label);
        } else {
            messageLine.add(label);
            messageLine.add(Box.createHorizontalGlue());
        }

        messageLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.add(messageLine);
        messagePanel.revalidate();
        messagePanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void refreshUserList() {
        userListModel.clear();
        userListModel.addElement(new User("Global", "none"));

        java.util.List<String> sortedUsers = new ArrayList<>(onlineUsers);
        Collections.sort(sortedUsers);

        for (String user : sortedUsers) {
            if (!user.equals("Global")) {
                String status = userStatuses.getOrDefault(user, "online");
                userListModel.addElement(new User(user, status));
            }
        }

        if (userList.getSelectedValue() == null) {
            userList.setSelectedIndex(0);
        }

        updateConversationLabel();
    }

    private void updateConversationLabel() {
        User selected = userList.getSelectedValue();
        if (selected == null || selected.getUsername().equals("Global")) {
            conversationLabel.setText("Discussion : Global");
        } else {
            conversationLabel.setText("Discussion : MP avec " + selected.getUsername());
        }
    }

    private void sendStatusChange(String username, String status) {
        try {
            // Mettre à jour le statut localement immédiatement
            userStatuses.put(username, status);
            
            // Gérer la visibilité de l'utilisateur dans la liste
            if ("invisible".equals(status)) {
                // Si l'utilisateur devient invisible, le retirer de la liste des utilisateurs en ligne
                // mais garder son propre nom dans la liste
                if (!username.equals(this.username)) {
                    onlineUsers.remove(username);
                }
            } else {
                // Sinon, s'assurer qu'il est dans la liste
                onlineUsers.add(username);
            }
            
            // Rafraîchir l'affichage de la liste des utilisateurs
            refreshUserList();
            
            // Envoyer le changement au serveur
            JSONObject statusChange = new JSONObject();
            statusChange.put("type", "status");
            statusChange.put("user", username);
            statusChange.put("state", status);
            client.send(statusChange.toString());
            
        } catch (IOException e) {
            // En cas d'erreur d'envoi, annuler le changement local
            userStatuses.put(username, statusComboBox.getSelectedItem().toString());
            refreshUserList();
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du changement de statut", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}