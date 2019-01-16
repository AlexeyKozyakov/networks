package Proxy.Connections;

import Proxy.main.Debug;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ClientConnection extends Connection {

    private static final byte[] goodGreeting = {0x05, 0x00};
    private static final byte[] badGreeting = {0x05, (byte) 0xff};

    private final SocketChannel socketChannel;
    private ServerConnection server = null;
    private boolean finishInput = false;
    private boolean finishOutput = false;

    private boolean finishRequest = false;
    private byte addressType = 0;
    private byte addressLen = 4;
    private byte[] address;
    private int serverPort;

    private enum STATE { GREETING, REQUEST, MESSAGE}
    private STATE state = STATE.GREETING;

    public ClientConnection(SelectionKey key, SocketChannel channel) {
        super(key);
        this.socketChannel = channel;
    }

    @Override
    public void handleRead() throws IOException {
        if (finishInput) {
            removeEvent(SelectionKey.OP_READ);
            return;
        }
        int len;
        try {
            len = socketChannel.read(buffer);
        } catch (IOException e) {
            finishWork();
            return;
        }
        Debug.bytesPrintln(len + " bytes form client");
        if (len <= 0) {
            removeEvent(SelectionKey.OP_READ);
            socketChannel.shutdownInput();
            finishInput = true;
            if (server == null || !finishRequest) {
                finishWork();
                return;
            }
            return;
        }
        switch (state) {
            case GREETING:
                handleGreeting();
                break;

            case REQUEST:
                handleRequest();
                break;

            case MESSAGE:
                if (server != null && server.isConnected()) {
                    server.setEvent(SelectionKey.OP_WRITE);
                }
                if (buffer.position() >= buffer.capacity() - 1) {
                    removeEvent(SelectionKey.OP_READ);
                }
                break;
        }
    }

    @Override
    public void handleWrite() throws IOException {
        if (server == null || finishOutput || server.getBuffer().position() == 0) {
            removeEvent(SelectionKey.OP_WRITE);
            return;
        }
        server.getBuffer().flip();
        int len;
        try {
            len = socketChannel.write(server.getBuffer());
        } catch (IOException e) {
            finishWork();
            return;
        }
        server.getBuffer().compact();
        Debug.bytesPrintln(len + " bytes to client");
        if (len > 0) {
            server.setEvent(SelectionKey.OP_READ);
        }
        if (server.getBuffer().position() <= 0) {
            removeEvent(SelectionKey.OP_WRITE);
            if (server.isFinishInput()) {
                socketChannel.shutdownOutput();
                finishOutput = true;
            }
        }
    }

    @Override
    public boolean finishConnection() throws IOException {
        if (server != null
                && finishInput
                && buffer.position() == 0
                && server.isFinishInput()
                && server.getBuffer().position() == 0
                || server == null && finishInput && finishOutput) {
            closeSocket();
            return true;
        }
        return false;
    }

    @Override
    public void finishWork() {
        finishInput = true;
        finishOutput = true;
        buffer.clear();
        key.cancel();
        if (server != null) {
            server.finishWork();
        }
    }

    @Override
    public void closeSocket() throws IOException {
        socketChannel.close();
        key.cancel();
    }

    @Override
    public String toString() {
        return "Client i-" + finishInput + " o-" + finishOutput + " b-" + buffer.position();
    }

    private void handleGreeting() throws IOException {
        byte[] array = buffer.array();
        //handle version
        if (array.length >= 1) {
            if (array[0] != 0x05) {
                finishWork();
                return;
            }
        } else {
            return;
        }
        if (array.length >= 2) {
            if (array[1] != 0x01) {
                sendAnswer(badGreeting);
                finishWork();
                return;
            }
        } else {
            return;
        }
        if (array.length >= 3) {
            if (array[2] != 0x00) {
                sendAnswer(badGreeting);
                finishWork();
                return;
            }
        }
        sendAnswer(goodGreeting);
        buffer.clear();
        state = STATE.REQUEST;
    }

    private void handleRequest() throws IOException {
        byte[] array = buffer.array();
        //handle version
        if (array.length >= 1) {
            if (array[0] != 0x05) {
                ignoreClientConnection((byte) 0x05);
                return;
            }
        } else {
            return;
        }
        //handle command
        if (array.length >= 2) {
            if (array[1] != 0x01) {
                ignoreClientConnection((byte) 0x07);
                return;
            }
        } else {
            return;
        }
        //handle address type
        if (array.length >= 4) {
            addressType = array[3];
            if (addressType == IPv6_TYPE) {
                ignoreClientConnection((byte) 0x08);
                return;
            }
        } else {
            return;
        }
        //handle address length
        boolean isDomain = false;
        if (array.length >= 5) {
            if (addressType == DOMAIN_TYPE) {
                addressLen = array[4];
                isDomain = true;
            }
            address = new byte[addressLen];
        } else {
            return;
        }
        //handle address
        if (array.length >= 4 + addressLen) {
            System.arraycopy(array, isDomain ? 5 : 4, address, 0, addressLen);
        } else {
            return;
        }
        //handle port
        if (array.length >= 6 + addressLen) {
            int pos = isDomain ? 5 + addressLen : 4 + addressLen;
            serverPort = ((array[pos] & 0xFF) << 8) | (array[pos + 1] & 0xFF);
        }
        finishRequest = true;
        state = STATE.MESSAGE;
        buffer.clear();
    }

    private byte[] createAnswer(byte code) {
        byte[] answer;
        if (addressType == IPv4_TYPE) {
            answer = new byte[10];
        } else {
            answer = new byte[7 + addressLen];
        }
        answer[0] = 0x05;
        answer[1] = code;
        answer[2] = 0x00;
        answer[3] = addressType;
        int pos = 4;
        if (addressType == DOMAIN_TYPE) {
            answer[4] = addressLen;
            pos = 5;
        }
        //Init ip and port by 0
        for (int i = pos; i < pos + addressLen + 2; ++i) {
            answer[i] = 0;
        }
        return answer;
    }

    private void sendAnswer(byte[] answer) throws IOException {
        socketChannel.write(ByteBuffer.wrap(answer));
    }

    public void sendGoodAnswer() throws IOException {
        byte[] answer = createAnswer((byte) 0x00);
        sendAnswer(answer);
    }

    public void sendBadAnswer(byte code) throws IOException {
        byte[] answer = createAnswer(code);
        sendAnswer(answer);
        key.cancel();
        finishInput = true;
        buffer.clear();
    }

    private void ignoreClientConnection(byte code) throws IOException {
        byte[] answer = createAnswer(code);
        sendAnswer(answer);
        finishWork();
        Debug.println("Connection was ignored with code " + code);
    }

    public boolean isFinishInput() {
        return finishInput;
    }

    public void setServer(ServerConnection server) {
        this.server = server;
    }

    public ServerConnection getServer() {
        return server;
    }

    public boolean isFinishRequest() {
        return finishRequest;
    }

    public byte getAddressType() {
        return addressType;
    }

    public byte[] getAddress() {
        return address;
    }

    public int getServerPort() {
        return serverPort;
    }
}
