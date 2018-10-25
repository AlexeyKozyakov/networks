package msg;

import client.Node;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class PingMessage extends Message {
    public PingMessage(InetAddress ip, int port) {
        super(MsgType.PING, ip, port);
    }

    public PingMessage() {super(MsgType.PING);}

    @Override
    public DatagramPacket pack(InetAddress ip, int port) {
        byte [] buf = new byte[BUF_SIZE];
        buf[0] = (byte) type.ordinal();
        return new DatagramPacket(buf, buf.length, ip, port);
    }

    @Override
    public void handle(Node handler) {
        handler.handlePingMessage(this);
    }
}
