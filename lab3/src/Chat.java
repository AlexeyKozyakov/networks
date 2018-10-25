import client.Node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public class Chat {

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 5) {
            System.err.println("Error in args\n" +
                    "Usage: java Chat name lose_percent port [parent_ip parent_port]");
            return;
        }
        String name = args[0];
        int lose = Integer.valueOf(args[1]);
        int port = Integer.valueOf(args[2]);
        Node myNode = null;
        try {
            if (args.length == 3) {
                myNode = new Node(name, lose, port);
            } else {
                InetAddress parentIp = InetAddress.getByName(args[3]);
                int parentPort = Integer.valueOf(args[4]);
                myNode = new Node(name, lose, port, parentIp, parentPort);
            }
            myNode.start();
        } catch (SocketException e) {
            e.printStackTrace();
            System.err.println("Error while creating socket");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error, cannot join to chat");
        }
    }
}
