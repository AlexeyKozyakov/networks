package Proxy.main;

import Proxy.Connections.ClientConnection;
import Proxy.Connections.Connection;
import Proxy.Connections.DNSConnection;
import Proxy.Connections.ServerConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SocksProxy {

    private boolean run = true;

    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final Map<SelectableChannel, Connection> connections = new HashMap<>();
    private final DNSConnection dnsConnection;

    SocksProxy(InetSocketAddress listenAddress) throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(listenAddress);
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        dnsConnection = new DNSConnection(selector);
        connections.put(dnsConnection.getDatagramChannel(), dnsConnection);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                run = false;
                selector.wakeup();
                synchronized (this) {
                    for (Map.Entry<SelectableChannel, Connection> entry : connections.entrySet()) {
                        entry.getValue().closeSocket();
                    }
                    connections.clear();
                    dnsConnection.closeSocket();
                    serverChannel.close();
                }
            } catch (IOException ignore) {

            }
        }));

        Debug.print("Proxy successfully started on port " + listenAddress.getPort());
    }

    public void listen() throws IOException {
        Debug.println(" Listening incoming connections...");

        synchronized (this) {
            while (run) {
                int count = selector.select();
                if (count == 0) {
                    continue;
                }
                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key : keys) {
                    if (!key.isValid()) {
                        ((Connection) key.attachment()).finishWork();
                        continue;
                    }
                    if (key.isAcceptable()) {
                        Debug.print("New accept... ");
                        handleAccept();
                        continue;
                    }
                    if (key.isConnectable()) {
                        Debug.print("Connect");
                        handleConnect(key);
                        continue;
                    }
                    if (key.isReadable()) {
                        Debug.bytesPrint("Read ");
                        handleRead(key);
                        if (!key.isValid()) {
                            continue;
                        }
                    }
                    if (key.isWritable()) {
                        Debug.bytesPrint("Write ");
                        handleWrite(key);
                    }
                }
                connections.entrySet().removeIf(e -> {
                    try {
                        return e.getValue().finishConnection();
                    } catch (IOException ignore) {
                        return false;
                    }
                });
                Debug.openPrintln("Open sockets: " + (connections.size() - 1));
                for (Map.Entry<SelectableChannel, Connection> entry : connections.entrySet()) {
                    if (!(entry.getValue() instanceof DNSConnection))  {
                        Debug.openInfoPrint(entry.getValue().toString() + ";   ");
                    }
                }
                if (connections.size() != 0) {
                    Debug.openInfoPrintln("");
                }
                keys.clear();
            }
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel clientSocketChannel = serverChannel.accept();
        if (clientSocketChannel == null) {
            return;
        }
        clientSocketChannel.configureBlocking(false);
        SelectionKey key = clientSocketChannel.register(selector, SelectionKey.OP_READ);

        ClientConnection clientConnection = new ClientConnection(key, clientSocketChannel);
        key.attach(clientConnection);
        connections.put(clientSocketChannel, clientConnection);
        Debug.println("Accepted");
    }

    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            if (channel.isConnectionPending() && channel.finishConnect()) {
                channel.keyFor(selector).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                /*Debug.print("ed to ");
                ((ServerConnection) connections.get(channel)).setConnected();
                ((ServerConnection) connections.get(channel)).getClient().sendGoodAnswer();*/
                if (key.attachment() instanceof ServerConnection) {
                    Debug.print("ed to ");
                    ((ServerConnection) key.attachment()).setConnected();
                    ((ServerConnection) key.attachment()).getClient().sendGoodAnswer();
                } else {
                    Debug.println("ed " + key.attachment().getClass().toString());
                }

                return;
            }
            Debug.println("ing");
        } catch (IOException e) {
            ((ServerConnection) key.attachment()).getClient().sendBadAnswer((byte) 0x04);
            ((ServerConnection) key.attachment()).finishWork();
            Debug.println("ing failure");
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        Connection connection = (Connection) key.attachment();
        connection.handleRead();

        if (connection instanceof ClientConnection) {
            ClientConnection clientConnection = (ClientConnection) connection;
            if (clientConnection.isFinishRequest() && clientConnection.getServer() == null && !clientConnection.isFinishInput()) {
                ServerConnection serverConnection = new ServerConnection(selector, clientConnection);
                clientConnection.setServer(serverConnection);
                connections.put(serverConnection.getSocketChannel(), serverConnection);

                if (clientConnection.getAddressType() == Connection.IPv4_TYPE) {
                    //Connect by IPv4 address
                    byte[] addr = clientConnection.getAddress();
                    String ip = (addr[0] & 0xFF) + "." + (addr[1] & 0xFF) + "." + (addr[2] & 0xFF) + "." + (addr[3] & 0xFF);
                    InetSocketAddress serverAddress = new InetSocketAddress(ip, clientConnection.getServerPort());
                    clientConnection.getServer().connectToServer(serverAddress);
                    serverConnection.getKey().attach(serverConnection);
                    Debug.println("by IPv4");
                } else {
                    //Connect by domain name with dns resolve
                    Debug.println("Resolve domain " + new String(clientConnection.getAddress()) + ":" + clientConnection.getServerPort() + " by DNS");
                    dnsConnection.startResolve(clientConnection.getServer(),
                            new String(clientConnection.getAddress()) + ".",
                            clientConnection.getServerPort());
                }
            }
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        Connection connection = (Connection) key.attachment();
        connection.handleWrite();
    }
}
