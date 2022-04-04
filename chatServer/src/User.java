import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {
    private OutputStreamWriter out = null;
    private Socket socket = null;
    private final Map<Integer, Friend> friends = new HashMap<>();
    private final Map<Integer, String> friendsRequests = new HashMap<>();

    public synchronized void notify(String msg) throws IOException {
        send("Notification " + msg);
    }

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
    }

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

    public synchronized boolean isAbleToSendRequestToUser(int id) {
        return !friends.containsKey(id) && !friendsRequests.containsKey(id);
    }

    public synchronized void addNewFriendsRequest(int id, String username) throws IOException {
        if (!friends.containsKey(id) && !friendsRequests.containsKey(id)) {
            friendsRequests.put(id, username);
            sendNumberOfRequests();
        }
    }

    public synchronized void acceptFriendsRequest(int id) throws IOException {
        String username = friendsRequests.get(id);
        if (username != null && !friends.containsKey(id)) {
            friendsRequests.remove(id);
            friends.put(id, new Friend(id, username));
        }
        send("NewFriend " + username + " " + id + "\n");
        sendNumberOfRequests();
    }

    public synchronized void friendsRequestAccepted(int id, String username) throws IOException {
        if (!friendsRequests.containsKey(id) && !friends.containsKey(id)) {
            friends.put(id, new Friend(id, username));
        }
        send("NewFriend " + username + " " + id + "\n");
    }

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

    public synchronized void sendNumberOfRequests() throws IOException {
        send("NumberOfRequests " + friendsRequests.size() + "\n");
    }

    public synchronized void sendMessage(int id, String msg) {
        Friend from = friends.get(id);
        if (from != null) {
            from.messages.add(msg);
            try {
                send("UnreadMessages " + from.id + " " + from.messages.size() + "\n");
            } catch (IOException ignored) {
            }
        }
    }

    public synchronized void sendFirstMessageFrom(int id) throws IOException {
        Friend from = friends.get(id);
        if (from != null && !from.messages.isEmpty()) {
            send("NewMessage " + from.id + " " + from.messages.getFirst() + "\n");
        }
    }

    public synchronized void deleteReceivedMessage(int id, int hashCode) throws IOException {
        Friend from = friends.get(id);
        if (from != null && !from.messages.isEmpty())
            if (from.messages.getFirst().hashCode() == hashCode) {
                from.messages.removeFirst();
            }
        sendFirstMessageFrom(id);
    }

    public synchronized void messageReceived(int friendsId, int hashCode) throws IOException {
        send("MessageReceived " + friendsId + " " + hashCode + "\n");
    }

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

    private void send(String msg) throws IOException {
        if (out != null) {
            out.write(msg);
            out.flush();
        }
    }
}





