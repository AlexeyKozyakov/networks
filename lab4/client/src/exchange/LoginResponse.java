package exchange;

import com.google.gson.annotations.Expose;

import java.util.UUID;

public class LoginResponse {
    @Expose
    private int id;
    @Expose
    private String username;
    @Expose
    private Boolean online;
    @Expose
    private UUID token;


    public LoginResponse(int id, String username, Boolean online, UUID token) {
        this.id = id;
        this.username = username;
        this.online = online;
        this.token = token;
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

    public UUID getToken() {
        return token;
    }
}
