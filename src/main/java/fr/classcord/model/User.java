package fr.classcord.model;

public class User {
    private String username;
    private String status;

    public User(String username, String status) {
        this.username = username;
        this.status = status;
    }

    public String getUsername() {
        return this.username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String toString() {
        return username;
    }
    

    // Constructeurs, getters/setters
}