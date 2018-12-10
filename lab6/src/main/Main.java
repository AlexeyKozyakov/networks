package main;

import forwarder.Forwarder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Wrong program args");
            printUsage();
        } else {
            try {
                int leftPort = Integer.valueOf(args[0]);
                InetAddress rightHost = InetAddress.getByName(args[1]);
                int rightPort = Integer.valueOf(args[2]);
                Forwarder forwarder = new Forwarder(leftPort, rightHost, rightPort);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        forwarder.stop();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }));
                forwarder.start();
            } catch (NumberFormatException e) {
                System.err.println("Bad port value");
                printUsage();
            } catch (UnknownHostException e) {
                System.err.println("Unknown host");
                printUsage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void printUsage() {
        System.err.println("usage: " + Main.class.getName() + " <lport> <rhost> <rport>");
    }
}
