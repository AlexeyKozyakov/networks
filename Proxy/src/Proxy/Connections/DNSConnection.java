package Proxy.Connections;

import Proxy.main.Debug;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

public class DNSConnection extends Connection{

    private final DatagramChannel datagramChannel;
    private final Map<Integer, ServerConnection> resolving = new HashMap<>();
    private final Map<Integer, Integer> ports = new HashMap<>();

    public DNSConnection(Selector selector) throws IOException {
        super(null);
        datagramChannel = DatagramChannel.open();
        datagramChannel.configureBlocking(false);
        key = datagramChannel.register(selector, SelectionKey.OP_READ);
        datagramChannel.connect(new InetSocketAddress("8.8.8.8", 53));
        key.attach(this);
    }

    public void startResolve(ServerConnection serverConnection, String domainName, int port) throws IOException {
        Name name = Name.fromString(domainName);
        Record record = Record.newRecord(name, Type.A, DClass.IN);
        Message message = Message.newQuery(record);
        byte[] request = message.toWire();

        datagramChannel.write(ByteBuffer.wrap(request));

        resolving.put(message.getHeader().getID(), serverConnection);
        ports.put(message.getHeader().getID(), port);
    }

    @Override
    public void handleRead() throws IOException {
        int len = datagramChannel.read(buffer);
        Debug.bytesPrintln("DNS resolver receive " + len + " bytes");
        Message message = new Message(buffer.array());
        Record[] records = message.getSectionArray(Section.ANSWER);

        ServerConnection server = resolving.get(message.getHeader().getID());
        int port = ports.get(message.getHeader().getID());

        for (Record record : records) {
            if (record.getType() == Type.A) {
                InetSocketAddress address = new InetSocketAddress(((ARecord) record).getAddress(), port);
                server.connectToServer(address);
                server.getKey().attach(server);
                Debug.println("by domain name");
                resolving.remove(message.getHeader().getID());
                ports.remove(message.getHeader().getID());
                break;
            }
        }
        if (resolving.isEmpty()) {
            buffer.clear();
        }
    }

    @Override
    public void handleWrite() throws IOException {
        removeEvent(SelectionKey.OP_WRITE);
    }

    @Override
    public boolean finishConnection() throws IOException {
        return false;
    }

    @Override
    public void finishWork() {

    }

    @Override
    public void closeSocket() throws IOException {
        datagramChannel.close();
    }

    @Override
    public String toString() {
        return "DNS";
    }

    public DatagramChannel getDatagramChannel() {
        return datagramChannel;
    }
}
