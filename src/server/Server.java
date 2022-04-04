package server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private final ServerSocket serverSocket;
    private final Map<String, UserInfo> usernames = new HashMap<>();
    private final ArrayList<User> users = new ArrayList<>();

    public static final int defaultServerPort = 31337;

    private Thread mainThread = null;

    public Server(ServerSocket ss) {
        serverSocket = ss;
    }

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
                System.out.println("Server died: " + e.getMessage());
            }
        });
        mainThread.start();
    }

    public void stop() {
        if (mainThread != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

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
                        if (info != null && getUser(id).isAbleToSendRequestToUser(info.id)) {
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

                        getUser(friendsId).sendMessage(id, localBuffer[1]);
                        getUser(id).messageReceived(friendsId, localBuffer[1].hashCode());
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
                case "MessageReceived":
                    localBuffer = buffer[1].split(" ", 2);
                    try {
                        int friendsId = Integer.parseInt(localBuffer[0]);
                        getUser(id).deleteReceivedMessage(friendsId, Integer.parseInt(localBuffer[1]));

                    } catch (NumberFormatException ignored) {
                    }
                    break;
            }
        }

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
                            synchronized (users) {
                                id = users.size();
                                usernames.put(buffer[1], new UserInfo(id, buffer[2]));
                                users.add(id, new User());
                            }
                            return writeResponse(out, "Accepted\n", true);
                        }
                        return this.writeResponse(out, "Password should be at least 3 characters long\n", false);
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

        private boolean writeResponse(OutputStreamWriter out, String msg, boolean r) throws IOException {
            out.write(msg);
            out.flush();
            return r;
        }
    }

    private UserInfo getUserInfo(String username) {
        synchronized (usernames) {
            return usernames.get(username);
        }
    }

    private User getUser(int id) {
        synchronized (users) {
            return users.get(id);
        }
    }
}