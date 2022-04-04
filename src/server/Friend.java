package server;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Class that represents user's friend for server.
 */
public class Friend implements Serializable {
    /**
     * Friend's id.
     */
    public int id;
    /**
     * Friend's username.
     */
    public String username;
    /**
     * Messages from friend.
     */
    public final LinkedList<String> messages = new LinkedList<>();

    /**
     * Constructs new friend with specified username and id.
     *
     * @param id       friend's id
     * @param username friend's username
     */
    public Friend(int id, String username) {
        this.id = id;
        this.username = username;
    }
}
