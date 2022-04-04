package client;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

public class Client extends JFrame {
    private final JTextPane msgArea = new JTextPane();
    private final JTextField textField = new JTextField("Your message...");
    JButton requestsButton = new JButton("Friends requests");
    private final FriendsListModel friends = new FriendsListModel();
    private final JList<String> friendsList = new JList<>(friends);
    private Friend selectedFriend = null;
    private final String clientUsername;
    private final Socket clientSocket;
    private OutputStreamWriter out = null;

    private String outputMessageBuffer = null;
    private Integer receiverId = null;

    Client(Socket socket, String username, String FriendsListString) {
        super("Messenger");
        clientSocket = socket;
        clientUsername = username;

        msgArea.setEditable(false);
        JScrollPane msgAreaScrollPane = new JScrollPane(
                msgArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(this::sendActionPerformed);
        requestsButton.addActionListener(this::requestsActionPerformed);
        JButton addFriendButton = new JButton("Add new friend");
        addFriendButton.addActionListener(this::sendRequestActionPerformed);
        //
        friendsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane friendsScrollPane = new JScrollPane(friendsList);
        //
        friendsList.addListSelectionListener(this::newFriendSelected);
        // setting layout for main window
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
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        });
        if (setFriends(FriendsListString)) {
            try {
                out = new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8);
                setVisible(true);
                Thread thread = new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
                                StandardCharsets.UTF_8));
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
                    } catch (IOException ignored) {
                    } finally {
                        SwingUtilities.invokeLater(() -> error("Connection closed"));
                    }
                });
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
                error("Connection closed");
            }
        } else {
            error("Illegal answer from server received");
        }
    }

    public void newFriendSelected(ListSelectionEvent e) {
        int index = friendsList.getSelectedIndex();
        Friend newSelection = friends.get(index);
        if (selectedFriend != newSelection) {
            selectedFriend = newSelection;
            reloadMessages();
        }
        checkNewMessages();
        if (index > 0)
            friends.setUnreadMessagesToIndex(index, 0);
    }

    public void requestsActionPerformed(ActionEvent event) {
        try {
            out.write("GetFriendsRequestsList\n");
            out.flush();
        } catch (IOException e) {
            error("Error sending request to server: " + e.getMessage());
        }
    }

    public void sendRequestActionPerformed(ActionEvent event) {
        new SendFriendsRequestDialog(this, out);
    }

    private class RequestsDialog extends JDialog {
        private JComboBox<String> comboBox = null;
        private final OutputStreamWriter out;
        private final Vector<String> requestsIds = new Vector<>();

        public RequestsDialog(Frame parent, OutputStreamWriter out, String msg) {
            super(parent, "Friend requests", true);
            this.out = out;
            // setting accept request button action
            JButton acceptButton = new JButton("Accept");
            acceptButton.addActionListener(this::acceptActionPerformed);
            // setting cancel button action
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(this::cancelActionPerformed);
            String[] buffer = msg.split(" ");
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
                setVisible(true);
            } catch (IllegalArgumentException e) {
                dispose();
            }
        }

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

    private class SendFriendsRequestDialog extends JDialog {
        private final OutputStreamWriter out;
        private final JTextField usernameField = new JTextField(16);

        public SendFriendsRequestDialog(Frame parent, OutputStreamWriter out) {
            super(parent, "Send new friends request", true);
            this.out = out;

            JButton sendButton = new JButton("Send request");
            sendButton.addActionListener(this::sendRequestActionPerformed);
            // setting exit button action for server authentication dialog
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
            setVisible(true);
        }

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

        public void cancelActionPerformed(ActionEvent event) {
            dispose();
        }
    }

    private void checkNewMessages() {
        if (selectedFriend != null)
            try {
                send("GetMessageFrom " + selectedFriend.id + "\n");
            } catch (IOException e) {
                error("Error occurred while sending message: " + e.getMessage());
            }
    }

    private void processReceivedMessage(String message) {
        String[] buffer = message.split(" ", 2);
        try {
            switch (buffer[0]) {
                case "NewFriend" -> {
                    String[] newFriend = buffer[1].split(" ");
                    friends.addElement(new Friend(Integer.parseInt(newFriend[1]), newFriend[0], 0));
                }
                case "RequestsList" -> new RequestsDialog(this, out, buffer[1]);
                case "Notification" -> JOptionPane.showMessageDialog(this,
                        buffer[1],
                        "Notification",
                        JOptionPane.INFORMATION_MESSAGE);
                case "NumberOfRequests" -> {
                    String requestsButtonName = "Friends requests";
                    if (Integer.parseInt(buffer[1]) > 0) {
                        requestsButtonName += " (" + buffer[1] + ")";
                    }
                    requestsButton.setText(requestsButtonName);
                }
                case "UnreadMessages" -> {
                    String[] localBuffer = buffer[1].split(" ", 2);
                    int friendId = Integer.parseInt(localBuffer[0]);
                    if (selectedFriend != null && selectedFriend.id == friendId) {
                        checkNewMessages();
                    } else
                        friends.setUnreadMessagesToId(Integer.parseInt(localBuffer[0]), Integer.parseInt(localBuffer[1]));
                }
                case "NewMessage" -> {
                    String[] localBuffer = buffer[1].split(" ", 2);
                    int friendId = Integer.parseInt(localBuffer[0]);
                    Friend friend = selectedFriend;
                    if (friend == null || friend.id != friendId) {
                        friend = friends.getFriendWithId(friendId);
                    }
                    if (friend != null) {
                        friend.messages.add(new Message(localBuffer[1], false));
                        if (selectedFriend == friend)
                            printMsg(selectedFriend.messages.getLast());
                        send("MessageReceived " + friend.id + " " + localBuffer[1].hashCode() + "\n");
                    }
                }
                case "MessageReceived" -> {
                    String[] localBuffer = buffer[1].split(" ", 2);
                    if (Integer.parseInt(localBuffer[0]) == receiverId &&
                            Integer.parseInt(localBuffer[1]) == outputMessageBuffer.hashCode()) {
                        if (selectedFriend != null && selectedFriend.id == receiverId) {
                            selectedFriend.messages.add(new Message(outputMessageBuffer, true));
                            printMsg(selectedFriend.messages.getLast());
                        } else {
                            Friend friend = friends.getFriendWithId(receiverId);
                            if (friend != null) {
                                friend.addNewMessage(outputMessageBuffer, true);
                            }
                        }
                        outputMessageBuffer = null;
                        receiverId = null;
                        textField.setText(null);
                    }
                }
            }
        } catch (IOException e) {
            error("Error while sending message: " + e.getMessage());
        } catch (NumberFormatException ignored) {

        }
    }

    private void send(String str) throws IOException {
        out.write(str);
        out.flush();
    }

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
                addUserToList(id, username, unreadMessages);
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private void addUserToList(Integer id, String username, int unreadMessages) {
        friends.addElement(new Friend(id, username, unreadMessages));
    }

    private void reloadMessages() {
        msgArea.setText(null);
        if (selectedFriend != null)
            for (Message msg : selectedFriend.messages) {
                printMsg(msg);
            }
    }

    private void printMsg(Message msg) {
        msgArea.setEditable(true);
        if (msg.myMsg) {
            appendToMsgArea(clientUsername + ": ", Color.magenta);
        } else {
            appendToMsgArea(selectedFriend.username + ": ", Color.green);
        }
        appendToMsgArea(msg.msg + "\n", Color.black);
        msgArea.setEditable(false);
    }

    private void appendToMsgArea(String msg, Color c) {

        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
        int len = msgArea.getDocument().getLength();
        msgArea.setCaretPosition(len);
        msgArea.setCharacterAttributes(aset, false);
        msgArea.replaceSelection(msg);
    }

    public void sendActionPerformed(ActionEvent event) {
        if (textField.getText().isBlank() || selectedFriend == null) return;
        try {
            outputMessageBuffer = textField.getText().trim();
            receiverId = selectedFriend.id;
            send("SendMessage " + selectedFriend.id + " " + outputMessageBuffer + "\n");
        } catch (IOException e) {
            error("Error occurred while sending message: " + e.getMessage());
        }
    }

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
