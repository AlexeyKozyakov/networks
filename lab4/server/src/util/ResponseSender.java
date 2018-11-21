package util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

public class ResponseSender {

    public static void sendResponse(HttpExchange exchange, int code, String headerName, String headerVal, String body) throws IOException {
        if (headerName != null && headerVal != null) {
            exchange.getResponseHeaders().add(headerName, headerVal);
        }
        if (body != null) {
            byte[] bytes = body.getBytes();
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } else {
            exchange.sendResponseHeaders(code, -1);
        }
    }

    public static void sendResponse(HttpExchange exchange, int code) throws IOException {
        sendResponse(exchange, code, null, null, null);
    }

    public static void sendResponse(HttpExchange exchange, int code, String headerName, String headerVal) throws IOException {
        sendResponse(exchange, code, headerName, headerVal, null);
    }

    public static void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        sendResponse(exchange, code, null, null, body);
    }
}
