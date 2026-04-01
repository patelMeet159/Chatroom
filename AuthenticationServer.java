//Name = Meet Patel

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class AuthenticationServer {

    private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<>();

    public AuthenticationServer() {
        credentials.put("John", "nhoJ");
        credentials.put("Alice", "ecilA");
        credentials.put("Eve", "evE");
        credentials.put("Bob", "boB");
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println("AuthenticationServer running on port 9999...");

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

            if (in.hasNextLine()) {
                String lineIn = in.nextLine().trim();
                String[] parts = lineIn.split("~", 2);

                if (parts.length == 2 && authenticate(parts[0], parts[1])) {
                    out.println("Authenticated!");
                } else {
                    out.println("Error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean authenticate(String username, String password) {
        return credentials.containsKey(username) && credentials.get(username).equals(password);
    }

    public static void main(String[] args) {
        new AuthenticationServer().start();
    }
}