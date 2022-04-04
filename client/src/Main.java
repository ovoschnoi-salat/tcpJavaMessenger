public class Main {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        String port = "31337";
        if (args.length > 0 && args[0] != null) {
            host = args[0];
        }
        if (args.length > 1 && args[1] != null) {
            port = args[1];
        }
        ConnectServerDialog.defaultHost = host;
        ConnectServerDialog.defaultPort = port;
        new ConnectServerDialog();
    }
}
