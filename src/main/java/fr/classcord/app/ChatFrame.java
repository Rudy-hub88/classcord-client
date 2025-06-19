package fr.classcord.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.json.JSONObject;

import fr.classcord.network.ClientInvite;

public class ChatFrame extends JFrame {
    private final JTextArea messageArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final DefaultListModel<String> userModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userModel);
    private final ClientInvite client;
    private final String username;

    public ChatFrame(ClientInvite client, String username) {
        this.client = client;
        this.username = username;

        setTitle("ClassCord - " + username);
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        messageArea.setEditable(false);
        add(new JScrollPane(messageArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JButton sendBtn = new JButton("Envoyer");
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(userList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Connectés"));
        scrollPane.setPreferredSize(new Dimension(150, getHeight()));
        add(scrollPane, BorderLayout.EAST);

        client.setUserListUpdateCallback(this::updateUserList);
        client.setMessageCallback(this::receiveMessage);

        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    private void updateUserList(Set<String> users) {
        SwingUtilities.invokeLater(() -> {
            userModel.clear();
            users.forEach(userModel::addElement);
        });
    }

    private void receiveMessage(JSONObject msg) {
        String from = msg.optString("from", "Serveur");
        String content = msg.optString("content", "");
        String subtype = msg.optString("subtype", "global");

        String formatted = "private".equalsIgnoreCase(subtype)
                ? "[MP de " + from + "] " + content
                : from + " : " + content;

        SwingUtilities.invokeLater(() -> messageArea.append(formatted + "\n"));
    }

    private void sendMessage() {
        String content = inputField.getText().trim();
        if (content.isEmpty()) return;

        JSONObject msg = new JSONObject();
        msg.put("type", "message");
        msg.put("from", username);
        msg.put("content", content);

        String to = userList.getSelectedValue();
        if (to != null) {
            msg.put("subtype", "private");
            msg.put("to", to);
            appendOwnMessage("[MP à " + to + "] " + content);
        } else {
            msg.put("subtype", "global");
            msg.put("to", "global");
            appendOwnMessage("Vous : " + content);
        }
        client.sendMessage(msg);
        inputField.setText("");

    }

    private void appendOwnMessage(String text) {
    SwingUtilities.invokeLater(() -> {
        messageArea.append(text + "\n");
    });
}


    public void launch() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}
