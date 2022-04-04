package client;

import javax.swing.AbstractListModel;
import java.util.Vector;

/**
 * List of friends.
 */
public class FriendsListModel extends AbstractListModel<String> {
    /**
     * Friends list.
     */
    private final Vector<Friend> friends = new Vector<>();

    /**
     * Initializes friends list with empty cell.
     */
    public FriendsListModel() {
        friends.add(null);
    }

    /**
     * Returns size of friends list.
     *
     * @return size of friends list.
     */
    public int getSize() {
        return friends.size();
    }

    /**
     * Returns string representation of friend for main window friends list.
     *
     * @param index index of friend
     * @return string representation of friend for main window friends list.
     */
    public String getElementAt(int index) {
        if (index == 0) return "---";
        return friends.elementAt(index).username +
                (friends.elementAt(index).unreadMessages > 0 ?
                        " (" + friends.elementAt(index).unreadMessages + ")" : "");
    }

    /**
     * Returns friend stores at passed index.
     *
     * @param index index
     * @return friend stores at passed index.
     */
    public Friend get(int index) {
        return friends.elementAt(index);
    }

    /**
     * Adds new friend to list.
     *
     * @param element new friend
     */
    public void addElement(Friend element) {
        if (!friends.contains(element)) {
            int index = friends.size();
            friends.addElement(element);
            fireIntervalAdded(this, index, index);
        }
    }

    /**
     * Sets unread messages counter to friend with passed id in list.
     *
     * @param id    friend's id
     * @param count unread messages counter
     */
    public void setUnreadMessagesToId(int id, int count) {
        for (int i = 0; i < friends.size(); i++) {
            Friend friend = friends.elementAt(i);
            if (friend != null && friend.id == id) {
                friend.unreadMessages = count;
                fireContentsChanged(this, i, i);
                return;
            }
        }
    }

    /**
     * Returns friend with specified id.
     * Returns null if no friend found.
     *
     * @param id friend's id
     * @return friend with specified id.
     */
    public Friend getFriendWithId(int id) {
        for (Friend friend : friends) {
            if (friend != null && friend.id == id) {
                return friend;
            }
        }
        return null;
    }
}
