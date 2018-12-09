package exchange;

import com.google.gson.annotations.Expose;

public class LogoutResponse {
    @Expose
    private String message;

    public LogoutResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
