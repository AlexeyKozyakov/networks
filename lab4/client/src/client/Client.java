package client;

import com.google.gson.JsonSyntaxException;
import exchange.*;
import gui.AppFrame;
import gui.ChatPanel;
import gui.LoginPanel;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import util.JsonParser;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class Client {
    private final static int BAD_REQUEST = 400, UNAUTHORIZED = 401, OK = 200, FORBIDDEN = 403, METHOD_NOT_ALLOWED = 405;
    private final static int NOT_FOUND = 404;
    private final static int TRY_NUM = 5, DELAY = 1000;
    private ChatPanel chatPanel;
    private LoginPanel loginPanel;
    private HttpClient client;
    private AppFrame appFrame = new AppFrame();
    private String url;
    private String port;
    private String username;
    private JsonParser jsonParser = new JsonParser();
    private UUID token;
    private int lastMsgId = 0;
    private Thread updating;
    private boolean errorFlag = false;

    public Client() {
        loginPanel = appFrame.getLoginPanel();
        client = HttpClients.createDefault();
        appFrame.setCloseOperation(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                stop();
                System.exit(0);
            }
        });
    }

    public void start() {
        setLoginListener();
        appFrame.setVisible(true);
    }

    private void stop() {
        if (updating != null) {
            updating.interrupt();
            synchronized (this) {
                if (!errorFlag) {
                    String content = jsonParser.toJson(new LoginRequest(username));
                    ResponseResult result = doPostAuthorizedRequest("/logout", content, true);
                    if (result == null || result.getCode() != OK) {
                        System.err.println("Server error");
                    }
                    MessageResponse response = jsonParser.fromJson(result.getBody(), MessageResponse.class);
                    if (!response.getMessage().equals("bye!")) {
                        System.err.println("Error on server");
                    }
                }
            }
        }
    }

    private void setLoginListener() {
        loginPanel.setLoginListener(actionEvent -> {
            loginPanel.setStatus("Connecting...");
            loginPanel.getLoginButton().setEnabled(false);
            new Thread(() -> {
                username = loginPanel.getName();
                port = loginPanel.getport();
                url = loginPanel.getUrl();
                if (!username.equals("") && !url.equals("") && !port.equals("")) {
                    if (!url.startsWith("http://")) {
                        url = "http://" + url;
                    }
                    doLoginRequest();
                } else {
                    setLoginError("Some of the fields is empty");
                }
                SwingUtilities.invokeLater(() -> {
                    loginPanel.getLoginButton().setEnabled(true);
                });
            }).start();
        });
    }

    private void doLoginRequest() {
        HttpPost loginPost = new HttpPost(url + ":" + port + "/login");
        loginPost.addHeader("Content-Type", "application/json");
        try {
            loginPost.setEntity(new StringEntity(jsonParser.toJson(new LoginRequest(username))));
            try {
                HttpResponse response = client.execute(loginPost);
                if (response.getStatusLine().getStatusCode() == UNAUTHORIZED
                        && response.getFirstHeader("WWW-Authenticate").getElements()[0].getValue()
                        .equals("'Username is already in use'")) {
                    setLoginError("Username is already in use");
                } else if (response.getStatusLine().getStatusCode() == OK) {
                    try {
                        LoginResponse loginResponse = jsonParser.fromJson(response.getEntity().getContent(),
                                LoginResponse.class);
                        openChat(loginResponse);
                    } catch (JsonSyntaxException e) {
                        setLoginError("Error on server");
                        System.err.println("Wrong json format");
                    }
                } else {
                    setLoginError("Error on server");
                }
            } catch (ClientProtocolException e) {
                setLoginError("Cannot find host");
            } catch (IOException e1) {
                setLoginError("Connection error");
                System.err.println(e1);
            } catch (IllegalArgumentException e2) {
                setLoginError("Wrong port number");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void setLoginError(String error) {
        SwingUtilities.invokeLater(() -> {
            loginPanel.setErrorStatus(error);
        });
    }

    private void openChat(LoginResponse loginResponse) {
        token = loginResponse.getToken();
        SwingUtilities.invokeLater(() -> {
            appFrame.openChatPanel(loginResponse.getUsername());
            chatPanel = appFrame.getChatPanel();
            setSendListener();
            updating = new Thread(updatingTask);
            updating.start();
        });
    }

    private void setSendListener() {
        chatPanel.setSendListener(actionEvent -> {
            new Thread(() -> {
                String message = chatPanel.getMessage();
                SwingUtilities.invokeLater(() -> chatPanel.clear(message));
                String content = jsonParser.toJson(new MessageRequest(message));
                ResponseResult result = doPostAuthorizedRequest("/messages", content, true);
                if (result == null || result.getCode() != OK) {
                    System.err.println("Server error");
                }
                MessageResponse response = jsonParser.fromJson(result.getBody(), MessageResponse.class);
                if (!response.getMessage().equals(message)) {
                    System.err.println("Error on server");
                }
            }).start();
        });
    }

    private Runnable updatingTask = () -> {
        while (!Thread.interrupted()) {
            ResponseResult usersResult = doGetAuthorizedRequest("/users");
            if (usersResult == null || usersResult.getCode() != OK) {
                if (usersResult != null)
                    System.err.println(usersResult.getCode());
                else {
                    break;
                }
            } else {
                UsersResponse users = jsonParser.fromJson(usersResult.getBody(), UsersResponse.class);
                SwingUtilities.invokeLater(() -> chatPanel.setUsers(users.getUsers()));
            }
            ResponseResult messagesResult = doGetAuthorizedRequest("/messages?offset=" + lastMsgId + "&count=" + -1);
            if (messagesResult == null || messagesResult.getCode() != OK) {
                if (messagesResult != null)
                    System.err.println(messagesResult.getCode());
                else {
                    break;
                }
            } else {
                MessagesResponse messages = jsonParser.fromJson(messagesResult.getBody(), MessagesResponse.class);
                if (messages != null && messages.getMessages().size() != 0) {
                    SwingUtilities.invokeLater(() -> chatPanel.showMessages(messages.getMessages()));
                    lastMsgId = messages.getMessages().get(messages.getMessages().size() - 1).getId() + 1;
                }
            }
            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                break;
            }
        }
    };

    private ResponseResult doPostAuthorizedRequest(String uri, String content, boolean json) {
        HttpPost request = new HttpPost(url + ":" + port + uri);
        request.addHeader("Authorization", "Token " + token.toString());
        if (json) {
            request.addHeader("Content-Type", "application/json");
        }
        try {
            request.setEntity(new StringEntity(content));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ResponseResult response = getResponseResult(request);
        return response;
    }

    private ResponseResult doGetAuthorizedRequest(String uri) {
        HttpGet request = new HttpGet(url + ":" + port + uri);
        request.addHeader("Authorization", "Token " + token.toString());
        ResponseResult response = getResponseResult(request);
        return response;

    }

    private ResponseResult getResponseResult(HttpRequestBase request) {
        boolean success = false;
        while (!success) {
            try {
                HttpResponse response = client.execute(request);
                if (response.getStatusLine().getStatusCode() == FORBIDDEN) {
                    success = true;
                    lastMsgId = 0;
                    changeErrorFlag(false);
                    SwingUtilities.invokeLater(() -> {
                        loginPanel.setErrorStatus("Authorization error, try again");
                        appFrame.openLoginPanel();
                        updating.interrupt();
                    });
                } else {
                    success = true;
                    SwingUtilities.invokeLater(() -> chatPanel.reSetError());
                    changeErrorFlag(false);
                    return new ResponseResult(response.getStatusLine().getStatusCode(),
                            response.getEntity().getContent());
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> chatPanel.setError());
                changeErrorFlag(true);
            }
        }
        changeErrorFlag(false);
        return null;
    }

    private void changeErrorFlag(boolean val) {
        synchronized (this) {
            errorFlag = val;
        }
    }

}
