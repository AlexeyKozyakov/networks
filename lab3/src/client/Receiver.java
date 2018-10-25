package client;

import msg.Message;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.Random;


public class Receiver extends Thread{
    private Node owner;
    private DatagramSocket socket;
    private int lose;
    private Random random = new Random();

    public Receiver(Node owner) {
        this.owner = owner;
        socket = owner.getSocket();
        lose = owner.getLose();
    }

    @Override
    public void run() {
        while (true) {
            if(isInterrupted()){
                break;
            }
            try {
                Message msg = Message.receive(socket);
                if (random.nextInt(101) + 1 >= lose) {
                    msg.handle(owner);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println(e.toString());
            }
        }
    }
}
