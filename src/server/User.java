package server;

import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * User representation class.
 */
public class User implements Serializable {
    /**
     * Output stream to user.
     */
    private transient OutputStreamWriter out = null;
    /**
     * User's socket.
     */
    private transient Socket socket = null;
    /**
     * User's friends list.
     */
    private final Map<Integer, Friend> friends = new HashMap<>();
    /**
     * User's friends requests.
     */
    private final Map<Integer, String> friendsRequests = new HashMap<>();

    /**
     * Sends notification to user if user is connected.
     *
     * @param msg notification message
     * @throws IOException if i/o error occurred while sending notification.
     */
    public synchronized void notify(String msg) throws IOException {
        send("Notification " + msg);
    }

    /**
     * Sets user connected to server and sends friends list answer.
     *
     * @param newSocket user's socket
     * @param out       user's output stream
     * @throws IOException if i/o error occurred while sending answer.
     */
    public synchronized void setUserLoggedIn(Socket newSocket, OutputStreamWriter out) throws IOException {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        socket = newSocket;
        this.out = out;
        sendFriendsList();
        sendNumberOfRequests();
    }

    /**
     * Closes user's connection.
     *
     * @param socket user's socket
     */
    public synchronized void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        if (this.socket == socket) {
            this.socket = null;
            out = null;
        }
    }

    /**
     * Returns true if user is able to send friends request to specified user.
     *
     * @param id user's id
     * @return true if user is able to send friends request to specified user, otherwise false.
     */
    public synchronized boolean isAbleToSendRequestToUser(int id) {
        return !friends.containsKey(id) && !friendsRequests.containsKey(id);
    }

    /**
     * Adds new friends request from specified user.
     * Notifies user about new request if user is connected.
     *
     * @param id       sender's id
     * @param username sender's username
     * @throws IOException if i/o error occurred while notifying user.
     */
    public synchronized void addNewFriendsRequest(int id, String username) throws IOException {
        if (isAbleToSendRequestToUser(id)) {
            friendsRequests.put(id, username);
            sendNumberOfRequests();
        }
    }

    /**
     * Accepts sender's friends request that were sent before.
     * Notifies user about new friend if user is connected.
     *
     * @param id sender's id
     * @throws IOException if i/o error occurred while notifying user.
     */
    public synchronized void acceptFriendsRequest(int id) throws IOException {
        String username = friendsRequests.get(id);
        if (username != null && !friends.containsKey(id)) {
            friendsRequests.remove(id);
            friends.put(id, new Friend(id, username));
        }
        send("NewFriend " + username + " " + id + "\n");
        sendNumberOfRequests();
    }

    /**
     * Adds to friends user who accepted the sent request.
     * Notifies user about new friend if user is connected.
     *
     * @param id       new friend's id
     * @param username new friend's username
     * @throws IOException if i/o error occurred while notifying user.
     */
    public synchronized void friendsRequestAccepted(int id, String username) throws IOException {
        if (!friendsRequests.containsKey(id) && !friends.containsKey(id)) {
            friends.put(id, new Friend(id, username));
        }
        send("NewFriend " + username + " " + id + "\n");
    }

    /**
     * Sends friends request list to user.
     *
     * @throws IOException if i/o error occurred while sending message to user.
     */
    public synchronized void sendFriendsRequestsList() throws IOException {
        StringBuilder sb = new StringBuilder("RequestsList ");
        sb.append(friendsRequests.size());
        friendsRequests.forEach((id, username) -> {
            sb.append(" ");
            sb.append(username);
            sb.append(" ");
            sb.append(id);
        });
        sb.append("\n");
        send(sb.toString());
    }

    /**
     * Sends number of friends requests to user.
     *
     * @throws IOException if i/o error occurred while sending message to user.
     */
    public synchronized void sendNumberOfRequests() throws IOException {
        send("NumberOfRequests " + friendsRequests.size() + "\n");
    }

    /**
     * Saves passed message received from user with specified id.
     *
     * @param id  sender's id
     * @param msg message
     */
    public synchronized void saveMessageFrom(int id, String msg) {
        Friend from = friends.get(id);
        if (from != null) {
            from.messages.add(msg);
            try {
                send("UnreadMessages " + from.id + " " + from.messages.size() + "\n");
                sendUnreadMessagesCount(from);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Sends first message from specified friend to user.
     *
     * @param id friend's id
     * @throws IOException if i/o error occurred while sending message to user.
     */
    public synchronized void sendFirstMessageFrom(int id) throws IOException {
        Friend from = friends.get(id);
        if (from != null && !from.messages.isEmpty()) {
            send("NewMessage " + from.id + " " + from.messages.getFirst() + "\n");
            from.messages.removeFirst();
            sendUnreadMessagesCount(from);
        }
    }

    /**
     * Sends number of unread messages from specified friend.
     *
     * @param from friend
     */
    private void sendUnreadMessagesCount(Friend from) {
        try {
            send("UnreadMessages " + from.id + " " + from.messages.size() + "\n");
        } catch (IOException ignored) {
        }
    }

    /**
     * Sends friends list to user.
     *
     * @throws IOException if i/o error occurred while sending message to user.
     */
    private void sendFriendsList() throws IOException {
        StringBuilder sb = new StringBuilder("FriendsList ");
        sb.append(friends.size());
        friends.forEach((id, friend) -> {
            sb.append(" ");
            sb.append(friend.username);
            sb.append(" ");
            sb.append(friend.id);
            sb.append(" ");
            sb.append(friend.messages.size());
        });
        sb.append("\n");
        send(sb.toString());
    }

    /**
     * Sends passed message to user if user is connected.
     *
     * @param msg message
     * @throws IOException if i/o error occurred while sending message to user.
     */
    private void send(String msg) throws IOException {
        if (out != null) {
            out.write(msg);
            out.flush();
        }
    }
}





