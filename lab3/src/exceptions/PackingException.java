package exceptions;

import java.io.IOException;

public class PackingException extends IOException {
    public PackingException(String message) { super(message); }
}
