package server;

public class Message {
    private int id;
    private String message;
    private int author;

    public Message(int id, String message, int author) {
        this.id = id;
        this.message = message;
        this.author = author;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public int getAuthor() {
        return author;
    }
}
