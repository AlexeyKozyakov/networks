package exchange;

import com.google.gson.annotations.Expose;

public class MessageRequest {
    @Expose
    private String message;

    public MessageRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
