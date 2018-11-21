package exchange;

import com.google.gson.annotations.Expose;
import client.Message;

import java.util.List;

public class MessagesResponse {
    @Expose
    private List<Message> messages;

    public MessagesResponse(List<Message> messages) {
        this.messages = messages;
    }

    public List<Message> getMessages() {
        return messages;
    }
}
