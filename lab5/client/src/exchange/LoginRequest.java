package exchange;

import com.google.gson.annotations.Expose;

public class LoginRequest {
    @Expose
    private String username;

    public LoginRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
