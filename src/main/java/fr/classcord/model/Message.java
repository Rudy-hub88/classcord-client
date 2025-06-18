package fr.classcord.model;

public class Message {
    public String type;
    public String subtype;
    public String from;
    public String to;
    public String content;
    public String timestamp;

    public Message() {}

    public Message(String type, String subtype, String from, String to, String content, String timestamp) {
        this.type = type;
        this.subtype = subtype;
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters / Setters si besoin
}
