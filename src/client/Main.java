package client;

/**
 * Class that starts application.
 */
public class Main {
    /**
     * Starts application with optionally specified default host and port.
     * Expects launching with 0, 1 or 2 arguments.
     * First argument should contain default host name.
     * Second argument should contain default server port.
     *
     * @param args server's host and port
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0] != null) {
            ConnectServerDialog.defaultHost = args[0];
        }
        if (args.length > 1 && args[1] != null) {
            ConnectServerDialog.defaultPort = args[1];
        }
        new ConnectServerDialog();
    }
}
