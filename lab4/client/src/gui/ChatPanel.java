package gui;

import client.Message;
import client.User;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChatPanel extends JPanel {

    private JButton sendButton = new JButton("Send");
    private JTextField messageArea = new JTextField("Enter message...");
    private JTextArea messagesArea = new JTextArea();
    private JList<String> userList = new JList<>();
    private Map<Integer, User> users = new ConcurrentHashMap<>();
    private Map<Integer, User> allUsers = new ConcurrentHashMap<>();
    private String username;
    private JLabel errorLabel = new JLabel("<html><font color='red'>Connecting...</font></html>");
    private boolean firstUsers = true;

    public ChatPanel(String username) {
        messageArea.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                messageArea.setText("");
                messageArea.removeFocusListener(this);
            }
        });
        this.username = username;
        errorLabel.setVisible(false);
        JScrollPane userScrollPane = new JScrollPane();
        JScrollPane messagesScrollPane = new JScrollPane();
        userScrollPane.setViewportView(userList);
        messagesScrollPane.setViewportView(messagesArea);
        setLayout(new BorderLayout());
        userList.setLayoutOrientation(JList.VERTICAL);
        sendButton.setFont(new Font("arial", Font.BOLD, 18));
        messageArea.setFont(new Font("arial", Font.PLAIN, 17));
        messagesArea.setFont(new Font("arial", Font.PLAIN, 16));
        errorLabel.setFont(new Font("times new roman", Font.PLAIN, 20));
        userList.setFont(new Font("times new roman", Font.PLAIN, 14));
        messagesArea.setLineWrap(true);
        DefaultCaret caret = (DefaultCaret)messageArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        messagesArea.setEditable(false);
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        add(errorLabel, BorderLayout.NORTH);
        add(messagesScrollPane, BorderLayout.CENTER);
        add(userScrollPane, BorderLayout.EAST);
        inputPanel.add(messageArea, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        messagesScrollPane.setBorder(BorderFactory.createEmptyBorder());
        inputPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        userScrollPane.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.BLACK));
        add(inputPanel, BorderLayout.SOUTH);
    }

    public void showMessages(List<Message> messages) {
        if (messages.size() > 0) {
            for (Message msg : messages) {
                User user = allUsers.get(msg.getAuthor());
                if (user != null)
                    messagesArea.append(allUsers.get(msg.getAuthor()).getUsername() + ": " + msg.getMessage() + "\n");
            }
        }
    }

    public void clear(String msg) {
        messageArea.setText("");
    }

    public void setMessageText(String msg) {
        messageArea.setText(msg);
    }

    public void setUsers(List<User> users) {
        Map<Integer, User> newUsers = users.stream().collect(Collectors.toMap(user -> user.getId(), user -> user));
        for (User user : newUsers.values()) {
            allUsers.put(user.getId(), user);
            if (!firstUsers && !this.users.containsKey(user.getId())) {
                messagesArea.append(user.getUsername() + " login\n");
            }
        }
        for (User user: this.users.values()) {
            if (!firstUsers && !newUsers.containsKey(user.getId())) {
                messagesArea.append(user.getUsername() + " logout\n");
            }
        }
        firstUsers = false;
        this.users = newUsers;
        List<String> listElems = new ArrayList<>();
        listElems.add("Users:");
        for (User user : users) {
            String postfix = "";
            if (user.getOnline() != null && user.getOnline()) {
                postfix = "(online)";
            }
            String desc = (user.getId() + 1) + ". " + user.getUsername() + postfix;
            listElems.add(desc);
        }
        userList.setListData(listElems.toArray(new String[0]));
    }

    public void setSendListener(ActionListener listener) {
        sendButton.addActionListener(listener);
    }

    public String getMessage() {
        return messageArea.getText();
    }

    public JButton getSendButton() {
        return sendButton;
    }

    public void setError() {
        errorLabel.setVisible(true);
        sendButton.setEnabled(false);
    }

    public void reSetError() {
        errorLabel.setVisible(false);
        sendButton.setEnabled(true);
    }

}
