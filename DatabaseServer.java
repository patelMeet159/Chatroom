import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DatabaseServer
 *
 * Purpose:
 * - Maintains a live list of logged-in / registered users
 * - Learns each client's LAN IP using socket.getInetAddress()
 * - Stores the client's chosen listening port
 * - Receives outgoing chat messages and forwards them to the receiver
 *
 * Port used:
 * - 9998
 *
 * Supported commands from clients:
 * - REGISTER~username~listenPort
 * - MESSAGE~sender~receiver~text
 * - LOGOUT~username
 */
public class DatabaseServer {

    // Stores currently active/logged-in users
    private final ConcurrentHashMap<String, Person> activeUsers = new ConcurrentHashMap<>();

    /**
     * Starts the chat coordination server.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(9998)) {
            System.out.println("DatabaseServer running on port 9998...");

            while (true) {
                Socket incoming = serverSocket.accept();

                // Handle each client in its own thread
                new Thread(() -> handleClient(incoming)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles one incoming client connection.
     */
    private void handleClient(Socket incoming) {
        try (
            Socket socket = incoming;
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            out.println("Connected");

            if (!in.hasNextLine()) {
                return;
            }

            String line = in.nextLine().trim();

            // Split into at most 4 parts so message text can safely contain "~"
            String[] parts = line.split("~", 4);

            if (parts.length == 0) {
                out.println("Error");
                return;
            }

            String command = parts[0];

            if ("REGISTER".equalsIgnoreCase(command)) {
                handleRegister(parts, socket, out);
            } else if ("MESSAGE".equalsIgnoreCase(command)) {
                handleMessage(parts, out);
            } else if ("LOGOUT".equalsIgnoreCase(command)) {
                handleLogout(parts, out);
            } else {
                out.println("UnknownCommand");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers a user as online.
     *
     * Client sends:
     * REGISTER~username~listenPort
     *
     * The server uses socket.getInetAddress() to learn the client's real IP.
     */
    private void handleRegister(String[] parts, Socket socket, PrintWriter out) {
        if (parts.length != 3) {
            out.println("RegisterError");
            return;
        }

        String username = parts[1];
        int listenPort;

        try {
            listenPort = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            out.println("RegisterError");
            return;
        }

        // Automatically learn the IP of the connecting client
        String clientIp = socket.getInetAddress().getHostAddress();

        Person p = new Person(username, "", clientIp, listenPort);
        activeUsers.put(username, p);

        System.out.println("Registered: " + username + " at " + clientIp + ":" + listenPort);
        out.println("Registered");
    }

    /**
     * Logs a user out and removes them from the active user map.
     */
    private void handleLogout(String[] parts, PrintWriter out) {
        if (parts.length != 2) {
            out.println("LogoutError");
            return;
        }

        String username = parts[1];
        activeUsers.remove(username);

        System.out.println("Logged out: " + username);
        out.println("LoggedOut");
    }

    /**
     * Handles a chat message from one user to another.
     *
     * Client sends:
     * MESSAGE~sender~receiver~text
     */
    private void handleMessage(String[] parts, PrintWriter out) {
        if (parts.length != 4) {
            out.println("MessageError");
            return;
        }

        String senderName = parts[1];
        String receiverName = parts[2];
        String text = parts[3];

        Person sender = activeUsers.get(senderName);
        Person receiver = activeUsers.get(receiverName);

        if (sender == null) {
            out.println("SenderNotRegistered");
            return;
        }

        if (receiver == null) {
            out.println("ReceiverOffline");
            return;
        }

        Message message = new Message(sender, receiver, text);

        if (sendMessage(message)) {
            out.println("Sent");
        } else {
            out.println("DeliveryFailed");
        }
    }

    /**
     * Forwards the message to the receiver's listening socket.
     */
    public boolean sendMessage(Message m) {
        try (
            Socket socket = new Socket(
                m.getReceiver().getIpAddress(),
                m.getReceiver().getPortNumber()
            );
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Format delivered to the receiving client:
            // sender~receiver~message
            out.println(
                m.getSender().getUsername() + "~" +
                m.getReceiver().getUsername() + "~" +
                m.getMessage()
            );

            System.out.println(
                "Delivered: " + m.getSender().getUsername() +
                " -> " + m.getReceiver().getUsername() +
                " : " + m.getMessage()
            );

            return true;
        } catch (Exception e) {
            System.out.println("Could not deliver to " + m.getReceiver().getUsername());
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        new DatabaseServer().start();
    }
}