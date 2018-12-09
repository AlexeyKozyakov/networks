package websocket;

import server.Server;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/messages")
public class MessagesEndpoint{

    private static Map<Session, UUID> sessions = new ConcurrentHashMap<>();
    private static Server server;

    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        sessions.remove(session);
    }

    @OnMessage
    public void message(Session session, String msg) throws IOException {
        if (!sessions.containsKey(session)) {
            UUID uuid = server.authorization(session, msg);
            if (uuid == null) {
                session.close();
            } else {
                sessions.put(session, uuid);
            }
        } else {
            session.getBasicRemote().sendText("bad_request");
        }
    }

    public static void broadcast(String msg) throws IOException {
        for (Session s : sessions.keySet()) {
            if (s.isOpen()) {
                s.getBasicRemote().sendText(msg);
            }
        }
    }

    public static void setServer(Server server){
        MessagesEndpoint.server = server;
    }
}
