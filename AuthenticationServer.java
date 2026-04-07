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
 * Responsibilities:
 * - Verify username/password
 * - Bind a user's current signing public key to their authenticated login
 * - Provide a directory lookup for signing public keys
 *
 * Port:
 * - 9999
 *
 * Supported commands:
 * - AUTH~username~password~signingPublicKeyBase64
 * - GETKEY~username
 */
public class AuthenticationServer {

    // Demo credentials
    private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<>();

    // username -> Ed25519 public signing key (Base64-encoded X.509 form)
    private final ConcurrentHashMap<String, String> signingKeyDirectory = new ConcurrentHashMap<>();

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

            if (!in.hasNextLine()) {
                return;
            }

            String line = in.nextLine().trim();
            String[] parts = line.split("~", 4);

            if (parts.length == 0) {
                out.println("ERROR");
                return;
            }

            String command = parts[0];

            if ("AUTH".equalsIgnoreCase(command)) {
                handleAuth(parts, out);
            } else if ("GETKEY".equalsIgnoreCase(command)) {
                handleGetKey(parts, out);
            } else {
                out.println("ERROR");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAuth(String[] parts, PrintWriter out) {
        if (parts.length != 4) {
            out.println("ERROR");
            return;
        }

        String username = parts[1];
        String password = parts[2];
        String signingPubKeyBase64 = parts[3];

        if (authenticate(username, password)) {
            signingKeyDirectory.put(username, signingPubKeyBase64);
            System.out.println("Authenticated " + username + " and stored signing key.");
            out.println("AUTH_OK");
        } else {
            out.println("ERROR");
        }
    }

    private void handleGetKey(String[] parts, PrintWriter out) {
        if (parts.length != 2) {
            out.println("KEYERROR");
            return;
        }

        String username = parts[1];
        String key = signingKeyDirectory.get(username);

        if (key == null) {
            out.println("KEYERROR");
        } else {
            out.println("KEY~" + username + "~" + key);
        }
    }

    public boolean authenticate(String username, String password) {
        return credentials.containsKey(username) && credentials.get(username).equals(password);
    }

    public static void main(String[] args) {
        new AuthenticationServer().start();
    }
}