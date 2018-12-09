package server;

import com.google.gson.annotations.Expose;

public class Message {
    @Expose
    private int id;
    @Expose
    private String message;
    @Expose
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
