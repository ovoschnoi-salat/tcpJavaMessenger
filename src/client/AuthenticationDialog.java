package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class AuthenticationDialog extends JDialog {
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final String[] items = {"Log in", "Sign up"};
    private final JComboBox<String> comboBox = new JComboBox<>(items);
    private final JButton authButton = new JButton(items[0]);
    private OutputStreamWriter out = null;
    private BufferedReader in = null;
    private int selectedMethod = 0;
    private final Socket newSocket;

    public AuthenticationDialog(Socket socket) {
        super((Frame) null, "Authentication", true);
        // setting server socket
        newSocket = socket;
        // setting text fields for username and password input
        JLabel usernameLabel = new JLabel("Username: ");
        JLabel passwordLabel = new JLabel("Password: ");
        // setting action for changing authentication method
        comboBox.addActionListener(this::authMethodChangingActionPerformed);
        // setting authentication button action
        authButton.addActionListener(this::authActionPerformed);
        // setting exit button action for server authentication dialog
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this::exitActionPerformed);
        // setting layout for server authentication dialog
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.linkSize(SwingConstants.HORIZONTAL, usernameField, passwordField);
        layout.linkSize(SwingConstants.VERTICAL, usernameField, passwordField);
        layout.linkSize(SwingConstants.HORIZONTAL, usernameLabel, passwordLabel);
        layout.linkSize(SwingConstants.VERTICAL, usernameLabel, passwordLabel);
        layout.linkSize(SwingConstants.VERTICAL, usernameLabel, usernameField);
        layout.linkSize(SwingConstants.VERTICAL, comboBox, authButton);
        layout.linkSize(SwingConstants.VERTICAL, cancelButton, authButton);
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                        .addComponent(usernameLabel)
                        .addComponent(usernameField))
                .addGroup(layout.createParallelGroup()
                        .addComponent(passwordLabel)
                        .addComponent(passwordField))
                .addGroup(layout.createParallelGroup()
                        .addComponent(comboBox)
                        .addComponent(authButton)
                        .addComponent(cancelButton))
        );
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                        .addComponent(usernameLabel)
                        .addComponent(passwordLabel)
                        .addComponent(comboBox))
                .addGroup(layout.createParallelGroup()
                        .addComponent(usernameField)
                        .addComponent(passwordField)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(authButton)
                                .addComponent(cancelButton)))
        );
        pack();
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        try {
            out = new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8);
            in = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
            setVisible(true);
        } catch (IOException e) {
            error("Error occurred creating connection");
        }
    }

    public void authMethodChangingActionPerformed(ActionEvent e) {
        selectedMethod = comboBox.getSelectedIndex();
        String item = (String) comboBox.getSelectedItem();
        authButton.setText(item);
    }

    public void authActionPerformed(ActionEvent e) {
        if (in == null || out == null) error("Something went wrong");
        try {
            String username = usernameField.getText().trim();
            String password = String.valueOf(passwordField.getPassword());
            if (username.isBlank() || password.isBlank()) {
                JOptionPane.showMessageDialog(this, "Username or password field left blank",
                        "Wrong input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            StringBuilder sb = new StringBuilder();
            if (selectedMethod == 0) {
                sb.append("auth ");
            } else {
                sb.append("reg ");
            }
            sb.append(username);
            sb.append(" ");
            sb.append(password);
            sb.append("\n");
            out.write(sb.toString());
            out.flush();
            try {
                String buffer = in.readLine();
                if (buffer != null) {
                    if (buffer.equals("Accepted")) {
                        buffer = in.readLine();
                        dispose();
                        new Client(newSocket, username, buffer);
                    } else {
                        JOptionPane.showMessageDialog(this, buffer,
                                "Access denied",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    error("Error: connection closed");
                }
            } catch (IOException ex) {
                error("Error occurred while receiving answer from server: " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                error("Error: wrong user id received from server: " + ex.getMessage());
            }
        } catch (IOException ex) {
            error("Error occurred while sending request to server: " + ex.getMessage());
        }
    }

    public void exitActionPerformed(ActionEvent e) {
        error("Authentication canceled");
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this,
                msg,
                "Error",
                JOptionPane.ERROR_MESSAGE);
        dispose();
        new ConnectServerDialog();
    }
}
