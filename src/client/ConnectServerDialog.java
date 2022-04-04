package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.Socket;

public class ConnectServerDialog extends JDialog {
    public static String defaultHost;
    public static String defaultPort;
    private final JTextField hostField;
    private final JTextField portField;

    public ConnectServerDialog() {
        super((Frame) null, "Server connection", true);
        // setting text fields for host and port input
        JLabel hostLabel = new JLabel("Host: ");
        JLabel portLabel = new JLabel("Port: ");
        hostField = new JTextField(defaultHost, 16);
        portField = new JTextField(defaultPort, 16);
        // setting connection button action for server connection dialog
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(this::connectActionPerformed);
        //setting layout for server connection dialog
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.linkSize(SwingConstants.HORIZONTAL, hostField, portField);
        layout.linkSize(SwingConstants.VERTICAL, hostField, portField);
        layout.linkSize(SwingConstants.HORIZONTAL, hostLabel, portLabel);
        layout.linkSize(SwingConstants.VERTICAL, hostLabel, portLabel);
        layout.linkSize(SwingConstants.VERTICAL, hostField, hostLabel);
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                        .addComponent(hostLabel)
                        .addComponent(hostField))
                .addGroup(layout.createParallelGroup()
                        .addComponent(portLabel)
                        .addComponent(portField))
                .addGroup(layout.createParallelGroup()
                        .addComponent(connectButton))
        );
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                        .addComponent(hostLabel)
                        .addComponent(portLabel))
                .addGroup(layout.createParallelGroup()
                        .addComponent(hostField)
                        .addComponent(portField)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(connectButton)))
        );
        pack();
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void connectActionPerformed(ActionEvent e) {
        try {
            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            Socket newSocket = new Socket(host, Integer.parseInt(port));
            dispose();
            new AuthenticationDialog(newSocket);
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error connecting to server: " + ex.getMessage(),
                    "Server connection error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}