//Name = Meet Patel

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

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
            out.println("Connected");

            if (!in.hasNextLine()) {
                return;
            }

            String line = in.nextLine().trim();
            String[] parts = line.split("~", 5);

            if (parts.length == 0) {
                out.println("Error");
                return;
            }

            String command = parts[0];

            if ("REGISTER".equalsIgnoreCase(command)) {
                handleRegister(parts, out);
            } else if ("MESSAGE".equalsIgnoreCase(command)) {
                handleMessage(parts, out);
            } else {
                out.println("UnknownCommand");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRegister(String[] parts, PrintWriter out) {
        if (parts.length != 4) {
            out.println("RegisterError");
            return;
        }

        String username = parts[1];
        String ip = parts[2];
        int port;

        try {
            port = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            out.println("RegisterError");
            return;
        }

        Person p = new Person(username, "", ip, port);
        activeUsers.put(username, p);

        System.out.println("Registered: " + username + " at " + ip + ":" + port);
        out.println("Registered");
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

    public boolean sendMessage(Message m) {
        try (
            Socket socket = new Socket(m.getReceiver().getIpAddress(), m.getReceiver().getPortNumber());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println(m.getSender().getUsername() + "~"
                    + m.getReceiver().getUsername() + "~"
                    + m.getMessage());

            System.out.println("Delivered: " + m.getSender().getUsername()
                    + " -> " + m.getReceiver().getUsername()
                    + " : " + m.getMessage());
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