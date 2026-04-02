/**
 * Message
 *
 * Represents a chat message between two users.
 */
public class Message {
    private String message;
    private Person sender;
    private Person receiver;

    public Message(Person sender, Person receiver, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
    }

    public Message() {
    }

    public String getMessage() {
        return message;
    }

    public Person getSender() {
        return sender;
    }

    public Person getReceiver() {
        return receiver;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSender(Person sender) {
        this.sender = sender;
    }

    public void setReceiver(Person receiver) {
        this.receiver = receiver;
    }
}