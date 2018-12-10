package forwarder;

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

    public void remove(Connection connection) {
        leftConnections.remove(connection);
        rightConnections.remove(connection);
    }

}
