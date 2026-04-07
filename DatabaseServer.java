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
 * Responsibilities:
 * - Track currently online users
 * - Learn each client's IP from socket.getInetAddress()
 * - Store each client's listening port
 * - Relay secure protocol messages between users
 *
 * Port:
 * - 9998
 *
 * Supported commands:
 * - REGISTER~username~listenPort
 * - LOGOUT~username
 * - Otherwise: a Message wire-format record
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
                return;
            }

            String line = in.nextLine().trim();
            System.out.println("Received line: " + line);

            if (line.startsWith("REGISTER~")) {
                handleRegister(line, socket, out);
            } else if (line.startsWith("LOGOUT~")) {
                handleLogout(line, out);
            } else {
                handleProtocolMessage(line, out);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRegister(String line, Socket socket, PrintWriter out) {
        String[] parts = line.split("~", 3);

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

        String clientIp = socket.getInetAddress().getHostAddress();

        Person p = new Person(username, "", clientIp, listenPort);
        activeUsers.put(username, p);

        System.out.println("Registered: " + username + " at " + clientIp + ":" + listenPort);
        System.out.println("Active users: " + activeUsers.keySet());

        out.println("Registered");
    }

    private void handleLogout(String line, PrintWriter out) {
        String[] parts = line.split("~", 2);

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

    /**
     * Routes a secure protocol message to the intended receiver.
     */
    private void handleProtocolMessage(String line, PrintWriter out) {
        try {
            Message msg = Message.fromWireString(line);

            if (msg == null) {
                out.println("MessageError");
                return;
            }

            Person receiver = activeUsers.get(msg.getReceiver());

            if (receiver == null) {
                out.println("ReceiverOffline");
                return;
            }

            boolean sent = forwardMessage(receiver, line);

            if (sent) {
                out.println("Sent");
            } else {
                out.println("DeliveryFailed");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("MessageError");
        }
    }

    /**
     * Forwards the raw protocol line to the receiver's listening socket.
     */
    private boolean forwardMessage(Person receiver, String wireLine) {
        try (
            Socket socket = new Socket(receiver.getIpAddress(), receiver.getPortNumber());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println(wireLine);

            System.out.println("Forwarded to " + receiver.getUsername() +
                    " at " + receiver.getIpAddress() + ":" + receiver.getPortNumber());

            return true;
        } catch (Exception e) {
            System.out.println("Could not deliver to " + receiver.getUsername() +
                    " at " + receiver.getIpAddress() + ":" + receiver.getPortNumber());
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        new DatabaseServer().start();
    }
}
