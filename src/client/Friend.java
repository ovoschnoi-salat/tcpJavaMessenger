package client;

import java.util.LinkedList;

/**
 * Class that represents user's friend for client.
 */
public class Friend {
    /**
     * Friend's id.
     */
    public final int id;
    /**
     * Friend's username.
     */
    public final String username;
    /**
     * Unread messages counter.
     */
    public int unreadMessages;
    /**
     * Messages list.
     */
    public final LinkedList<Message> messages = new LinkedList<>();

    /**
     * Crates new friend with specified id, username and unread messages counter.
     *
     * @param id             friend's id
     * @param username       friend's username
     * @param unreadMessages unread messages counter
     */
    public Friend(Integer id, String username, int unreadMessages) {
        this.id = id;
        this.username = username;
        this.unreadMessages = unreadMessages;
    }

    /**
     * Adds new message to messages list.
     *
     * @param Message   new message
     * @param myMessage client's message flag
     */
    public void addNewMessage(String Message, boolean myMessage) {
        messages.add(new Message(Message, myMessage));
        unreadMessages++;
    }
}
