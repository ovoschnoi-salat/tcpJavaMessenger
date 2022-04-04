import java.util.LinkedList;

public class Friend {
    public int id;
    public String username;
    public final LinkedList<String> messages = new LinkedList<>();

    public Friend(int id, String username){
        this.id = id;
        this.username = username;
    }
}
