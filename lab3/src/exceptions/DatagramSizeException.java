package exceptions;

import java.io.IOException;

public class DatagramSizeException extends IOException {
    public DatagramSizeException(String message) { super(message); }
}
