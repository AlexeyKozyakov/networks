package forwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Connection {
    private SocketChannel leftSocket, rightSocket;
    private ByteBuffer leftBuffer, rightBuffer;

    public Connection(SocketChannel leftSocker, SocketChannel rightSocker, ByteBuffer leftBuffer, ByteBuffer rightBuffer) {
        this.leftSocket = leftSocker;
        this.rightSocket = rightSocker;
        this.leftBuffer = leftBuffer;
        this.rightBuffer = rightBuffer;
    }

    public SocketChannel getLeftSocket() {
        return leftSocket;
    }

    public SocketChannel getRightSocket() {
        return rightSocket;
    }

    public ByteBuffer getLeftBuffer() {
        return leftBuffer;
    }

    public ByteBuffer getRightBuffer() {
        return rightBuffer;
    }

    public long readLeft() throws IOException {
        return readFrom(leftSocket, leftBuffer);
    }

    public long readRight() throws IOException {
        return readFrom(rightSocket, rightBuffer);
    }

    private long readFrom(SocketChannel socket, ByteBuffer bb) throws IOException {
        if (bb.position() < bb.limit()) {
            return socket.read(bb);
        }
        return 0;
    }

    public long writeToRight() throws IOException {
        return writeTo(rightSocket, leftBuffer);
    }

    public long writeToLeft() throws IOException {
        return writeTo(leftSocket, rightBuffer);
    }

    private long writeTo(SocketChannel socket, ByteBuffer bb) throws IOException {
        if (bb.position() > 0) {
            bb.flip();
            int sent = socket.write(bb);
            bb.compact();
            return sent;
        }
        return 0;
    }

    public void close() throws IOException {
        leftSocket.close();
        rightSocket.close();
    }

    public boolean checkLeftBuf() {
        return checkBuf(leftBuffer);
    }

    public boolean checkRightBuf() {
        return checkBuf(rightBuffer);
    }

    private boolean checkBuf(ByteBuffer bb) {
        return bb.limit() != bb.position();

    }
}
