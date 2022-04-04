package client;

/**
 * Message representaion class for client.
 */
public class Message {
    /**
     * Flag that indicates that message was sent from client.
     */
    boolean myMsg;
    /**
     * String with message.
     */
    String msg;

    /**
     * Constructs new message with specified message and flag.
     *
     * @param message   message
     * @param myMessage client's message flag
     */
    public Message(String message, boolean myMessage) {
        msg = message;
        myMsg = myMessage;
    }
}
