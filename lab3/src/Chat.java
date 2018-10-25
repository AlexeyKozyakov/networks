import client.Node;
import sun.misc.Signal;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Chat {

    private static void printUsage() {
        System.err.println("Usage: java Chat name lose_percent port [parent_ip parent_port]");
    }

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 5) {
            System.out.println("Error in args");
            printUsage();
            return;
        }
        String name = args[0];
        int lose = Integer.valueOf(args[1]);
        int port = Integer.valueOf(args[2]);
        final Node myNode;
        try {
            if (args.length == 3) {
                myNode = new Node(name, lose, port);
            } else {
                InetAddress parentIp = InetAddress.getByName(args[3]);
                int parentPort = Integer.valueOf(args[4]);
                myNode = new Node(name, lose, port, parentIp, parentPort);
            }
            myNode.start();
            System.out.println("Commands:\n"+ Node.END_MESSAGE +" - send message\n" + Node.QUIT_MESSAGE +" - exit");
            System.out.println("Enter messages:");
        } catch (BindException e) {
            System.err.println("This port is already used!");
            printUsage();
        } catch (UnknownHostException e) {
            System.err.println("Wrong ip address!");
            printUsage();
        } catch (NumberFormatException ex) {
            System.err.println("Wrong port!");
            printUsage();
        } catch (SocketException e) {
            e.printStackTrace();
            System.err.println("Error while creating socket");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error, cannot join to chat");
        }
    }
}
