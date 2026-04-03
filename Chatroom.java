import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Chatroom
 *
 * Purpose:
 * - GUI chat client
 * - Authenticates against AuthenticationServer
 * - Registers with DatabaseServer
 * - Opens a local listening socket for incoming messages
 * - Displays sent and received messages in the text area
 *
 * GUI fields:
 * - Username
 * - Password
 * - Server IP / Host
 * - Listen Port
 *
 * Notes:
 * - The Server IP / Host is the machine running AuthenticationServer and DatabaseServer
 * - The Listen Port is the local port THIS client listens on for incoming messages
 *
 * Added:
 * - Millisecond timestamps to console immediately after SEND and RECEIVE events
 */
public class Chatroom extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(Chatroom.class.getName());

    // Authentication state
    private boolean auth = false;

    // Current logged-in user
    private Person currentUser;

    // Current selected receiver from the combo box
    private Person selectedReceiver;

    // Server host entered by the user
    private String serverHost;

    // Listening socket for incoming chat messages
    private ServerSocket listenerSocket;

    // Background thread that accepts incoming messages
    private Thread listenerThread;

    // Formatter for timestamps shown in the chat area
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public Chatroom() {
        initComponents();

        // Prevent user from directly typing into the chat transcript area
        jTextArea1.setEditable(false);

        // Start with empty outgoing message box
        txt_msg.setText("");

        // Default server host for same-machine testing
        txt_serverHost.setText("localhost");

        // Pressing Enter in the message field sends the message
        txt_msg.addActionListener(this::btn_sendActionPerformed);

        // Attempt graceful logout when the window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupAndLogout();
            }
        });
    }

    /**
     * Builds the Swing GUI.
     */
    private void initComponents() {

        lbl_usrName = new javax.swing.JLabel();
        txt_usrName = new javax.swing.JTextField();
        lbl_pass = new javax.swing.JLabel();
        txt_pass = new javax.swing.JTextField();
        lbl_serverHost = new javax.swing.JLabel();
        txt_serverHost = new javax.swing.JTextField();
        lbl_port = new javax.swing.JLabel();
        txt_port = new javax.swing.JTextField();
        btn_login = new javax.swing.JButton();
        btn_logout = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        lbl_to = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        txt_msg = new javax.swing.JTextField();
        btn_send = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setTitle("Chatroom");

        lbl_usrName.setText("User name");
        lbl_pass.setText("Password");
        lbl_serverHost.setText("Server IP / Host");
        lbl_port.setText("Listen Port");

        btn_login.setText("Login");
        btn_login.addActionListener(this::btn_loginActionPerformed);

        btn_logout.setText("Logout");
        btn_logout.addActionListener(this::btn_logoutActionPerformed);

        lbl_to.setText("To:");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[] { "Alice", "Bob", "Eve", "John" }));
        jComboBox1.addActionListener(this::jComboBox1ActionPerformed);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(20);
        jScrollPane1.setViewportView(jTextArea1);

        btn_send.setText("Send");
        btn_send.addActionListener(this::btn_sendActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jSeparator1)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane1)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(txt_msg)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btn_send))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(lbl_to)
                            .addGap(12, 12, 12)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                    javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(lbl_usrName)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(txt_usrName, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
                                    javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(lbl_pass)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(txt_pass, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
                                    javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(lbl_serverHost)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(txt_serverHost, javax.swing.GroupLayout.PREFERRED_SIZE, 120,
                                    javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(lbl_port)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(txt_port, javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                    javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(btn_login)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btn_logout)
                            .addGap(0, 0, Short.MAX_VALUE)))
                    .addContainerGap())
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbl_usrName)
                        .addComponent(txt_usrName, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbl_pass)
                        .addComponent(txt_pass, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbl_serverHost)
                        .addComponent(txt_serverHost, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbl_port)
                        .addComponent(txt_port, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btn_login)
                        .addComponent(btn_logout))
                    .addGap(18, 18, 18)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbl_to)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(18, 18, 18)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                    .addGap(18, 18, 18)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txt_msg, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btn_send))
                    .addContainerGap())
        );

        pack();
    }

    /**
     * Handles login button press.
     */
    private void btn_loginActionPerformed(ActionEvent evt) {
        if (auth) {
            JOptionPane.showMessageDialog(this, "Already logged in.");
            return;
        }

        String username = txt_usrName.getText().trim();
        String password = txt_pass.getText().trim();
        serverHost = txt_serverHost.getText().trim();
        String portText = txt_port.getText().trim();

        if (username.isEmpty() || password.isEmpty() || serverHost.isEmpty() || portText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill in all login fields.");
            return;
        }

        int listenPort;
        try {
            listenPort = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Listen port must be a number.");
            return;
        }

        try (
            Socket authSocket = new Socket(serverHost, 9999);
            InputStream inStream = authSocket.getInputStream();
            OutputStream outStream = authSocket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            System.out.println("Trying to connect to AuthenticationServer at " + serverHost + ":9999");

            if (in.hasNextLine()) {
                String greeting = in.nextLine();
                System.out.println("AuthenticationServer says: " + greeting);
            }

            out.println(username + "~" + password);

            if (!in.hasNextLine()) {
                JOptionPane.showMessageDialog(this, "No response from AuthenticationServer.");
                return;
            }

            String response = in.nextLine().trim();
            System.out.println("Authentication response: " + response);

            if (!"Authenticated!".equals(response)) {
                JOptionPane.showMessageDialog(
                    this,
                    "Either username or password is incorrect.",
                    "ERROR",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            // Current user stores the listen port; server infers client IP at registration time
            currentUser = new Person(username, password, "", listenPort);

            // Start local message listener before registering with the chat server
            startMessageListener();

            boolean registered = registerWithDatabaseServer(currentUser);

            if (!registered) {
                JOptionPane.showMessageDialog(this, "Login succeeded, but registration has failed.");
                stopListener();
                currentUser = null;
                return;
            }

            auth = true;
            selectedReceiver = new Person(jComboBox1.getSelectedItem().toString());

            appendSystemMessage("Logged in as " + currentUser.getUsername()
                    + " on listen port " + currentUser.getPortNumber());

            JOptionPane.showMessageDialog(this, "Login successful.");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to AuthenticationServer.");
        }
    }

    /**
     * Registers the client with DatabaseServer.
     *
     * The server infers the client's IP using socket.getInetAddress().
     */
    private boolean registerWithDatabaseServer(Person p) {
        System.out.println("Trying to register with serverHost = " + serverHost + " on port 9998");

        try (
            Socket socket = new Socket(serverHost, 9998);
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            System.out.println("Connected to DatabaseServer.");

            if (in.hasNextLine()) {
                String greeting = in.nextLine();
                System.out.println("DatabaseServer says: " + greeting);
            } else {
                System.out.println("No greeting received from DatabaseServer.");
            }

            String registerCommand = "REGISTER~" + p.getUsername() + "~" + p.getPortNumber();
            System.out.println("Sending: " + registerCommand);
            out.println(registerCommand);

            if (in.hasNextLine()) {
                String response = in.nextLine().trim();
                System.out.println("Register response: " + response);
                return "Registered".equals(response);
            } else {
                System.out.println("No registration response received.");
            }

        } catch (Exception e) {
            System.out.println("Registration exception:");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Handles send button press or Enter key in the message field.
     */
    private void btn_sendActionPerformed(ActionEvent evt) {
        if (!auth || currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please log in first.");
            return;
        }

        selectedReceiver = new Person(jComboBox1.getSelectedItem().toString());
        String messageText = txt_msg.getText().trim();

        if (messageText.isEmpty()) {
            return;
        }

        if (currentUser.getUsername().equals(selectedReceiver.getUsername())) {
            JOptionPane.showMessageDialog(this, "You cannot send a message to yourself.");
            return;
        }

        try (
            Socket socket = new Socket(serverHost, 9998);
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            System.out.println("Trying to send message through DatabaseServer at " + serverHost + ":9998");

            if (in.hasNextLine()) {
                String greeting = in.nextLine();
                System.out.println("DatabaseServer says: " + greeting);
            }

            String sendCommand = "MESSAGE~" + currentUser.getUsername()
                    + "~" + selectedReceiver.getUsername()
                    + "~" + messageText;

            System.out.println("Sending: " + sendCommand);
            out.println(sendCommand);

            String response = "";
            if (in.hasNextLine()) {
                response = in.nextLine().trim();
                System.out.println("Message response: " + response);
            }

            if ("Sent".equals(response)) {
                // Millisecond timestamp printed immediately after successful send
                logMillis("SENT", currentUser.getUsername() + " -> "
                        + selectedReceiver.getUsername() + " : " + messageText);

                appendChatLine("Me", selectedReceiver.getUsername(), messageText);
                txt_msg.setText("");
            } else if ("ReceiverOffline".equals(response)) {
                JOptionPane.showMessageDialog(this,
                        selectedReceiver.getUsername() + " is not logged in.");
            } else if ("DeliveryFailed".equals(response)) {
                JOptionPane.showMessageDialog(this,
                        "Message could not be delivered. Check the receiver's firewall/listen port.");
            } else {
                JOptionPane.showMessageDialog(this, "Message could not be sent.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to DatabaseServer.");
        }
    }

    /**
     * Updates the selected receiver when the combo box changes.
     */
    private void jComboBox1ActionPerformed(ActionEvent evt) {
        selectedReceiver = new Person(jComboBox1.getSelectedItem().toString());
    }

    /**
     * Handles logout button press.
     */
    private void btn_logoutActionPerformed(ActionEvent evt) {
        cleanupAndLogout();
        JOptionPane.showMessageDialog(this, "Logged out.");
    }

    /**
     * Starts the background thread that listens for incoming chat messages.
     */
    private void startMessageListener() throws Exception {
        listenerSocket = new ServerSocket(currentUser.getPortNumber());
        System.out.println("Listening for incoming messages on port " + currentUser.getPortNumber());

        listenerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket incoming = listenerSocket.accept();

                    try (
                        Socket socket = incoming;
                        Scanner in = new Scanner(socket.getInputStream())
                    ) {
                        while (in.hasNextLine()) {
                            String line = in.nextLine().trim();
                            System.out.println("Incoming delivered line: " + line);

                            String[] parts = line.split("~", 3);

                            if (parts.length == 3) {
                                String sender = parts[0];
                                String receiver = parts[1];
                                String msg = parts[2];

                                // Millisecond timestamp printed immediately after receive
                                logMillis("RECEIVED", sender + " -> " + receiver + " : " + msg);

                                SwingUtilities.invokeLater(() ->
                                    appendChatLine(sender, receiver, msg)
                                );
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                if (listenerSocket != null && !listenerSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Stops the local message listener.
     */
    private void stopListener() {
        try {
            if (listenerSocket != null && !listenerSocket.isClosed()) {
                listenerSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    /**
     * Sends a logout request to DatabaseServer and resets local state.
     */
    private void cleanupAndLogout() {
        if (auth && currentUser != null) {
            try (
                Socket socket = new Socket(serverHost, 9998);
                InputStream inStream = socket.getInputStream();
                OutputStream outStream = socket.getOutputStream();
                Scanner in = new Scanner(inStream);
                PrintWriter out = new PrintWriter(outStream, true)
            ) {
                if (in.hasNextLine()) {
                    in.nextLine(); // "Connected"
                }

                out.println("LOGOUT~" + currentUser.getUsername());

                if (in.hasNextLine()) {
                    System.out.println("Logout response: " + in.nextLine().trim());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        stopListener();

        auth = false;
        currentUser = null;
        selectedReceiver = null;
    }

    /**
     * Appends a system/status line to the chat area.
     */
    private void appendSystemMessage(String text) {
        String timestamp = LocalTime.now().format(timeFormatter);
        jTextArea1.append("[" + timestamp + "] " + text + "\n");
    }

    /**
     * Appends a chat message line to the chat area.
     */
    private void appendChatLine(String sender, String receiver, String msg) {
        String timestamp = LocalTime.now().format(timeFormatter);
        jTextArea1.append("[" + timestamp + "] " + sender + " -> " + receiver + ": " + msg + "\n");
    }

    /**
     * Logs a readable timestamp with millisecond precision to the client console.
     * Example:
     * 2026-04-03 16:22:18.451 | SENT | John -> Alice : hello
     */
    private void logMillis(String eventType, String details) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        System.out.println(now.format(formatter) + " | " + eventType + " | " + details);
    }

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info
                    : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> new Chatroom().setVisible(true));
    }

    // Swing components
    private javax.swing.JButton btn_login;
    private javax.swing.JButton btn_logout;
    private javax.swing.JButton btn_send;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel lbl_pass;
    private javax.swing.JLabel lbl_port;
    private javax.swing.JLabel lbl_serverHost;
    private javax.swing.JLabel lbl_to;
    private javax.swing.JLabel lbl_usrName;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField txt_msg;
    private javax.swing.JTextField txt_pass;
    private javax.swing.JTextField txt_port;
    private javax.swing.JTextField txt_serverHost;
    private javax.swing.JTextField txt_usrName;
}