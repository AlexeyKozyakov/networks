package forwarder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Forwarder {

    private final int BUF_SIZE = 1024;
    private ServerSocketChannel serverSocket;
    private InetAddress rightHost;
    private Selector selector;
    private int leftPort, rightPort;
    private Connections connections = new Connections();
    private boolean running = true;

    public Forwarder(int leftPort, InetAddress rightHost, int rightPort){
        this.leftPort = leftPort;
        this.rightHost = rightHost;
        this.rightPort = rightPort;
    }

    public void start() throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(leftPort));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Forwarder started");
        listening();
    }

    public void stop() throws IOException {
        running = false;
        selector.close();
        connections.closeAll();
        System.out.println("Forwarder finalized");
    }

    private void listening() throws IOException {
        while (running) {
            if (selector.select() > 0) {
                try {
                    Set<SelectionKey> selected = selector.selectedKeys();
                    for (Iterator<SelectionKey> iterator = selected.iterator(); iterator.hasNext(); ) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isValid() && key.isAcceptable()) {
                            acceptSocket();
                        }
                        if (key.isValid() && key.isConnectable()) {
                            connectSocket(key);
                        }
                        if (key.isValid() && key.isReadable()) {
                            readSocket(key);
                        }
                        if (key.isValid() && key.isWritable()) {
                            writeSocket(key);
                        }
                    }
                } catch (ClosedSelectorException e) {
                    //selector closed, running == false
                }
            }
            connections.closeFinalized();
        }
    }

    private void acceptSocket() throws IOException{
        SocketChannel leftSocket = serverSocket.accept();
        leftSocket.configureBlocking(false);
        leftSocket.register(selector, SelectionKey.OP_READ);
        try {
            SocketChannel rightSocket = SocketChannel.open();
            rightSocket.configureBlocking(false);
            rightSocket.register(selector, SelectionKey.OP_CONNECT);
            rightSocket.connect(new InetSocketAddress(rightHost, rightPort));
            connections.addConnection(new Connection(leftSocket, rightSocket,
                    ByteBuffer.allocate(BUF_SIZE), ByteBuffer.allocate(BUF_SIZE), selector));
        } catch (IOException e) {
            leftSocket.close();
        }
    }

    private void connectSocket(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        try {
            if (socket.finishConnect()) {
                socket.keyFor(selector).interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            System.err.println(e);
            connections.getByRightSocket(socket).close();
        }
    }

    private void readSocket(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        Connection connection;
        if (connections.isLeft(socket)) {
            connection = connections.getByLeftSocket(socket);
            try {
                connection.readLeft();
            } catch (IOException e) {
                System.err.println(e);
                connection.getRightSocket().close();
            }
        } else {
            connection = connections.getByRightSocket(socket);
            try {
                connection.readRight();
            } catch (IOException e) {
                System.err.println(e);
                connection.getLeftSocket().close();
            }
        }
    }

    private void writeSocket(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        Connection connection;
        if (connections.isLeft(socket)) {
            connection = connections.getByLeftSocket(socket);
            try {
                connection.writeToLeft();
            } catch (IOException e) {
                System.err.println(e);
                connection.getRightSocket().close();
            }
        } else {
            connection = connections.getByRightSocket(socket);
            try {
                connection.writeToRight();
            } catch (IOException e) {
                System.err.println(e);
                connection.getLeftSocket().close();
            }
        }
    }

}
