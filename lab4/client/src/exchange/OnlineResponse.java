package exchange;

import com.google.gson.annotations.Expose;
import client.User;

public class OnlineResponse {
    @Expose
    private int id;
    @Expose
    private String username;
    @Expose
    private Boolean online;

    public OnlineResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.online = user.getOnline();
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
}
