package msg;

import client.Node;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.UUID;

public class AckMessage extends Message {
    private UUID ackUUID;

    public AckMessage(UUID ackUUID, InetAddress ip, int port) {
        super(MsgType.ACK, ip, port);
        this.ackUUID = ackUUID;
    }

    @Override
    public DatagramPacket pack(InetAddress ip, int port) {
        byte [] buf = new byte[BUF_SIZE];
        buf[0] = (byte) type.ordinal();
        packUUIDtoBuf(buf, ackUUID);
        return new DatagramPacket(buf, buf.length, ip, port);
    }

    @Override
    public void handle(Node handler){
        handler.handleAckMessage(this);
    }

    public UUID getAckUUID() {
        return ackUUID;
    }
}
