package exceptions;

import java.io.IOException;

public class DatagramFormatException extends IOException {
    public DatagramFormatException(String message) { super(message); }
}
