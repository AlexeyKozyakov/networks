package forwarder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Forwarder {

    private final int BUF_SIZE = 1024;
    private ServerSocketChannel serverSocket;
    private InetAddress rightHost;
    private Selector selector;
    private int leftPort, rightPort;
    private Connections connections = new Connections();

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
        listening();
    }

    private void listening() throws IOException {
        while (true) {
            if (selector.select() > 0) {
                Set<SelectionKey> selected = selector.selectedKeys();
                for (Iterator<SelectionKey> iterator = selected.iterator(); iterator.hasNext(); ) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid() && key.isAcceptable()) {
                        acceptSocket(key);
                    }
                    if (key.isValid() && key.isReadable()) {
                        readSocket(key);
                    }
                    if (key.isValid() && key.isWritable()) {
                        writeSocket(key);
                    }
                }
            }
        }
    }

    private void acceptSocket(SelectionKey key) throws IOException{
        SocketChannel leftSocket = serverSocket.accept();
        leftSocket.configureBlocking(false);
        leftSocket.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        SocketChannel rightSocket = SocketChannel.open();
        try {
            rightSocket.connect(new InetSocketAddress(rightHost, rightPort));
            rightSocket.configureBlocking(false);
            rightSocket.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            connections.addConnection(new Connection(leftSocket, rightSocket,
                    ByteBuffer.allocate(BUF_SIZE), ByteBuffer.allocate(BUF_SIZE)));
        } catch (IOException e) {
            System.err.println(e);
            leftSocket.close();
        }
    }

    private void readSocket(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        Connection connection;
        if (connections.isLeft(socket)) {
            connection = connections.getByLeftSocket(socket);
            try {
                if (connection.checkLeftBuf() && connection.readLeft() <= 0) {
                    closeConnection(connection);
                }
            } catch (IOException e) {
                System.err.println(e);
                connection.getRightSocket().close();
            }
        } else {
            connection = connections.getByRightSocket(socket);
            try {
                if (connection.checkRightBuf() && connection.readRight() <= 0) {
                    closeConnection(connection);
                }
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

    private void closeConnection(Connection connection) throws IOException {
        connection.close();
        connections.remove(connection);
    }

}
