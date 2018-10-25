
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Usage: java Main IPv4/IPv6_address");
            return;
        }

        try {
            InetAddress groupAddress = InetAddress.getByName(args[0]);
            if (!groupAddress.isMulticastAddress()) {
                System.out.println("Not multicast address:\nIPv4: should be between 224.0.0.0 and 239.255.255.255\n" +
                        "IPv6: should start from FF");
                return;
            }
            Host host = new Host(groupAddress);
            host.start();

        } catch (UnknownHostException e) {
            System.out.println("Invalid address");
            System.out.println("Use address format:\nIPv4: x.x.x.x\nIPv6: x:x:x:x:x:x:x:x");
        }
    }

}
