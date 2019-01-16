package Proxy.Connections;

import Proxy.main.Debug;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ServerConnection extends Connection {

    private Selector selector;
    private SocketChannel socketChannel;
    private ClientConnection client;
    private boolean isConnected = false;
    private boolean finishInput = false;
    private boolean finishOutput = false;

    public ServerConnection(Selector selector, ClientConnection client) throws IOException {
        super(null);
        this.selector = selector;
        this.client = client;
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
    }

    public void connectToServer(InetSocketAddress address) throws IOException {
        key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
        socketChannel.connect(address);
        Debug.print("Connecting to " + socketChannel.getRemoteAddress().toString() + " ");
    }

    @Override
    public void handleRead() throws IOException {
        if (finishInput) {
            removeEvent(SelectionKey.OP_READ);
            return;
        }
        int len;
        try {
            len = socketChannel.read(buffer);
        } catch (IOException e) {
            finishWork();
            client.finishWork();
            return;
        }
        Debug.bytesPrintln(len + " bytes form server");
        if (len <= 0) {
            removeEvent(SelectionKey.OP_READ);
            socketChannel.shutdownInput();
            finishInput = true;
            return;
        }
        client.setEvent(SelectionKey.OP_WRITE);
        if (buffer.position() >= buffer.capacity() - 1) {
            removeEvent(SelectionKey.OP_READ);
        }
    }

    @Override
    public void handleWrite() throws IOException {
        if (finishOutput || client.getBuffer().position() == 0) {
            removeEvent(SelectionKey.OP_WRITE);
            return;
        }
        client.getBuffer().flip();
        int len;
        try {
            len = socketChannel.write(client.getBuffer());
        } catch (IOException e) {
            finishWork();
            client.finishWork();
            return;
        }
        client.getBuffer().compact();
        Debug.bytesPrintln(len + " bytes to server");
        if (len > 0) {
            client.setEvent(SelectionKey.OP_READ);
        }
        if (client.getBuffer().position() <= 0) {
            removeEvent(SelectionKey.OP_WRITE);
            if (client.isFinishInput()) {
                socketChannel.shutdownOutput();
                finishOutput = true;
            }
        }
    }

    @Override
    public void finishWork() {
        finishInput = true;
        finishOutput = true;
        buffer.clear();
        if (key != null) {
            key.cancel();
        }
    }

    @Override
    public boolean finishConnection() throws IOException {
        if (finishInput
                && buffer.position() == 0
                && client.isFinishInput()
                && client.getBuffer().position() == 0) {
            closeSocket();
            return true;
        }
        return false;
    }

    @Override
    public void closeSocket() throws IOException {
        if (isConnected) {
            socketChannel.close();
        }
        if (key != null) {
            key.cancel();
        }
    }

    @Override
    public String toString() {
        return "Client i-" + finishInput + " o-" + finishOutput + " b-" + buffer.position();
    }

    public boolean isFinishInput() {
        return finishInput;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public ClientConnection getClient() {
        return client;
    }

    public void setConnected() throws IOException {
        isConnected = true;
        Debug.println(socketChannel.getRemoteAddress().toString());
    }

    public boolean isConnected() {
        return isConnected;
    }
}
