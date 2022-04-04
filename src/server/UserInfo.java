package server;

import java.io.Serializable;

/**
 * Class that stores information about registered user.
 */
public class UserInfo implements Serializable {
    public int id;
    public String password;

    /**
     * Constructs user's information class with passed arguments.
     *
     * @param id       user's id
     * @param password user's password
     */
    public UserInfo(int id, String password) {
        this.id = id;
        this.password = password;
    }
}
