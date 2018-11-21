package main;

import server.Server;
import sun.misc.Signal;

import java.io.IOException;
import java.net.SocketException;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            showHint();
            return;
        }
        try {
            int port = Integer.valueOf(args[0]);
            Server server = new Server(port);
            server.start();
            Signal.handle(new Signal("INT"), signal -> {
                server.stop();
            });
        } catch (SocketException e) {
            System.err.println("Choose another port");
            System.err.println(e.getMessage());
        }
    }

    private static void showHint() {
        System.out.println("Error in args\n" +
                "Usage: server PORT");
    }
}
