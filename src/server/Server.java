package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Server's main class
 */
public class Server {
    /**
     * Default server's port.
     */
    public static final int defaultServerPort = 31337;
    /**
     * Default file's path for users data.
     */
    public static String defaultUsersFileName = ".serverUsersData";
    /**
     * Server's socket.
     */
    private final ServerSocket serverSocket;
    /**
     * Container with information about registered users.
     */
    private final Map<String, UserInfo> usernames;
    /**
     * Container with users data.
     */
    private final ArrayList<User> users;
    /**
     * Thread that listens to server socket for new connections.
     */
    private Thread mainThread = null;

    /**
     * Server that waits for user to connect to server socket.
     * Creates new Listener for each new socket.
     * Loads users data from default file if presented.
     * Saves users data to default file.
     *
     * @param ss server socket that server should listen
     */
    @SuppressWarnings("unchecked")
    public Server(ServerSocket ss) {
        serverSocket = ss;
        Path path = Paths.get(defaultUsersFileName);
        Map<String, UserInfo> usernamesTmp;
        ArrayList<User> usersTmp;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            usernamesTmp = (Map<String, UserInfo>) in.readObject();
            usersTmp = (ArrayList<User>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            usernamesTmp = new HashMap<>();
            usersTmp = new ArrayList<>();
        }
        usernames = usernamesTmp;
        users = usersTmp;
    }

    /**
     * Creates thread that listens server socket.
     */
    public void run() {
        mainThread = new Thread(() -> {
            try {
                while (true) {
                    Socket newSocket = serverSocket.accept();
                    Thread thread = new Thread(new Listener(newSocket));
                    thread.setDaemon(true);
                    thread.start();
                }
            } catch (IOException e) {
                saveUsers();
                System.out.println("Server died: " + e.getMessage());
            }
        });
        mainThread.start();
    }

    /**
     * Stops thread that listens server socket.
     */
    public void stop() {
        if (mainThread != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * This class is constructed for each new socket to listen it.
     */
    private class Listener implements Runnable {
        private final Socket socket;
        private int id = -1;
        private String username;

        Listener(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {
                while (!socket.isInputShutdown()) {
                    if (acceptUser(in, out)) {
                        getUser(id).setUserLoggedIn(socket, out);
                        break;
                    }
                }
                while (!socket.isInputShutdown()) {
                    String buffer = in.readLine();
                    if (buffer == null)
                        break;
                    processReceivedMessage(buffer);
                }
            } catch (IOException ignored) {
            } finally {
                if (id != -1)
                    getUser(id).closeSocket(socket);
                System.out.println("socket closed: " + id);
            }
        }

        /**
         * Processes received message from user.
         *
         * @param msg message from user
         * @throws IOException if error occured while working with tcp connection
         */
        private void processReceivedMessage(String msg) throws IOException {
            String[] buffer = msg.split(" ", 2);
            String[] localBuffer;
            switch (buffer[0]) {
                case "GetFriendsRequestsList":
                    getUser(id).sendFriendsRequestsList();
                    break;
                case "AcceptRequest":
                    try {
                        int friendsId = Integer.parseInt(buffer[1]);
                        getUser(id).acceptFriendsRequest(friendsId);
                        getUser(friendsId).friendsRequestAccepted(id, username);
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "SendRequest":
                    try {
                        UserInfo info = getUserInfo(buffer[1]);
                        if (info != null && getUser(id).isAbleToSendRequestToUser(info.id)
                                && !buffer[1].equals(username)) {
                            getUser(info.id).addNewFriendsRequest(id, username);
                            getUser(id).notify("Friends request sent to " + buffer[1] + "\n");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "SendMessage":
                    localBuffer = buffer[1].split(" ", 2);
                    try {
                        int friendsId = Integer.parseInt(localBuffer[0]);

                        getUser(friendsId).saveMessageFrom(id, localBuffer[1]);
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "GetMessageFrom":
                    try {
                        int friendsId = Integer.parseInt(buffer[1]);
                        getUser(id).sendFirstMessageFrom(friendsId);
                    } catch (NumberFormatException ignored) {
                    }
                    break;
            }
        }

        /**
         * Returns true if user logged in or registered successfully, otherwise false.
         * Sends answer to user.
         *
         * @param in  input stream to get users messages
         * @param out output stream to send messages to user
         * @return true if user logged in or registered successfully, otherwise false.
         * @throws IOException if error occurred while working with
         */
        private boolean acceptUser(BufferedReader in, OutputStreamWriter out) throws IOException {
            String msg = in.readLine();
            if (msg == null)
                return writeResponse(out, "Error receiving request\n", false);
            String[] buffer = msg.split(" ", 3);
            if (buffer.length != 3)
                return writeResponse(out, "Wrong format\n", false);
            UserInfo user = getUserInfo(buffer[1]);
            if (buffer[0].equals("auth")) {
                if (user != null) {
                    if (user.password.equals(buffer[2])) {
                        id = user.id;
                        username = buffer[1];
                        return writeResponse(out, "Accepted\n", true);
                    }
                    return writeResponse(out, "Wrong password\n", false);
                }
                return writeResponse(out, "No such user\n", false);
            } else if (buffer[0].equals("reg")) {
                if (user == null) {
                    if (buffer[1].matches("^[a-zA-Z]+[\\w]{2,}$")) {
                        if (buffer[2].length() > 2) {
                            username = buffer[1];
                            id = addUser(username, buffer[2]);
                            return writeResponse(out, "Accepted\n", true);
                        }
                        return this.writeResponse(out, "Password should be at least 3 characters long\n",
                                false);
                    }
                    return this.writeResponse(out, "Wrong username format, should: " +
                            "start with letter, " +
                            "be at 3 characters long " +
                            "and consist only of letters, digits and underscores\n", false);
                }
                return writeResponse(out, "Username exists\n", false);
            }
            return writeResponse(out, "Wrong format\n", false);
        }

        /**
         * Writes authentication response to specified output stream and returns passed return value.
         *
         * @param out output stream
         * @param msg authentication response
         * @param r   return value
         * @return passed return value
         * @throws IOException if error occurred while writing to output stream
         */
        private boolean writeResponse(OutputStreamWriter out, String msg, boolean r) throws IOException {
            out.write(msg);
            out.flush();
            return r;
        }
    }

    /**
     * Returns UserInfo class with information about user with passed username.
     * Returns null if no user found.
     *
     * @param username username that user's information should return for
     * @return information about user or null if user not found
     */
    private UserInfo getUserInfo(String username) {
        synchronized (usernames) {
            return usernames.get(username);
        }
    }

    /**
     * Returns User with specified id.
     * Returns null if no user found.
     *
     * @param id user's id
     * @return User with specified id or null
     */
    private User getUser(int id) {
        synchronized (users) {
            return users.get(id);
        }
    }

    private int addUser(String username, String password) {
        int id;
        synchronized (users) {
            id = users.size();
            users.add(id, new User());
        }
        synchronized (usernames) {
            usernames.put(username, new UserInfo(id, password));
        }
        return id;
    }

    /**
     * Saves users information to default file
     */
    private void saveUsers() {
        Path path = Paths.get(defaultUsersFileName);
        try (ObjectOutputStream out = new ObjectOutputStream(
                Files.newOutputStream(path))) {
            synchronized (usernames) {
                out.writeObject(usernames);
            }
            synchronized (users) {
                out.writeObject(users);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }
}