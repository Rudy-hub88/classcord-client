package fr.classcord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.json.JSONObject;

import fr.classcord.app.ChatFrame;
import fr.classcord.network.ClientInvite;

public class Main {
    private static String username;

    public static void main(String[] args) {
        try {
            String serverAddress = JOptionPane.showInputDialog("Adresse IP du serveur :");
            String portStr = JOptionPane.showInputDialog("Port :");
            int port = Integer.parseInt(portStr);
            String pseudo = JOptionPane.showInputDialog("Pseudo :");
            username = pseudo;

            Socket socket = new Socket(serverAddress, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            showLoginWindow(out, in, socket);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Connexion échouée : " + e.getMessage());
        }
    }

    private static void showLoginWindow(PrintWriter out, BufferedReader in, Socket socket) {
        JFrame frame = new JFrame("Connexion ClassCord");
        JPanel panel = new JPanel(new java.awt.GridLayout(3, 2));
        JTextField usernameField = new JTextField(username);
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Se connecter");
        JButton registerButton = new JButton("S’inscrire");

        panel.add(new JLabel("Nom d’utilisateur :"));
        panel.add(usernameField);
        panel.add(new JLabel("Mot de passe :"));
        panel.add(passwordField);
        panel.add(loginButton);
        panel.add(registerButton);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Consumer<String> auth = (type) -> {
            try {
                JSONObject req = new JSONObject();
                req.put("type", type);
                req.put("username", usernameField.getText().trim());
                req.put("password", new String(passwordField.getPassword()).trim());

                out.println(req.toString());
                JSONObject res = new JSONObject(in.readLine());

                if ("ok".equalsIgnoreCase(res.optString("status"))) {
                    username = usernameField.getText().trim();
                    frame.dispose();
                    ClientInvite client = new ClientInvite(socket, out, in);
                    ChatFrame chat = new ChatFrame(client, username);
                    chat.launch();
                } else {
                    JOptionPane.showMessageDialog(null, res.optString("message", "Erreur inconnue"));
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Erreur : " + ex.getMessage());
            }
        };

        loginButton.addActionListener(e -> auth.accept("login"));
        registerButton.addActionListener(e -> auth.accept("register"));
        passwordField.addActionListener(e -> loginButton.doClick());
    }
}
