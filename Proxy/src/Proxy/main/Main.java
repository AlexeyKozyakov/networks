package Proxy.main;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            printUsage();
            System.exit(0);
        }
        InetSocketAddress address = new InetSocketAddress("localhost", Integer.parseInt(args[0]));
        try {
            SocksProxy proxy = new SocksProxy(address);
            proxy.listen();
        } catch (IOException e) {
            System.err.println("Proxy finished with error");
            e.printStackTrace();
        }

    }

    private static void printUsage() {
        System.out.println("Usage: java Main port");
    }
}
