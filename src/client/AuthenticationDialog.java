package client;

import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.GroupLayout;
import javax.swing.SwingConstants;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Class that show authentication dialog window.
 */
public class AuthenticationDialog extends JDialog {
    /**
     * Username text field.
     */
    private final JTextField usernameField = new JTextField(16);
    /**
     * Password text field.
     */
    private final JPasswordField passwordField = new JPasswordField(16);
    /**
     * Authentication options names.
     */
    private final String[] items = {"Log in", "Sign up"};
    /**
     * Authentication options combo box.
     */
    private final JComboBox<String> comboBox = new JComboBox<>(items);
    /**
     * Authentication button.
     */
    private final JButton authButton = new JButton(items[0]);
    /**
     * Server output stream.
     */
    private OutputStreamWriter out = null;
    /**
     * Server input stream.
     */
    private BufferedReader in = null;
    /**
     * Selected authentication method.
     */
    private int selectedMethod = 0;
    /**
     * Server connection socket.
     */
    private final Socket newSocket;

    /**
     * Authentication window Initializer.
     *
     * @param socket server socket
     */
    public AuthenticationDialog(Socket socket) {
        super((Frame) null, "Authentication", true);
        if (socket == null || socket.isClosed())
            throw new IllegalArgumentException("Illegal arguments passed");
        // setting server socket
        newSocket = socket;
        // setting text fields for username and password input
        JLabel usernameLabel = new JLabel("Username: ");
        JLabel passwordLabel = new JLabel("Password: ");
        // setting action for changing authentication method
        comboBox.addActionListener(this::authMethodChangingActionPerformed);
        // setting authentication button action
        authButton.addActionListener(this::authActionPerformed);
        getRootPane().setDefaultButton(authButton);
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
            in = new BufferedReader(new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8));
            setVisible(true);
        } catch (IOException e) {
            error("Error occurred creating connection");
        }
    }

    /**
     * Called when authentication method changed.
     * Changes authentication method.
     *
     * @param e ignored
     */
    public void authMethodChangingActionPerformed(ActionEvent e) {
        selectedMethod = comboBox.getSelectedIndex();
        String item = (String) comboBox.getSelectedItem();
        authButton.setText(item);
    }

    /**
     * Called when authenticate button pressed.
     * Sends authentication request to server.
     * Shows main window on success.
     *
     * @param e ignored
     */
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
                        dispose();
                        new Client(newSocket, username, in);
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

    /**
     * Called when cancel button pressed.
     * Closes window.
     *
     * @param e ignored
     */
    public void exitActionPerformed(ActionEvent e) {
        error("Authentication canceled");
    }

    /**
     * Called if error occurred.
     * Closes window and shows server connection dialog.
     * Shows notification with cause before closing.
     *
     * @param msg cause message
     */
    private void error(String msg) {
        JOptionPane.showMessageDialog(this,
                msg,
                "Error",
                JOptionPane.ERROR_MESSAGE);
        dispose();
        new ConnectServerDialog();
    }
}
