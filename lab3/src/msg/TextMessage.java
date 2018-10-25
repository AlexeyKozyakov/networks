package msg;

import client.Node;
import exceptions.PackingException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public class TextMessage extends Message {
    private UUID msgUUID;
    private String text;
    private String senderName;

    public TextMessage(UUID msgUUID, String senderName, String text, InetAddress ip, int port) {
        super(MsgType.TEXT, ip, port);
        this.msgUUID = msgUUID;
        this.text = text;
        this.senderName = senderName;
    }

    public TextMessage(UUID msgUUID, String senderName, String text) {
        super(MsgType.TEXT);
        this.msgUUID = msgUUID;
        this.text = text;
        this.senderName = senderName;
    }


    @Override
    public DatagramPacket pack(InetAddress ip, int port) throws PackingException {
        byte [] buf = new byte[BUF_SIZE];
        byte [] senderNameB = senderName.getBytes();
        byte [] textB = text.getBytes();
        if (TEXT_OFFSET + senderNameB.length + textB.length > BUF_SIZE) {
            throw new PackingException("Message too long");
        }
        buf[0] = (byte) type.ordinal();
        packUUIDtoBuf(buf, msgUUID);
        packLenToBuf(buf);
        System.arraycopy(senderNameB, 0, buf, TEXT_OFFSET, senderNameB.length);
        System.arraycopy(textB, 0, buf, TEXT_OFFSET + senderNameB.length, textB.length);
        return new DatagramPacket(buf, 0, buf.length, ip, port);
    }

    @Override
    public void handle(Node handler) {
        handler.handleTextMessage(this);
    }

    private void packLenToBuf(byte [] buf) {
        ByteBuffer bb = ByteBuffer.wrap(buf, LEN_OFFSET, LEN_SIZE);
        bb.putInt(senderName.getBytes().length);
        bb.putInt(text.getBytes().length);
    }

    public UUID getMsgUUID() {
        return msgUUID;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getText() {
        return text;
    }

}
