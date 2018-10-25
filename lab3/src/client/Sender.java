package client;

import msg.JoinMessage;
import msg.Message;
import msg.MsgType;
import msg.TextMessage;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;

public class Sender{
    private Node owner;
    DatagramSocket socket;
    Map<InetSocketAddress, Long> neighbours;
    Map<UUID, Message> latestMessages;

    public Sender(Node owner) {
        this.owner = owner;
        socket = owner.getSocket();
        neighbours = owner.getNeighbours();
        latestMessages = owner.getLatestMessages();
    }

    public void resendTextMessage(TextMessage msg) throws IOException {
        if (!neighbours.isEmpty()) {
            for (InetSocketAddress addr : neighbours.keySet()) {
                if (!addr.equals(msg.getAddr())) {
                    msg.send(socket, addr);
                }
            }
            msg.markAsSent();
            latestMessages.put(msg.getMsgUUID(), msg);
        }

    }

    public void resendToNotAcked(Message msg) throws IOException {
        msg.getAcks().keySet().removeIf(key -> !neighbours.containsKey(key));
        for (Map.Entry<InetSocketAddress, Boolean> entry : msg.getAcks().entrySet()) {
            if (!entry.getValue()) {
                sendMessage(msg, entry.getKey());
            }
        }
    }

    public void sendMessageToNeignbours(Message msg) throws IOException {
        if (!neighbours.isEmpty()) {
            for (InetSocketAddress addr : neighbours.keySet()) {
                sendMessage(msg, addr);
            }
            msg.markAsSent();
            if (msg instanceof JoinMessage) {
                latestMessages.put(((JoinMessage) msg).getJoinUUID(), msg);
            }
            if (msg instanceof TextMessage) {
                latestMessages.put(((TextMessage) msg).getMsgUUID(), msg);
            }
        }
        if (msg.getType() == MsgType.LEAVE) {
            socket.close();
        }
    }

    public void sendMessage(Message msg, InetSocketAddress addr) throws IOException{
        msg.send(socket, addr);
        msg.markAsSent();
        if (msg instanceof JoinMessage) {
            latestMessages.put(((JoinMessage) msg).getJoinUUID(), msg);
        }
    }
}
