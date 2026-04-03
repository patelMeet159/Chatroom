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

    private final ConcurrentHashMap<String, Person> activeUsers = new ConcurrentHashMap<>();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(9998)) {
            System.out.println("DatabaseServer running on port 9998...");

            while (true) {
                Socket incoming = serverSocket.accept();
                new Thread(() -> handleClient(incoming)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket incoming) {
        try (
            Socket socket = incoming;
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            System.out.println("Connection received from: " + socket.getInetAddress().getHostAddress());

            out.println("Connected");

            if (!in.hasNextLine()) {
                System.out.println("Client connected but sent no data.");
                return;
            }

            String line = in.nextLine().trim();
            System.out.println("Received line: [" + line + "]");

            String[] parts = line.split("~", 4);

            System.out.println("parts.length = " + parts.length);
            for (int i = 0; i < parts.length; i++) {
                System.out.println("parts[" + i + "] = [" + parts[i] + "]");
            }

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

    private void handleRegister(String[] parts, Socket socket, PrintWriter out) {
        System.out.println("Inside handleRegister...");
        System.out.println("Expected 3 parts for REGISTER");

        if (parts.length != 3) {
            System.out.println("RegisterError because parts.length was " + parts.length);
            out.println("RegisterError");
            return;
        }

        String username = parts[1];
        int listenPort;

        try {
            listenPort = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            System.out.println("RegisterError because listen port was not a valid integer: " + parts[2]);
            out.println("RegisterError");
            return;
        }

        String clientIp = socket.getInetAddress().getHostAddress();

        Person p = new Person(username, "", clientIp, listenPort);
        activeUsers.put(username, p);

        System.out.println("Registered: " + username + " at " + clientIp + ":" + listenPort);
        System.out.println("Active users: " + activeUsers.keySet());

        out.println("Registered");
    }

    private void handleLogout(String[] parts, PrintWriter out) {
        if (parts.length != 2) {
            out.println("LogoutError");
            return;
        }

        String username = parts[1];
        activeUsers.remove(username);

        System.out.println("Logged out: " + username);
        System.out.println("Active users: " + activeUsers.keySet());

        out.println("LoggedOut");
    }

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
            System.out.println("Sender not registered: " + senderName);
            out.println("SenderNotRegistered");
            return;
        }

        if (receiver == null) {
            System.out.println("Receiver offline or not registered: " + receiverName);
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

    public boolean sendMessage(Message m) {
        try (
            Socket socket = new Socket(
                m.getReceiver().getIpAddress(),
                m.getReceiver().getPortNumber()
            );
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
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
            System.out.println(
                "Could not deliver to " + m.getReceiver().getUsername() +
                " at " + m.getReceiver().getIpAddress() +
                ":" + m.getReceiver().getPortNumber()
            );
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        new DatabaseServer().start();
    }
}