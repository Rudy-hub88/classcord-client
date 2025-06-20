package fr.classcord.ui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.json.JSONObject;

import fr.classcord.network.ClientInvite;

public class LoginFrame extends JFrame {

    private JTextField ipField = new JTextField("10.0.108.52");
    private JTextField portField = new JTextField("12345");
    private JTextField usernameField = new JTextField("Rudy");
    private JPasswordField passwordField = new JPasswordField("azerty");
    private JButton loginButton = new JButton("Se connecter");
    private JButton registerButton = new JButton("S'inscrire");
    private JButton guestButton = new JButton("Connexion Invité");
    private JLabel statusLabel = new JLabel(" ");

    private ClientInvite client;
    private boolean listenerAdded = false; // Pour ne pas ajouter plusieurs fois

    public LoginFrame(ClientInvite client) {
        super("Connexion");

        this.client = client;

        setLayout(new GridLayout(8, 2, 5, 5));
        setSize(350, 280);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new JLabel("IP serveur :"));
        add(ipField);
        add(new JLabel("Port :"));
        add(portField);
        add(new JLabel("Nom d'utilisateur :"));
        add(usernameField);
        add(new JLabel("Mot de passe :"));
        add(passwordField);
        add(loginButton);
        add(registerButton);
        add(guestButton);
        add(new JLabel("")); // Espace vide pour l'alignement
        add(statusLabel);
        add(new JLabel("")); // Espace vide pour l'alignement

        loginButton.addActionListener(new LoginListener());
        registerButton.addActionListener(e -> {
            // Ouvre la fenêtre d'inscription sans fermer celle-ci
            new RegisterFrame(client);
        });
        guestButton.addActionListener(new GuestListener());

        setVisible(true);
    }

    private class LoginListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            handleConnection("login");
        }
    }

    private class GuestListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            handleGuestConnection();
        }
    }

    private void handleConnection(String actionType) {
        String ip = ipField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Port invalide");
            return;
        }

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Veuillez remplir tous les champs");
            return;
        }

        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
        guestButton.setEnabled(false);
        statusLabel.setText("Connexion en cours...");

        new Thread(() -> {
            try {
                client.connect(ip, port);

                // Ajoute le listener une seule fois
                if (!listenerAdded) {
                    client.addMessageListener(message -> {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                JSONObject response = new JSONObject(message);
                                String type = response.optString("type");
                                String status = response.optString("status");

                                if (status.equals("ok") && (type.equals("login") || type.equals("guest"))) {
                                    new ChatFrame(client, username);
                                    dispose();
                                } else if (type.equals("error")) {
                                    statusLabel.setText("Erreur : " + response.optString("message", "Inconnue"));
                                    enableButtons();
                                }
                            } catch (Exception ex) {
                                statusLabel.setText("Erreur JSON");
                                enableButtons();
                            }
                        });
                    });
                    listenerAdded = true;
                }

                JSONObject json = new JSONObject();
                json.put("type", actionType);
                json.put("username", username);
                json.put("password", password);
                client.send(json.toString());

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erreur de connexion : " + ex.getMessage());
                    enableButtons();
                });
            }
        }).start();
    }

    private void handleGuestConnection() {
        String ip = ipField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Port invalide");
            return;
        }

        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setText("Veuillez entrer un nom d'utilisateur");
            return;
        }

        // Générer un nom d'invité unique avec timestamp
        String guestUsername = "Invité_" + username + "_" + System.currentTimeMillis();

        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
        guestButton.setEnabled(false);
        statusLabel.setText("Création du compte invité...");

        new Thread(() -> {
            try {
                client.connect(ip, port);

                // Ajoute le listener une seule fois
                if (!listenerAdded) {
                    client.addMessageListener(message -> {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                JSONObject response = new JSONObject(message);
                                String type = response.optString("type");
                                String status = response.optString("status");

                                if (status.equals("ok") && type.equals("register")) {
                                    // Inscription réussie, maintenant se connecter
                                    statusLabel.setText("Connexion invité en cours...");
                                    try {
                                        JSONObject loginJson = new JSONObject();
                                        loginJson.put("type", "login");
                                        loginJson.put("username", guestUsername);
                                        loginJson.put("password", "guest123");
                                        client.send(loginJson.toString());
                                    } catch (Exception e) {
                                        statusLabel.setText("Erreur lors de la connexion");
                                        enableButtons();
                                    }
                                } else if (status.equals("ok") && type.equals("login")) {
                                    // Connexion réussie
                                    new ChatFrame(client, guestUsername);
                                    dispose();
                                } else if (type.equals("error")) {
                                    String errorMsg = response.optString("message", "Inconnue");
                                    if (errorMsg.contains("already exists") || errorMsg.contains("existe déjà")) {
                                        // Si le nom existe déjà, essayer avec un autre nom
                                        statusLabel.setText("Nom occupé, nouvel essai...");
                                        handleGuestConnectionRetry(ip, port, username);
                                    } else {
                                        statusLabel.setText("Erreur : " + errorMsg);
                                        enableButtons();
                                    }
                                }
                            } catch (Exception ex) {
                                statusLabel.setText("Erreur JSON");
                                enableButtons();
                            }
                        });
                    });
                    listenerAdded = true;
                }

                // D'abord essayer de créer un compte invité
                JSONObject registerJson = new JSONObject();
                registerJson.put("type", "register");
                registerJson.put("username", guestUsername);
                registerJson.put("password", "guest123");
                client.send(registerJson.toString());

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erreur de connexion : " + ex.getMessage());
                    enableButtons();
                });
            }
        }).start();
    }

    private void handleGuestConnectionRetry(String ip, int port, String baseUsername) {
        // Générer un nouveau nom avec un timestamp différent
        String guestUsername = "Invité_" + baseUsername + "_" + (System.currentTimeMillis() + (int)(Math.random() * 1000));
        
        try {
            JSONObject registerJson = new JSONObject();
            registerJson.put("type", "register");
            registerJson.put("username", guestUsername);
            registerJson.put("password", "guest123");
            client.send(registerJson.toString());
        } catch (Exception ex) {
            statusLabel.setText("Erreur lors du retry : " + ex.getMessage());
            enableButtons();
        }
    }

    private void enableButtons() {
        loginButton.setEnabled(true);
        registerButton.setEnabled(true);
        guestButton.setEnabled(true);
    }
}