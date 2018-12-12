package forwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Connection {
    private final SocketChannel leftSocket;
    private final SocketChannel rightSocket;
    private final ByteBuffer leftBuffer;
    private final ByteBuffer rightBuffer;
    private final Selector selector;
    private boolean leftClosed = false, rightClosed = false;

    public Connection(SocketChannel leftSocket, SocketChannel rightSocket,
                      ByteBuffer leftBuffer, ByteBuffer rightBuffer, Selector selector) {
        this.leftSocket = leftSocket;
        this.rightSocket = rightSocket;
        this.leftBuffer = leftBuffer;
        this.rightBuffer = rightBuffer;
        this.selector = selector;
    }

    public SocketChannel getLeftSocket() {
        return leftSocket;
    }

    public SocketChannel getRightSocket() {
        return rightSocket;
    }

    public void readLeft() throws IOException {
        readFrom(leftSocket, rightSocket, leftBuffer, true);
    }

    public void readRight() throws IOException {
        readFrom(rightSocket, leftSocket, rightBuffer, false);
    }

    private void readFrom(SocketChannel socket, SocketChannel another, ByteBuffer bb, boolean left) throws IOException {
        int received = socket.read(bb);
        if (received <= 0) {
            if (left) {
                leftClosed = true;
            } else {
                rightClosed = true;
            }
            removeOp(socket, SelectionKey.OP_READ);
        }
        if (received > 0 && another.isConnected())
            addOp(another, SelectionKey.OP_WRITE);
        if (bb.position() >= bb.limit()) {
            removeOp(socket, SelectionKey.OP_READ);
        }
    }

    public void writeToRight() throws IOException {
        writeTo(rightSocket, leftSocket, leftBuffer);
    }

    public void writeToLeft() throws IOException {
        writeTo(leftSocket, rightSocket, rightBuffer);
    }

    private void writeTo(SocketChannel socket, SocketChannel another, ByteBuffer bb) throws IOException {
        bb.flip();
        if (socket.write(bb) > 0 && another.isConnected()) {
            addOp(another, SelectionKey.OP_READ);
        }
        bb.compact();
        if (bb.position() <= 0) {
            removeOp(socket, SelectionKey.OP_WRITE);
        }
    }

    public void close() throws IOException {
        leftSocket.close();
        rightSocket.close();
        leftSocket.keyFor(selector).cancel();
    }

    private void addOp(SocketChannel socket, int op) {
        socket.keyFor(selector).interestOps(socket.keyFor(selector).interestOps() | op);
    }

    private void removeOp(SocketChannel socket, int op) {
        socket.keyFor(selector).interestOps(socket.keyFor(selector).interestOps() & ~op);
    }

    public boolean finalized() {
        return leftClosed && rightClosed && leftBuffer.position() == 0 && rightBuffer.position() == 0;
    }

}
