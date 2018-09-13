import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Host {

    private Map<String, Boolean> apps = new ConcurrentHashMap<>();
    private String myPID = String.valueOf("/PID:" + getPID());
    private MulticastSocket mySocket;
    private InetAddress myGroup;
    private int delay = 200;
    private boolean running = true;

    public Host(InetAddress multicastAdress) throws IOException {
        myGroup = multicastAdress;
        mySocket = new MulticastSocket(12321);
        mySocket.joinGroup(myGroup);
    }

    public void start() {
        initPublisher();
        initListener();
        new Thread(() -> {
            while (running) {
                int size = apps.size();
                apps.values().removeIf(value -> !value);
                apps.replaceAll((id, live) -> false);
                if (apps.size() < size) {
                    printApps();
                }
                try {
                    if (running) {
                        Thread.sleep(delay * 2);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initListener() {
        new Thread(() -> {
            byte [] buf = new byte[256];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    mySocket.receive(packet);
                } catch (SocketException e) {
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String recvID = packet.getAddress() +  (new String(packet.getData(), 0, packet.getLength()));
                int size = apps.size();
                apps.put(recvID, true);
                if (apps.size() > size) {
                    printApps();
                }
            }
        }).start();
    }

    private void initPublisher() {
        new Thread(() -> {
            while (running) {
                byte[] buf = myPID.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, myGroup, mySocket.getLocalPort());
                try {
                    mySocket.send(packet);
                } catch (SocketException e) {
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (running) {
                        Thread.sleep(delay);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stop() throws IOException {
        running = false;
        mySocket.leaveGroup(myGroup);
        mySocket.close();
        System.out.println("Finalized");
    }

    private void printApps() {
        System.out.println(apps.size() + ((apps.size() == 1) ? " host is running:" : " hosts are running:"));
        apps.keySet().forEach(System.out::println);
        System.out.println();
    }

    private long getPID() {
        String processName =
                java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return Long.parseLong(processName.split("@")[0]);
    }

}