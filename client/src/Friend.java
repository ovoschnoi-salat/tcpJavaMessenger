import java.util.LinkedList;

public class Friend {
    public final int id;
    public final String username;
    public int unreadMessages;
    public final LinkedList<Message> messages = new LinkedList<>();

    public Friend(Integer id, String username, int unreadMessages) {
        this.id = id;
        this.username = username;
        this.unreadMessages = unreadMessages;
    }

    public void addNewMessage(String Message, boolean myMessage) {
        messages.add(new Message(Message, myMessage));
        unreadMessages++;
    }
}
