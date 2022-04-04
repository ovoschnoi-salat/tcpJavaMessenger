package client;

import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.GroupLayout;
import javax.swing.SwingConstants;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.Socket;

/**
 * Class that show server connection dialog window.
 */
public class ConnectServerDialog extends JDialog {
    /**
     * String with default server host name.
     */
    public static String defaultHost = "127.0.0.1";
    /**
     * String with default server port number.
     */
    public static String defaultPort = "31337";
    /**
     * Text field for server host name.
     */
    private final JTextField hostField;
    /**
     * Text field for server port number.
     */
    private final JTextField portField;

    /**
     * Initializes server connection dialog window.
     */
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
        getRootPane().setDefaultButton(connectButton);
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
        // showing window
        setVisible(true);
    }

    /**
     * Called when connect button pressed.
     * Tries to connect to server and show authentication dialog window.
     *
     * @param e ignored
     */
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