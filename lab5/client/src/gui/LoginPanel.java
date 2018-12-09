package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class LoginPanel extends JPanel {

    private JButton loginButton = new JButton("Login");
    private JTextField nameField = new JTextField();
    private JTextField urlField = new JTextField("127.0.0.1");
    private JTextField portField = new JTextField("8080");
    private JLabel usernameLabel = new JLabel("User name:");
    private JLabel urlLabel = new JLabel("url:");
    private JLabel portLabel = new JLabel("port:");
    private JLabel statusLabel = new JLabel("");

    public LoginPanel() {
        nameField.setFont(new Font("arial", Font.PLAIN, 17));
        urlField.setFont(new Font("arial", Font.PLAIN, 17));
        portField.setFont(new Font("arial", Font.PLAIN, 17));
        add(urlField);
        add(urlLabel);
        add(portLabel);
        add(portField);
        add(nameField);
        add(loginButton);
        add(usernameLabel);
        add(statusLabel);
        urlField.setBounds(250, 205, 300, 30);
        urlLabel.setBounds(160, 205, 300, 30);
        portField.setBounds(250, 255, 300, 30);
        portLabel.setBounds(160, 255, 300, 30);
        usernameLabel.setBounds(160, 305, 300, 30);
        nameField.setBounds(250, 305, 300, 30);
        loginButton.setBounds(250, 360, 300, 30);
        statusLabel.setBounds(280, 155, 300, 30);
        setLayout(null);
    }

    public String getName() {
        return nameField.getText();
    }

    public void setLoginListener(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public JButton getLoginButton() {
        return loginButton;
    }

    public String getUrl() {
        return urlField.getText();
    }

    public void setErrorStatus(String status) {
        statusLabel.setText("<html><font color='red'>" + status + "</font></html>");
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public String getport() {
        return portField.getText();
    }
}
