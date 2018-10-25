package msg;

import client.Node;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class LeaveMessage extends Message {
    public LeaveMessage(InetAddress ip, int port) {
        super(MsgType.LEAVE, ip, port);
    }

    public LeaveMessage() {
        super(MsgType.LEAVE);
    }

    @Override
    public DatagramPacket pack(InetAddress ip, int port) {
        byte [] buf = new byte[BUF_SIZE];
        buf[0] = (byte) type.ordinal();
        return new DatagramPacket(buf, buf.length, ip, port);
    }

    @Override
    public void handle(Node handler){
        handler.handleLeaveMessage(this);
    }
}
