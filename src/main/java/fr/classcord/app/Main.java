package fr.classcord.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import org.json.JSONObject;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Demande IP
        System.out.print("💻 Adresse IP du serveur : ");
        String ipAddress = scanner.nextLine();

        // Demande Port
        System.out.print("🔌 Port : ");
        int port = scanner.nextInt();
        scanner.nextLine(); // flush

        // Demande pseudo
        System.out.print("👤 Ton pseudo : ");
        String pseudo = scanner.nextLine();

        try (Socket socket = new Socket(ipAddress, port)) {
            System.out.println("✅ Connecté au serveur " + ipAddress + ":" + port);

            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Création du JSON
            JSONObject message = new JSONObject();
            message.put("action", "connect");
            message.put("pseudo", pseudo);

            // Envoi
            out.println(message.toString());
            System.out.println("📤 Message envoyé : " + message.toString());

            // Réception réponse serveur
            String response = in.readLine();
            if (response != null) {
                JSONObject jsonResponse = new JSONObject(response);
                System.out.println("📥 Réponse serveur : " + jsonResponse.toString());
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }
}
