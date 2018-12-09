package exchange;

import com.google.gson.annotations.Expose;

public class MessageResponse {
    @Expose
    private int id;
    @Expose
    private String message;

    public MessageResponse(int id, String message) {
        this.id = id;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}
