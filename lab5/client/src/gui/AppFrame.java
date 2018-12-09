package gui;

import javax.swing.*;
import java.awt.event.WindowAdapter;

public class AppFrame extends JFrame {
    LoginPanel loginPanel = new LoginPanel();
    ChatPanel chatPanel;

    public AppFrame() {
        super("Chat");
        add(loginPanel);
        getRootPane().setDefaultButton(loginPanel.getLoginButton());
        setSize(800, 600);
        setResizable(false);
    }

    public void openChatPanel(String login) {
        chatPanel = new ChatPanel(login);
        remove(loginPanel);
        add(chatPanel);
        getRootPane().setDefaultButton(chatPanel.getSendButton());
        revalidate();
        repaint();
    }

    public void openLoginPanel() {
        remove(chatPanel);
        add(loginPanel);
        getRootPane().setDefaultButton(loginPanel.getLoginButton());
        revalidate();
        repaint();
    }

    public LoginPanel getLoginPanel() {
        return loginPanel;
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public void setCloseOperation(WindowAdapter adapter) {
        this.addWindowListener(adapter);
    }
}
