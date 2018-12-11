package forwarder;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class Connections {
    private Map<SocketChannel, Connection> leftConnections = new HashMap<>();
    private Map<SocketChannel, Connection> rightConnections = new HashMap<>();

    public void addConnection(Connection connection) {
        leftConnections.put(connection.getLeftSocket(), connection);
        rightConnections.put(connection.getRightSocket(), connection);
    }

    public Connection getByLeftSocket(SocketChannel leftSocket) {
        return leftConnections.get(leftSocket);
    }

    public Connection getByRightSocket(SocketChannel rightSocket) {
        return rightConnections.get(rightSocket);
    }

    public boolean isLeft(SocketChannel socket) {
        return leftConnections.containsKey(socket);
    }

    public void closeFinalized() throws IOException {
        for (Connection connection : leftConnections.values()) {
            if (connection.finalized()) {
                connection.close();
            }
        }
        leftConnections.values().removeIf(Connection::finalized);
        rightConnections.values().removeIf(Connection::finalized);
    }

    public void closeAll() throws IOException {
        for (Connection connection : leftConnections.values()) {
            if (connection.finalized()) {
                connection.close();
            }
        }
        leftConnections.clear();
        rightConnections.clear();
    }

}
