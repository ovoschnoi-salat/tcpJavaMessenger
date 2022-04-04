package client;

import javax.swing.*;
import java.util.Vector;

public class FriendsListModel extends AbstractListModel<String> {
    private final Vector<Friend> delegate = new Vector<>();

    public FriendsListModel() {
        delegate.add(null);
    }

    public int getSize() {
        return delegate.size();
    }

    public String getElementAt(int index) {
        if (index == 0) return " ";
        return delegate.elementAt(index).username +
                (delegate.elementAt(index).unreadMessages > 0 ?
                        " (" + delegate.elementAt(index).unreadMessages + ")" : "");
    }

    public Friend get(int index) {
        return delegate.elementAt(index);
    }

    public void addElement(Friend element) {
        int index = delegate.size();
        delegate.addElement(element);
        fireIntervalAdded(this, index, index);
    }

    public void setUnreadMessagesToId(int id, int count) {
        for (int i = 0; i < delegate.size(); i++) {
            Friend friend = delegate.elementAt(i);
            if (friend != null && friend.id == id) {
                friend.unreadMessages = count;
                fireContentsChanged(this, i, i);
                return;
            }
        }
    }

    public void setUnreadMessagesToIndex(int index, int count) {
        if (index <= 0 || count < 0) throw new IllegalArgumentException("Index out of Bound");
        delegate.elementAt(index).unreadMessages = count;
        fireContentsChanged(this, index, index);
    }

    public Friend getFriendWithId(int id) {
        for (Friend friend : delegate) {
            if (friend != null && friend.id == id) {
                return friend;
            }
        }
        return null;
    }
}
