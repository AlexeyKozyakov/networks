import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client {

    private final int BUF_SIZE = 1024;

    private Socket socket;

    public void start(String filename, String ip, int port) {
        try {
            socket = new Socket(InetAddress.getByName(ip), port);
        } catch (IOException e) {
            System.err.println("Error while creating socket");
            return;
        }
        try {
            uploadFile(filename);
        } catch (IOException e) {
            System.err.println("Error while uploading file");
        }
    }

    private void uploadFile(String fileName) throws IOException{
        byte [] buf = new byte[BUF_SIZE];
        DataInputStream socketInput = new DataInputStream(socket.getInputStream());
        DataOutputStream socketOutput = new DataOutputStream(socket.getOutputStream());
        File file = new File(fileName);
        InputStream fileInput = new FileInputStream(file);
        socketOutput.writeUTF(fileName);
        socketOutput.writeLong(file.length());
        if (!"FILE_OK".equals(socketInput.readUTF())) {
            System.err.println("File already exists");
            socketInput.close();
            socketOutput.close();
            socket.close();
        }
        int len;
        while ((len = fileInput.read(buf)) != -1) {
            socketOutput.write(buf, 0, len);
        }
        fileInput.close();
        if ("OK".equals(socketInput.readUTF())) {
            System.out.println("Uploading completed successfully");
        }
        socketInput.close();
        socketOutput.close();
        socket.close();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Error in args\n" +
                    "Usage: java Client filename IP port");
            return;
        }
        Client client = new Client();
        client.start(args[0], args[1], Integer.valueOf(args[2]));
    }

}
