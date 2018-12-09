package server;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import exchange.*;
import query.MessagesQuery;
import util.JsonParser;
import util.ResponseSender;
import websocket.MessagesEndpoint;

import javax.websocket.DeploymentException;
import javax.websocket.Session;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private final static int BAD_REQUEST = 400, UNAUTHORIZED = 401, OK = 200, FORBIDDEN = 403, METHOD_NOT_ALLOWED = 405;
    private final static int NOT_FOUND = 404, MAX_MESSAGES = 100;
    private final static long OFFLINE_DELAY = 5000;
    private HttpServer server;
    private JsonParser jsonParser = new JsonParser();
    private Map<UUID, User> users = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<Integer, UUID> ids = Collections.synchronizedMap(new LinkedHashMap<>());
    private Set<String> names = ConcurrentHashMap.newKeySet();
    private List<Message> messages = new CopyOnWriteArrayList<>();
    private int maxId = -1;
    private org.glassfish.tyrus.server.Server webSocketServer;


    public Server(int port) throws IOException {
        server = HttpServer.create();
        server.bind(new InetSocketAddress(port), 0);
        MessagesEndpoint.setServer(this);
        webSocketServer = new org.glassfish.tyrus.server.Server(null, port + 1,
                "/ws", null, MessagesEndpoint.class);
        createHandlers();
    }

    public void start() throws DeploymentException {
        server.start();
        webSocketServer.start();
        updating.start();
        System.out.println("Server is started");
    }

    public void stop() {
        server.stop(1);
        webSocketServer.stop();
        updating.interrupt();
        System.out.println("Server is stopped");
    }

    private Thread updating = new Thread(() -> {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(OFFLINE_DELAY);
                users.forEach(((token, user) -> {
                    if (System.currentTimeMillis() - user.getLastRequestTime() > OFFLINE_DELAY) {
                        user.setOnline(null);
                    }
                }));
            } catch (InterruptedException e) {
                break;
            }
        }

    });

    private void createHandlers() {
        server.createContext("/login", loginHandler);
        server.createContext("/logout", logoutHandler);
        server.createContext("/users", usersHandler);
        server.createContext("/messages", messagesHandler);
    }

    private HttpHandler loginHandler = exchange -> {
        if (!exchange.getRequestMethod().equals("POST")) {
            ResponseSender.sendResponse(exchange, METHOD_NOT_ALLOWED, "Post method expected");
            return;
        }
        if (!checkContentType(exchange)) {
            ResponseSender.sendResponse(exchange, BAD_REQUEST, "Wrong content type, application/json expected");
        } else {
            try {
                LoginRequest request = jsonParser.fromJson(exchange.getRequestBody(), LoginRequest.class);
                String username = request.getUsername();
                if (names.contains(username)) {
                    ResponseSender.sendResponse(exchange, UNAUTHORIZED,
                            "Www-Authenticate", "Token realm='Username is already in use'");
                } else {
                    LoginResponse response;
                    synchronized (this) {
                        int id = maxId + 1;
                        UUID token = UUID.randomUUID();
                        User newUser = new User(id, username);
                        users.put(token, newUser);
                        ids.put(id, token);
                        names.add(username);
                        maxId = id;
                        response = new LoginResponse(id, username, true, token);
                    }
                    String responseJson = jsonParser.toJson(response);
                    sendResponseJson(exchange, responseJson);
                }
            } catch (JsonSyntaxException e) {
                ResponseSender.sendResponse(exchange, BAD_REQUEST, "Wrong json structure, LoginRequest expected");
            }
        }
    };

    private HttpHandler logoutHandler = exchange -> {
        if (!exchange.getRequestMethod().equals("POST")) {
            ResponseSender.sendResponse(exchange, METHOD_NOT_ALLOWED, "Post method expected");
            return;
        }
        UUID tokenUUID = authorization(exchange);
        if (tokenUUID != null) {
            synchronized (this) {
                User user = users.get(tokenUUID);
                names.remove(user.getUsername());
                ids.remove(user.getId());
                users.remove(tokenUUID);
            }
            String responseJson = jsonParser.toJson(new LogoutResponse("bye!"));
            sendResponseJson(exchange, responseJson);
        }
    };

    private HttpHandler usersHandler = exchange -> {
        if (!exchange.getRequestMethod().equals("GET")) {
            ResponseSender.sendResponse(exchange, METHOD_NOT_ALLOWED, "Get method expected");
            return;
        }
        UUID tokenUUID = authorization(exchange);
        if (tokenUUID != null) {
            String path = exchange.getRequestURI().getPath();
            String userId = path.substring(path.lastIndexOf('/'));
            if (userId.equals("/users")) {
                List<User> responseUsers;
                synchronized (this) {
                    responseUsers = new ArrayList<>(users.values());
                }
                String responseJson = jsonParser.toJson(new UsersResponse(responseUsers));
                sendResponseJson(exchange, responseJson);
            } else {
                try {
                    userId = userId.substring(1);
                    Integer id = Integer.valueOf(userId);
                    System.err.println(id);
                    User responseUser = null;
                    synchronized (this) {
                        if (ids.containsKey(id)) {
                            responseUser = users.get(ids.get(id));
                        }
                    }
                    if (responseUser != null) {
                        String responseJson = jsonParser.toJson(new OnlineResponse(responseUser));
                        sendResponseJson(exchange, responseJson);
                    } else {
                        ResponseSender.sendResponse(exchange, NOT_FOUND, "User doesn't exist");
                    }
                } catch (NumberFormatException e) {
                    ResponseSender.sendResponse(exchange, NOT_FOUND, "Wrong id");
                }
            }
        }
    };

    private HttpHandler messagesHandler = exchange -> {
        if (exchange.getRequestMethod().equals("GET")) {
            UUID token = authorization(exchange);
            if (token != null) {
                MessagesQuery query = MessagesQuery.fromQueryStr(exchange.getRequestURI().getQuery());
                if (query == null) {
                    ResponseSender.sendResponse(exchange, BAD_REQUEST, "Wrong querry");
                } else {
                    List<Message> responseMessages = null;
                    try {
                        if (query.getCount() == -1) {
                            if (messages.size() - query.getOffset() > MAX_MESSAGES) {
                                responseMessages = messages.subList(messages.size() - MAX_MESSAGES, messages.size());
                            } else {
                                responseMessages = messages.subList(query.getOffset(), messages.size());
                            }
                        } else if (query.getOffset() == -1) {
                            if (query.getCount() > MAX_MESSAGES) {
                                responseMessages = messages.subList(messages.size() - MAX_MESSAGES, messages.size());
                            } else {
                                responseMessages = messages.subList(messages.size() - query.getCount(), messages.size());
                            }
                        } else {
                            if (query.getCount() > MAX_MESSAGES) {
                                responseMessages = messages.subList(query.getOffset(), MAX_MESSAGES);
                            } else {
                                responseMessages = messages.subList(query.getOffset(), query.getCount());
                            }
                        }
                        String responseJson = jsonParser.toJson(new MessagesResponse(responseMessages));
                        sendResponseJson(exchange, responseJson);
                    } catch (IndexOutOfBoundsException e) {
                        ResponseSender.sendResponse(exchange, BAD_REQUEST, "Wrong message id");
                    }
                }
            }
        } else if (exchange.getRequestMethod().equals("POST")) {
            UUID token = authorization(exchange);
            if (token != null) {
                if (!checkContentType(exchange)) {
                    ResponseSender.sendResponse(exchange, BAD_REQUEST, "Wrong content type, application/json expected");
                } else {
                    try {
                        MessageRequest request = jsonParser.fromJson(exchange.getRequestBody(), MessageRequest.class);
                        Message msg;
                        synchronized (this) {
                            msg = new Message(messages.size(), request.getMessage(), users.get(token).getId());
                            messages.add(msg);
                        }
                        ResponseSender.sendResponse(exchange, OK, jsonParser.toJson(new MessageResponse(msg.getId(), msg.getMessage())));
                        MessagesEndpoint.broadcast(jsonParser.toJson(msg));
                    } catch (JsonSyntaxException e) {
                        ResponseSender.sendResponse(exchange, BAD_REQUEST, "Wrong json structure, MessageRequest expected");
                    }
                }
            }
        } else {
            ResponseSender.sendResponse(exchange, METHOD_NOT_ALLOWED, "Get or POST method expected");
        }
    };

    private boolean checkContentType(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        List<String> contentType = headers.get("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            return false;
        }
        return true;
    }

    private UUID authorization(HttpExchange exchange) throws IOException {
        UUID tokenUUID = null;
        Headers headers = exchange.getRequestHeaders();
        List<String> authorization = headers.get("Authorization");
        if (authorization == null) {
            ResponseSender.sendResponse(exchange, UNAUTHORIZED, "Authorization token expected");
        } else {
            String [] tokenWords = exchange.getRequestHeaders().get("Authorization").get(0).split(" ");
            String token = tokenWords[tokenWords.length - 1];
            try {
                tokenUUID = UUID.fromString(token);
                if (!users.containsKey(tokenUUID)) {
                    ResponseSender.sendResponse(exchange, FORBIDDEN, "Access is denied");
                }
            } catch (IllegalArgumentException e) {
                ResponseSender.sendResponse(exchange, BAD_REQUEST, "Wrong token format");
            }
        }
        synchronized (this) {
            users.get(tokenUUID).markAsOnline();
        }
        return tokenUUID;
    }

    public UUID authorization(Session session, String msg) throws IOException {
        UUID tokenUUID = null;
        try {
            tokenUUID = UUID.fromString(msg);
            if (!users.containsKey(tokenUUID)) {
                session.getBasicRemote().sendText("denied");
            }
        } catch (IllegalArgumentException e) {
            session.getBasicRemote().sendText("bad_token");
        }
        return tokenUUID;
    }

    private void sendResponseJson(HttpExchange exchange, String json) throws IOException {
        ResponseSender.sendResponse(exchange, OK, "Content-Type",
                "application/json", json);
    }
}