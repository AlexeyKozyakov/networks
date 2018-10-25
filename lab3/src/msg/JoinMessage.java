package msg;

import client.Node;
import exceptions.PackingException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.UUID;

public class JoinMessage extends Message {
    private UUID joinUUID;

    public JoinMessage(UUID joinUUID, InetAddress ip, int port) {
        super(MsgType.JOIN, ip, port);
        this.joinUUID = joinUUID;
    }

    public JoinMessage(UUID joinUUID) {
        super(MsgType.JOIN);
        this.joinUUID = joinUUID;
    }

    @Override
    public DatagramPacket pack(InetAddress ip, int port) {
        byte [] buf = new byte[BUF_SIZE];
        buf[0] = (byte) type.ordinal();
        packUUIDtoBuf(buf, joinUUID);
        return new DatagramPacket(buf, buf.length, ip, port);
    }

    @Override
    public void handle(Node handler) throws IOException, PackingException {
        handler.handleJoinMessage(this);
    }

    public UUID getJoinUUID() {
        return joinUUID;
    }
}
