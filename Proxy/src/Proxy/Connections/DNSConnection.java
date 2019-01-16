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
    private final Map<Name, ServerConnection> resolving = new HashMap<>();
    private final Map<Name, Integer> ports = new HashMap<>();

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

        resolving.put(name, serverConnection);
        ports.put(name, port);
    }

    @Override
    public void handleRead() throws IOException {
        int len = datagramChannel.read(buffer);
        Debug.bytesPrintln("DNS resolver receive " + len + " bytes");
        Message message = new Message(buffer.array());
        Record[] records = message.getSectionArray(Section.ANSWER);

        for (Record record : records) {
            if (record.getType() == Type.A) {
                Name name = null;
                ServerConnection server = null;
                int port = 0;
                for (Map.Entry<Name, ServerConnection> entry : resolving.entrySet()) {
                    for (int i = 0; i < entry.getKey().labels(); ++i) {
                        if (record.getName().toString().contains(entry.getKey().getLabelString(i))) {
                            name = entry.getKey();
                            server = entry.getValue();
                            break;
                        }
                    }
                    if (name != null) {
                        break;
                    }
                }
                if (name == null) {
                    continue;
                }
                port = ports.get(name);
                Debug.println(name.toString().substring(0, name.toString().length() - 1) + ":" + port + " resolved");
                InetSocketAddress address = new InetSocketAddress(((ARecord) record).getAddress(), port);
                server.connectToServer(address);
                server.getKey().attach(server);
                Debug.println("by domain name");

                resolving.remove(name);
                ports.remove(name);
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
