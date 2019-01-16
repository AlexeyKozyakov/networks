package Proxy.Connections;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public abstract class Connection {

    private static final int BUFFER_SIZE = 8192;

    public static final byte IPv4_TYPE = 0x01;
    public static final byte DOMAIN_TYPE = 0x03;
    public static final byte IPv6_TYPE = 0x04;

    protected SelectionKey key;
    protected final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    Connection(SelectionKey key) {
        this.key = key;
    }

    public abstract void handleRead() throws IOException;
    public abstract void handleWrite() throws IOException;
    public abstract boolean finishConnection() throws IOException;
    public abstract void finishWork();
    public abstract void closeSocket() throws IOException;

    @Override
    public abstract String toString();

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public SelectionKey getKey() {
        return key;
    }

    public void setEvent(int event) {
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() | event);
        }
    }

    public void removeEvent(int event) {
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() & ~event);
        }
    }
}
