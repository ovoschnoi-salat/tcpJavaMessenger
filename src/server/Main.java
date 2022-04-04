package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) {
        try {
            int port = Server.defaultServerPort;
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
            Server server = new Server(new ServerSocket(port));
            server.run();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                if (in.readLine().equals("stop"))
                    break;
            }
            server.stop();
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Unable to start Server: " +
                    e.getMessage());
        }
    }
}
