import sun.misc.Signal;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private final int BUF_SIZE = 1024;

    private boolean running = true;

    private ServerSocket serverSocket;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Error while creating socket");
            e.printStackTrace();
        }
        System.out.println("Starting server...");
        while (running) {
            try {
                new ClientHandler(serverSocket.accept()).start();
            } catch (IOException e) {
                System.out.println("Finalized");
            }
        }
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error while closing socket");
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {

        private long size;
        private long received = 0;
        private long lastReceived = 0;
        private Thread speed;

        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            clientSocket = socket;
        }

        private void startSpeedCount() {
            speed = new Thread(() -> {
                long time = 3;
                try {
                    while (received != size && running) {
                        Thread.sleep(3000);
                        System.out.println("\nCurrent speed: " + (received - lastReceived) / (3.0 * 1024 * 1024) + " MB/S\n" +
                                "Total speed: " + received / (time * 1024.0 * 1024) + " MB/S\n" +
                                "Received: " + received / (1024.0 * 1024) + " MB");
                        time += 3;
                        lastReceived = received;
                        if (isInterrupted()) {
                            if (received != size)
                                System.err.println("Speed thread finished before file was uploaded");
                        }
                    }
                    System.out.println("\nfinished");
                } catch (InterruptedException e) {
                    System.err.println("Speed thread finished before file was uploaded");
                }
            });
            speed.start();
        }

        @Override
        public void run() {
            try {
                byte [] buf = new byte[BUF_SIZE];
                DataInputStream socketInput = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream socketOutput = new DataOutputStream(clientSocket.getOutputStream());
                String name = socketInput.readUTF();
                size = socketInput.readLong();
                startSpeedCount();
                System.out.println("NAME: " + name + "\nSIZE: " + size / (1024.0 * 1024) + " MB");
                String outPath;
                if (name.contains("/")) {
                    outPath = "upload" + name.substring(name.lastIndexOf('/'));
                } else {
                    outPath = "upload/" + name;
                }
                File newFile = new File(outPath);
                if (newFile.exists()) {
                    System.err.println("File already exists");
                    socketOutput.writeUTF("FILE_FAILED");
                    throw new IOException();
                } else {
                    socketOutput.writeUTF("FILE_OK");
                }
                File newDir = newFile.getParentFile();
                if (!newDir.exists()) {
                    newDir.mkdirs();
                }
                newFile.createNewFile();
                OutputStream fileOutput = new FileOutputStream(outPath);
                int len;
                while (received != size && running) {
                    len = socketInput.read(buf);
                    if (len == -1) {
                        running = false;
                        break;
                    }
                    received += len;
                    fileOutput.write(buf, 0, len);
                }

                fileOutput.close();
                socketOutput.writeUTF("OK");
                socketInput.close();
                socketOutput.close();
                clientSocket.close();
            } catch (IOException e) {
                speed.interrupt();
                running = false;
                System.err.println("Something went wrong, closing connection");
                try {
                    clientSocket.close();
                } catch (IOException e1) {
                    System.err.println("Error while closing client socket");
                    System.err.println(e1);
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Error in args\n");
            return;
        }
        Server server = new Server();
        new Thread(() -> {
            server.start(Integer.valueOf(args[0]));
        }).start();
        Signal.handle(new Signal("INT"), signal -> {
            System.out.println("Terminating server...");
            server.stop();
        });
    }

}
