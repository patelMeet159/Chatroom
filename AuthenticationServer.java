import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuthenticationServer
 *
 * Purpose:
 * - Accepts login requests from clients
 * - Verifies username/password pairs
 * - Returns either "Authenticated!" or "Error"
 *
 * Port used:
 * - 9999
 */
public class AuthenticationServer {

    // Stores valid username/password combinations
    private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<>();

    public AuthenticationServer() {
        // Demo credentials
        credentials.put("John", "nhoJ");
        credentials.put("Alice", "ecilA");
        credentials.put("Eve", "evE");
        credentials.put("Bob", "boB");
    }

    /**
     * Starts the authentication server.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println("AuthenticationServer running on port 9999...");

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
     * Handles one authentication client connection.
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

            if (in.hasNextLine()) {
                String lineIn = in.nextLine().trim();

                // Expected format: username~password
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

    /**
     * Verifies the username/password pair.
     */
    public boolean authenticate(String username, String password) {
        return credentials.containsKey(username) && credentials.get(username).equals(password);
    }

    public static void main(String[] args) {
        new AuthenticationServer().start();
    }
}