/**
 * Message
 *
 * Represents protocol messages exchanged between clients through the relay server.
 *
 * Types:
 * - KEY_INIT
 * - KEY_RESP
 * - SECMSG
 *
 * Wire format:
 * type~sender~receiver~sessionId~data1~data2~data3
 */
public class Message {

    private String type;
    private String sender;
    private String receiver;
    private String sessionId;
    private String data1;
    private String data2;
    private String data3;

    public Message() {
    }

    public Message(String type, String sender, String receiver,
                   String sessionId, String data1, String data2, String data3) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.sessionId = sessionId;
        this.data1 = data1;
        this.data2 = data2;
        this.data3 = data3;
    }

    public String getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getData1() {
        return data1;
    }

    public String getData2() {
        return data2;
    }

    public String getData3() {
        return data3;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setData1(String data1) {
        this.data1 = data1;
    }

    public void setData2(String data2) {
        this.data2 = data2;
    }

    public void setData3(String data3) {
        this.data3 = data3;
    }

    /**
     * Serializes the message into a line that can be sent over the socket.
     */
    public String toWireString() {
        return nullSafe(type) + "~" +
               nullSafe(sender) + "~" +
               nullSafe(receiver) + "~" +
               nullSafe(sessionId) + "~" +
               nullSafe(data1) + "~" +
               nullSafe(data2) + "~" +
               nullSafe(data3);
    }

    /**
     * Parses a message from the wire format.
     */
    public static Message fromWireString(String line) {
        String[] parts = line.split("~", 7);

        if (parts.length != 7) {
            return null;
        }

        return new Message(
                parts[0], // type
                parts[1], // sender
                parts[2], // receiver
                parts[3], // sessionId
                parts[4], // data1
                parts[5], // data2
                parts[6]  // data3
        );
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}