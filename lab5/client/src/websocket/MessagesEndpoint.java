package websocket;

import client.Client;
import client.Message;
import com.google.gson.Gson;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;

@ClientEndpoint
public class MessagesEndpoint {

    private Gson gson = new Gson();

    private Client client;

    public MessagesEndpoint(Client client) {
        this.client = client;
    }

    @OnMessage
    public void onMessage(String message) {
        client.showMessage(gson.fromJson(message, Message.class));
    }

}
