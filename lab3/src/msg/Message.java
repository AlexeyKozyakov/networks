package msg;

import client.Node;
import exceptions.DatagramFormatException;
import exceptions.DatagramSizeException;
import exceptions.PackingException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Message {
    static final int TYPE_OFFSET = 0;
    static final int UUID_OFFSET = 1;
    static final int LEN_OFFSET = 17;
    static final int TEXT_OFFSET = 25;
    static final int UUID_SIZE = 16;
    static final int LEN_SIZE = 8;
    static final int BUF_SIZE = 2048;

    MsgType type;
    InetAddress ip;
    int port;

    private Map<InetSocketAddress, Boolean> acks = new ConcurrentHashMap<>();

    private int acked = 0;
    private boolean sent = false;

    public abstract DatagramPacket pack(InetAddress ip, int port) throws PackingException;

    public abstract void handle(Node handler) throws IOException;

    public void send(DatagramSocket socket, InetSocketAddress addr) throws IOException, PackingException {
        acks.put(addr, false);
        socket.send(pack(addr.getAddress(), addr.getPort()));
    }

    public static Message receive(DatagramSocket socket) throws IOException {
        byte [] buf = new byte[BUF_SIZE];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        return unpack(packet);
    }

    public static Message unpack(DatagramPacket packet) throws DatagramFormatException, DatagramSizeException {
        byte recvTypeByte = packet.getData()[TYPE_OFFSET];
        if (recvTypeByte > MsgType.values().length || recvTypeByte < 0) {
            throw new DatagramFormatException("Wrong message type");
        }
        MsgType recvType = MsgType.values()[recvTypeByte];
        Message message = null;
        switch (recvType) {
            case JOIN:
                message = unpackJoin(packet);
                break;
            case ACK:
                message = unpackAck(packet);
                break;
            case TEXT:
                message = unpackText(packet);
                break;
            case PING:
                message = unpackPing(packet);
                break;
            case LEAVE:
                message = unpackLeave(packet);
                break;
        }
        return message;
    }

    Message(MsgType type, InetAddress ip, int port) {
        this.type = type;
        this.ip = ip;
        this.port = port;
    }

    Message(MsgType type) {
        this.type = type;
    }

    private static JoinMessage unpackJoin(DatagramPacket packet) {
        UUID joinUUID = unpackUUID(packet);
        return new JoinMessage(joinUUID, packet.getAddress(), packet.getPort());
    }

    private static AckMessage unpackAck(DatagramPacket packet) {
        UUID ackUUID = unpackUUID(packet);
        return new AckMessage(ackUUID, packet.getAddress(), packet.getPort());
    }

    private static TextMessage unpackText(DatagramPacket packet) throws DatagramSizeException {
        UUID msgUUID = unpackUUID(packet);
        MsgLen msgLen = unpackLen(packet);
        if (TEXT_OFFSET + msgLen.senderLen + msgLen.textLen > BUF_SIZE) {
            throw new DatagramSizeException("Message too long");
        }
        String senderName = new String(packet.getData(), TEXT_OFFSET, msgLen.senderLen);
        String text = new String(packet.getData(), TEXT_OFFSET + msgLen.senderLen, msgLen.textLen);
        return new TextMessage(msgUUID,senderName, text, packet.getAddress(), packet.getPort());
    }

    private static PingMessage unpackPing(DatagramPacket packet) {
        return new PingMessage(packet.getAddress(), packet.getPort());
    }

    private static LeaveMessage unpackLeave(DatagramPacket packet) {
        return new LeaveMessage(packet.getAddress(), packet.getPort());
    }

    private static UUID unpackUUID(DatagramPacket packet) {
        ByteBuffer bb = ByteBuffer.wrap(packet.getData(), UUID_OFFSET, UUID_SIZE);
        long most = bb.getLong();
        long least = bb.getLong();
        return new UUID(most, least);
    }

    private static MsgLen unpackLen(DatagramPacket packet) {
        MsgLen msgLen = new MsgLen();
        ByteBuffer bb = ByteBuffer.wrap(packet.getData(), LEN_OFFSET, LEN_SIZE);
        msgLen.senderLen = bb.getInt();
        msgLen.textLen = bb.getInt();
        return msgLen;
    }

    static void packUUIDtoBuf(byte [] buf, UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(buf, 1, UUID_SIZE);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
    }

    static class MsgLen {
        int textLen;
        int senderLen;
    }

    public InetAddress getIp() {
        return ip;
    }


    public int getPort() {
        return port;
    }

    public InetSocketAddress getAddr() {
        return new InetSocketAddress(ip, port);
    }

    public boolean isAcked() {
        return acked == acks.size();
    }

    public boolean isSent() {
        return sent;
    }

    public void ack(InetSocketAddress addr) {
        if (acks.containsKey(addr) && !acks.get(addr)) {
            acks.put(addr, true);
            ++acked;
        }
    }

    public void markAsSent() {
        sent = true;
    }

    public Map<InetSocketAddress, Boolean> getAcks() {
        return acks;
    }
}

