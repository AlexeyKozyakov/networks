package client;

import msg.*;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Node {
    public static final long DELAY = 10000;
    public static final long CHECK_TIME = 500;
    public static final long CACHE_SIZE = 10000;
    public static final String END_MESSAGE = "::send";
    public static final String QUIT_MESSAGE = "::exit";

    private Scanner scanner = new Scanner(System.in);
    private DatagramSocket socket;
    private Map<InetSocketAddress, Long> neighbours = new ConcurrentHashMap<>();
    private Sender sender;
    private Receiver receiver;

    private String name;
    private int lose;

    private Map<UUID, Message> latestMessages = Collections.synchronizedMap(new LinkedHashMap<UUID, Message>() {
        @Override
        protected boolean removeEldestEntry(final Map.Entry eldest) {
            return size() > CACHE_SIZE;
        }
    });

    private Set<UUID> latestTextMessages = Collections.newSetFromMap(new LinkedHashMap<UUID, Boolean>(){
        @Override
        protected boolean removeEldestEntry(final Map.Entry eldest) {
            return size() > CACHE_SIZE;
        }
    });

    public Node(String name, int lose, int port, InetAddress parentIP, int parentPort) throws SocketException {
        this(name, lose, port);
        neighbours.put(new InetSocketAddress(parentIP, parentPort), System.currentTimeMillis());
    }

    public Node(String name, int lose, int port) throws SocketException {
        this.name = name;
        this.lose = lose;
        socket = new DatagramSocket(port);
    }

    public void start() throws IOException{
        sender = new Sender(this);
        receiver = new Receiver(this);
        receiver.start();
        updating.start();
        messenger.start();
        JoinMessage joinMessage = new JoinMessage(UUID.randomUUID());
        sender.sendMessageToNeignbours(joinMessage);
    }

    public void stop() throws IOException {
        LeaveMessage msg = new LeaveMessage();
        sender.sendMessageToNeignbours(msg);
        updating.interrupt();
        receiver.interrupt();
        messenger.interrupt();
    }

    private Thread messenger = new Thread(() -> {
        StringBuilder builder = new StringBuilder();
        builder.setLength(0);
        System.out.println("\nEnter message:");
        while (true) {
            System.out.print("\t");
            String str = scanner.nextLine();
            if (QUIT_MESSAGE.equals(str)) {
                try {
                    stop();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (!END_MESSAGE.equals(str)) {
                builder.append(str).append("\n");
            } else {
                TextMessage msg = new TextMessage(UUID.randomUUID(), name, builder.toString());
                try {
                    sender.sendMessageToNeignbours(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println(e.toString());
                }
                System.out.print("\nEnter message:\n");
            }
        }
    });

    private Thread updating = new Thread(() -> {
        while (true) {
            try {
                Thread.sleep(CHECK_TIME);
            } catch (InterruptedException e) {
                break;
            }
            if (Thread.interrupted()) {
                break;
            }
            PingMessage ping;
            ping = new PingMessage();
            try {
                sender.sendMessageToNeignbours(ping);
            } catch (IOException e) {
                if (Thread.interrupted()) {
                    break;
                } else {
                    System.err.println(e.toString());
                }
            }
            for (Message msg : latestMessages.values()) {
                if (!msg.isAcked() && msg.isSent()) {
                    try {
                        sender.resendToNotAcked(msg);
                    } catch (IOException e) {
                        if (Thread.interrupted()) {
                            return;
                        } else {
                            e.printStackTrace();
                            System.err.println(e.toString());
                        }
                    }
                }
            }
            neighbours.values().removeIf(lastResponse ->
                    (System.currentTimeMillis() - lastResponse) >= DELAY);
        }
    });

    public void handleLeaveMessage(LeaveMessage msg) {
        if (neighbours.containsKey(msg.getAddr())) {
            neighbours.keySet().remove(msg.getAddr());
        }
    }

    public void handlePingMessage(PingMessage msg) {
        if (neighbours.containsKey(msg.getAddr())) {
            neighbours.put(msg.getAddr(), System.currentTimeMillis());
        }
    }

    public void handleJoinMessage(JoinMessage msg) {
        if (!neighbours.containsKey(msg.getAddr())) {
            neighbours.put(msg.getAddr(), System.currentTimeMillis());
        }
        AckMessage ack = new AckMessage(msg.getJoinUUID());
        try {
            sender.sendMessage(ack, msg.getAddr());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.toString());
        }
    }

    public void handleAckMessage(AckMessage msg) {
        if (latestMessages.containsKey(msg.getAckUUID())) {
            latestMessages.get(msg.getAckUUID()).ack(msg.getAddr());
        }
    }

    public void handleTextMessage(TextMessage msg) {
        if (!latestTextMessages.contains(msg.getMsgUUID())) {
            showMessage(msg);
            latestTextMessages.add(msg.getMsgUUID());
        }
        try {
            sender.resendTextMessage(msg);
            AckMessage ack = new AckMessage(msg.getMsgUUID(), msg.getIp(), msg.getPort());
            sender.sendMessage(ack, ack.getAddr());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.toString());
        }
    }

    private void showMessage(TextMessage msg) {
        System.out.println("\nMessage from " + msg.getSenderName() + ":" +
                "\n\t" + String.join("\n\t", msg.getText().split("\n")));
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public Map<InetSocketAddress, Long> getNeighbours() {
        return neighbours;
    }

    public int getLose() {
        return lose;
    }

    public Map<UUID, Message> getLatestMessages() {
        return latestMessages;
    }
}
