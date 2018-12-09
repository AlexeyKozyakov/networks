package server;

public class User {
    private int id;
    private String username;
    private Boolean online;
    private long lastRequestTime;

    public User(int id, String username) {
        this.id = id;
        this.username = username;
        this.online = true;
        this.lastRequestTime = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Boolean getOnline() {
        return online;
    }

    public long getLastRequestTime() {
        return lastRequestTime;
    }

    public void markAsOnline() {
        lastRequestTime = System.currentTimeMillis();
        online = true;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }
}
