package client;

import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.GroupLayout;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

public class Client extends JFrame {
    /**
     * Messages area.
     */
    private final JTextPane msgArea = new JTextPane();
    /**
     * Text field for new messages.
     */
    private final JTextField textField = new JTextField("Your message...");
    /**
     * Button for accepting new friends from requests
     */
    private final JButton requestsButton = new JButton("Friends requests");
    /**
     * Friends list.
     */
    private final FriendsListModel friends = new FriendsListModel();
    /**
     * Friends list area.
     */
    private final JList<String> friendsList = new JList<>(friends);
    /**
     * Selected friend.
     */
    private Friend selectedFriend = null;
    /**
     * User's username.
     */
    private final String clientUsername;
    /**
     * Server connection socket.
     */
    private final Socket clientSocket;
    /**
     * Server output stream.
     */
    private OutputStreamWriter out = null;

    /**
     * Main window initializer.
     * Initializes main window with passed socket, username and server input stream.
     *
     * @param socket   server connection socket
     * @param username user's username
     * @param in       server input stream
     */
    Client(Socket socket, String username, BufferedReader in) {
        super("Messenger (" + username + ")");
        if (socket == null || socket.isClosed() ||
                username == null || username.isBlank())
            throw new IllegalArgumentException("Illegal arguments passed");
        clientSocket = socket;
        clientUsername = username;
        // setting up messages area
        msgArea.setEditable(false);
        JScrollPane msgAreaScrollPane = new JScrollPane(
                msgArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // setting up buttons
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(this::sendActionPerformed);
        getRootPane().setDefaultButton(sendButton);
        requestsButton.addActionListener(this::requestsActionPerformed);
        JButton addFriendButton = new JButton("Add new friend");
        addFriendButton.addActionListener(this::sendRequestActionPerformed);
        // setting up frinds list
        friendsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane friendsScrollPane = new JScrollPane(friendsList);
        friendsList.addListSelectionListener(this::newFriendSelected);
        // setting up layout for main window
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.linkSize(SwingConstants.VERTICAL, textField, sendButton);
        layout.linkSize(SwingConstants.VERTICAL, requestsButton, addFriendButton);
        layout.linkSize(SwingConstants.HORIZONTAL, requestsButton, addFriendButton);
        layout.linkSize(SwingConstants.HORIZONTAL, requestsButton, friendsScrollPane);
        layout.setVerticalGroup(layout.createParallelGroup().addGroup(
                        layout.createSequentialGroup()
                                .addComponent(msgAreaScrollPane)
                                .addGroup(layout.createParallelGroup()
                                        .addComponent(textField)
                                        .addComponent(sendButton)))
                .addGroup(layout.createSequentialGroup()
                        .addComponent(addFriendButton)
                        .addComponent(requestsButton)
                        .addComponent(friendsScrollPane))
        );
        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(
                        layout.createParallelGroup()
                                .addComponent(msgAreaScrollPane)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(textField)
                                        .addComponent(sendButton)))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(addFriendButton)
                        .addComponent(requestsButton)
                        .addComponent(friendsScrollPane))
        );
        Dimension size = new Dimension(400, 400);
        msgAreaScrollPane.setPreferredSize(size);
        pack();
        setMinimumSize(size);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        // setting function that will be called on window closing
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        });
        // adding friends from server answer to friends list
        try {
            if (setFriends(in.readLine())) {
                // setting output stream to server
                out = new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8);
                // showing window
                setVisible(true);
                // setting server messages listener thread
                Thread thread = new Thread(() -> {
                    while (!clientSocket.isInputShutdown()) {
                        String buffer;
                        try {
                            buffer = in.readLine();
                        } catch (IOException e) {
                            continue;
                        }
                        if (buffer == null)
                            break;
                        SwingUtilities.invokeLater(() -> processReceivedMessage(buffer));
                    }
                    SwingUtilities.invokeLater(() -> error("Connection closed"));
                });
                thread.setDaemon(true);
                thread.start();
            } else {
                error("Illegal answer from server received");
            }
        } catch (IOException e) {
            error("Connection closed");
        }
    }

    /**
     * Called when another friend selected.
     * Sets new friend selected from friends list.
     * Sends new messages request to server.
     *
     * @param e ignored
     */
    public void newFriendSelected(ListSelectionEvent e) {
        int index = friendsList.getSelectedIndex();
        Friend newSelection = friends.get(index);
        if (selectedFriend != newSelection) {
            selectedFriend = newSelection;
            reloadMessages();
        }
        getNewMessagesFromSelected();
    }

    /**
     * Called when friend requests button pressed.
     * Send request to server for message with friends requests.
     *
     * @param event ignored
     */
    public void requestsActionPerformed(ActionEvent event) {
        try {
            out.write("GetFriendsRequestsList\n");
            out.flush();
        } catch (IOException e) {
            error("Error sending request to server: " + e.getMessage());
        }
    }

    /**
     * Called when accept friends request pressed.
     * Shows dialog with friends requests.
     *
     * @param event ignored
     */
    public void sendRequestActionPerformed(ActionEvent event) {
        new SendFriendsRequestDialog(this, out);
    }

    /**
     * Called when send button pressed.
     * Sends message to current selected friend if selected and message isn't blank.
     * Sets message input field empty, adds message to memory
     * and prints message to message area on success.
     *
     * @param event ignored
     */
    public void sendActionPerformed(ActionEvent event) {
        if (textField.getText().isBlank() || selectedFriend == null) return;
        try {
            String msg = textField.getText().trim();
            send("SendMessage " + selectedFriend.id + " " + msg + "\n");
            Message newMsg = new Message(textField.getText().trim(), true);
            selectedFriend.messages.add(newMsg);
            printMsg(newMsg);
            textField.setText(null);
        } catch (IOException e) {
            error("Error occurred while sending message: " + e.getMessage());
        }
    }

    /**
     * Class that shows dialog with ability to add new friends from friends requests list.
     */
    private class RequestsDialog extends JDialog {
        /**
         * Combo box with list of friends requests.
         */
        private JComboBox<String> comboBox = null;
        /**
         * Server output stream.
         */
        private final OutputStreamWriter out;
        /**
         * Array with requests users ids.
         */
        private final Vector<String> requestsIds = new Vector<>();

        /**
         * Constructs class with specified output stream and specified list of friends.
         * Automatically closes if wrong answer from server received.
         * Sends specified request acceptation to server on accept button press.
         *
         * @param parent parent window
         * @param out    output stream
         * @param msg    answer from server with requests list
         */
        public RequestsDialog(Frame parent, OutputStreamWriter out, String msg) {
            super(parent, "Friend requests", true);
            this.out = out;
            // setting accept request button action
            JButton acceptButton = new JButton("Accept");
            acceptButton.addActionListener(this::acceptActionPerformed);
            getRootPane().setDefaultButton(acceptButton);
            // setting cancel button action
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(this::cancelActionPerformed);
            // splitting server answer
            String[] buffer = msg.split(" ");
            // trying to add usernames and ids to vectors
            try {
                int n = Integer.parseInt(buffer[0]);
                if (n * 2 + 1 != buffer.length)
                    throw new IllegalArgumentException("Received message with wrong format from server");
                Vector<String> requestsUsernames = new Vector<>();
                for (int i = 0; i < n; i++) {
                    requestsUsernames.add(buffer[i * 2 + 1]);
                    requestsIds.add(buffer[(i + 1) * 2]);
                }
                comboBox = new JComboBox<>(requestsUsernames);
                // setting layout for dialog
                GroupLayout layout = new GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setAutoCreateGaps(true);
                layout.setAutoCreateContainerGaps(true);
                layout.linkSize(SwingConstants.VERTICAL, acceptButton, cancelButton);
                layout.setVerticalGroup(layout.createSequentialGroup()
                        .addComponent(comboBox)
                        .addGroup(layout.createParallelGroup()
                                .addComponent(acceptButton)
                                .addComponent(cancelButton))
                );
                layout.setHorizontalGroup(layout.createParallelGroup()
                        .addComponent(comboBox)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(acceptButton)
                                .addComponent(cancelButton))
                );
                pack();
                setResizable(false);
                setLocationRelativeTo(parent);
                setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                // showing window
                setVisible(true);
            } catch (IllegalArgumentException e) {
                dispose();
            }
        }

        /**
         * Called when accept button pressed.
         * Sends server message to accept friends request.
         *
         * @param event ignored
         */
        public void acceptActionPerformed(ActionEvent event) {
            try {
                assert comboBox != null;
                if (comboBox.getSelectedIndex() >= 0) {
                    out.write("AcceptRequest " + requestsIds.get(comboBox.getSelectedIndex()) + "\n");
                    out.flush();
                    dispose();
                }
            } catch (IOException e) {
                dispose();
                error("Error occurred while sending request to server: " + e.getMessage());
            }
        }

        public void cancelActionPerformed(ActionEvent event) {
            dispose();
        }
    }

    /**
     * Class that shows dialog with ability to send new friends requests.
     */
    private class SendFriendsRequestDialog extends JDialog {
        /**
         * Server output stream.
         */
        private final OutputStreamWriter out;
        /**
         * Text field for user's username.
         */
        private final JTextField usernameField = new JTextField(16);

        /**
         * Constructs class with specified output stream.
         * Sends server friends request to user with entered username on send button press.
         *
         * @param parent parent window
         * @param out    server output stream
         */
        public SendFriendsRequestDialog(Frame parent, OutputStreamWriter out) {
            super(parent, "Send new friends request", true);
            this.out = out;
            // setting up buttons
            JButton sendButton = new JButton("Send request");
            sendButton.addActionListener(this::sendRequestActionPerformed);
            getRootPane().setDefaultButton(sendButton);
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(this::cancelActionPerformed);
            // setting layout for server authentication dialog
            GroupLayout layout = new GroupLayout(getContentPane());
            getContentPane().setLayout(layout);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);
            layout.linkSize(SwingConstants.VERTICAL, sendButton, cancelButton);
            layout.setVerticalGroup(layout.createSequentialGroup()
                    .addComponent(usernameField)
                    .addGroup(layout.createParallelGroup()
                            .addComponent(sendButton)
                            .addComponent(cancelButton))
            );
            layout.setHorizontalGroup(layout.createParallelGroup()
                    .addComponent(usernameField)
                    .addGroup(layout.createSequentialGroup()
                            .addComponent(sendButton)
                            .addComponent(cancelButton))
            );
            pack();
            setResizable(false);
            setLocationRelativeTo(parent);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            // showing dialog window
            setVisible(true);
        }

        /**
         * Called when send button pressed.
         * Sends server friends request to user with entered username.
         *
         * @param event ignored
         */
        public void sendRequestActionPerformed(ActionEvent event) {
            try {
                String username = usernameField.getText().trim();
                if (!username.isBlank()) {
                    out.write("SendRequest " + usernameField.getText().trim() + "\n");
                    out.flush();
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Username field left blank",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                dispose();
                error("Error sending request to server" + e.getMessage());
            }
        }

        /**
         * Called when cancel button pressed.
         * Closes window.
         *
         * @param event ignored
         */
        public void cancelActionPerformed(ActionEvent event) {
            dispose();
        }
    }

    /**
     * Adds all friends to friends list from passed message from server.
     *
     * @param answer message from server
     * @return true on success, otherwise false.
     */
    private boolean setFriends(String answer) {
        if (answer == null) return false;
        String[] friendsList = answer.split(" ");
        if (friendsList.length < 2 || !friendsList[0].equals("FriendsList"))
            return false;
        try {
            int n = Integer.parseInt(friendsList[1]);
            if (n * 3 + 2 != friendsList.length) return false;
            for (int i = 2; i < friendsList.length; i += 3) {
                String username = friendsList[i];
                int id = Integer.parseInt(friendsList[i + 1]);
                int unreadMessages = Integer.parseInt(friendsList[i + 2]);
                friends.addElement(new Friend(id, username, unreadMessages));
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Sends server request for new messages from selected user.
     * Does nothing if no friend selected.
     */
    private void getNewMessagesFromSelected() {
        if (selectedFriend != null)
            try {
                send("GetMessageFrom " + selectedFriend.id + "\n");
            } catch (IOException e) {
                error("Error occurred while sending message: " + e.getMessage());
            }
    }

    /**
     * Processes message received from server.
     *
     * @param message message from server
     */
    private void processReceivedMessage(String message) {
        String[] buffer = message.split(" ", 2);
        try {
            String[] localBuffer;
            int friendId;
            switch (buffer[0]) {
                case "NewFriend":
                    String[] newFriend = buffer[1].split(" ");
                    friends.addElement(new Friend(Integer.parseInt(newFriend[1]), newFriend[0], 0));
                    break;
                case "RequestsList":
                    new RequestsDialog(this, out, buffer[1]);
                    break;
                case "Notification":
                    JOptionPane.showMessageDialog(this,
                            buffer[1],
                            "Notification",
                            JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "NumberOfRequests":
                    String requestsButtonName = "Friends requests";
                    if (Integer.parseInt(buffer[1]) > 0) {
                        requestsButtonName += " (" + buffer[1] + ")";
                    }
                    requestsButton.setText(requestsButtonName);
                    break;
                case "UnreadMessages":
                    localBuffer = buffer[1].split(" ", 2);
                    friendId = Integer.parseInt(localBuffer[0]);
                    if (selectedFriend != null && selectedFriend.id == friendId) {
                        getNewMessagesFromSelected();
                    }
                    friends.setUnreadMessagesToId(Integer.parseInt(localBuffer[0]), Integer.parseInt(localBuffer[1]));
                    break;
                case "NewMessage":
                    localBuffer = buffer[1].split(" ", 2);
                    friendId = Integer.parseInt(localBuffer[0]);
                    if (selectedFriend != null && selectedFriend.id == friendId) {
                        selectedFriend.messages.add(new Message(localBuffer[1], false));
                        printMsg(selectedFriend.messages.getLast());
                    } else {
                        friends.getFriendWithId(friendId).addNewMessage(localBuffer[1], false);
                    }
                    break;
            }
        } catch (NumberFormatException ignored) {
        }
    }

    /**
     * Sends passed string to server.
     *
     * @param str message for server
     * @throws IOException if any i/o error occurred.
     */
    private void send(String str) throws IOException {
        out.write(str);
        out.flush();
    }

    /**
     * Loads messages from memory to message area.
     */
    private void reloadMessages() {
        msgArea.setText(null);
        if (selectedFriend != null)
            for (Message msg : selectedFriend.messages) {
                printMsg(msg);
            }
    }

    /**
     * Prints sender's username and passed message to message area.
     *
     * @param msg message
     */
    private void printMsg(Message msg) {
        msgArea.setEditable(true);
        if (msg.myMsg) {
            appendToMsgArea(clientUsername + ": ", Color.gray);
        } else {
            appendToMsgArea(selectedFriend.username + ": ", Color.blue);
        }
        appendToMsgArea(msg.msg + "\n", Color.black);
        msgArea.setEditable(false);
    }

    /**
     * Adds passed message to message area with specified color.
     *
     * @param msg message
     * @param c   text color
     */
    private void appendToMsgArea(String msg, Color c) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Arial");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
        int len = msgArea.getDocument().getLength();
        msgArea.setCaretPosition(len);
        msgArea.setCharacterAttributes(aset, false);
        msgArea.replaceSelection(msg);
    }

    /**
     * Called when i/o error occurred, restarts connection.
     *
     * @param msg error message to display before connection restart
     */
    private void error(String msg) {
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
        JOptionPane.showMessageDialog(this,
                msg,
                "Error",
                JOptionPane.ERROR_MESSAGE);
        dispose();
        new ConnectServerDialog();
    }
}
