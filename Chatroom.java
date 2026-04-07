import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Chatroom
 *
 * Secure end-to-end chat client using:
 * - authenticated key directory from AuthenticationServer
 * - signed X25519 handshake
 * - AES-GCM for encrypted messages
 */
public class Chatroom extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(Chatroom.class.getName());

    private boolean auth = false;
    private Person currentUser;
    private Person selectedReceiver;
    private String serverHost;

    private ServerSocket listenerSocket;
    private Thread listenerThread;

    // Long-term signing key pair for this login session
    private KeyPair signingKeyPair;

    // One secure session per peer username
    private final Map<String, SecureSession> sessions = new ConcurrentHashMap<>();

    // Pending first message while handshake completes
    private final Map<String, String> pendingPlaintexts = new ConcurrentHashMap<>();

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public Chatroom() {
        initComponents();

        jTextArea1.setEditable(false);
        txt_msg.setText("");
        txt_serverHost.setText("localhost");

        txt_msg.addActionListener(this::btn_sendActionPerformed);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupAndLogout();
            }
        });
    }

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
        setTitle("Secure Chatroom");

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

        try {
            // Generate a long-term signing key pair for this login session
            signingKeyPair = CryptoUtils.generateSigningKeyPair();
            String signingPubKeyB64 = CryptoUtils.toBase64(signingKeyPair.getPublic().getEncoded());

            // Authenticate and upload signing public key
            try (
                Socket authSocket = new Socket(serverHost, 9999);
                InputStream inStream = authSocket.getInputStream();
                OutputStream outStream = authSocket.getOutputStream();
                Scanner in = new Scanner(inStream);
                PrintWriter out = new PrintWriter(outStream, true)
            ) {
                if (in.hasNextLine()) {
                    in.nextLine(); // Connected
                }

                out.println("AUTH~" + username + "~" + password + "~" + signingPubKeyB64);

                if (!in.hasNextLine()) {
                    JOptionPane.showMessageDialog(this, "No response from AuthenticationServer.");
                    return;
                }

                String response = in.nextLine().trim();

                if (!"AUTH_OK".equals(response)) {
                    JOptionPane.showMessageDialog(this,
                            "Either username or password is incorrect.",
                            "ERROR",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            currentUser = new Person(username, password, "", listenPort);

            startMessageListener();

            boolean registered = registerWithDatabaseServer(currentUser);
            if (!registered) {
                JOptionPane.showMessageDialog(this, "Login succeeded, but registration has failed.");
                stopListener();
                currentUser = null;
                signingKeyPair = null;
                return;
            }

            auth = true;
            selectedReceiver = new Person(jComboBox1.getSelectedItem().toString());

            appendSystemMessage("Logged in securely as " + currentUser.getUsername()
                    + " on listen port " + currentUser.getPortNumber());

            JOptionPane.showMessageDialog(this, "Login successful.");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Login failed due to an exception.");
        }
    }

    private boolean registerWithDatabaseServer(Person p) {
        try (
            Socket socket = new Socket(serverHost, 9998);
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            if (in.hasNextLine()) {
                in.nextLine(); // Connected
            }

            out.println("REGISTER~" + p.getUsername() + "~" + p.getPortNumber());

            if (in.hasNextLine()) {
                String response = in.nextLine().trim();
                return "Registered".equals(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void btn_sendActionPerformed(ActionEvent evt) {
        if (!auth || currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please log in first.");
            return;
        }

        selectedReceiver = new Person(jComboBox1.getSelectedItem().toString());
        String peer = selectedReceiver.getUsername();
        String plaintext = txt_msg.getText().trim();

        if (plaintext.isEmpty()) {
            return;
        }

        if (currentUser.getUsername().equals(peer)) {
            JOptionPane.showMessageDialog(this, "You cannot send a message to yourself.");
            return;
        }

        try {
            SecureSession session = sessions.get(peer);

            if (session == null || !session.isEstablished()) {
                logMillis("SENT", currentUser.getUsername() + " -> " + peer + " : " + plaintext);
                pendingPlaintexts.put(peer, plaintext);
                txt_msg.setText("");
                initiateHandshake(peer);
                appendSystemMessage("Starting secure key exchange with " + peer + "...");
                return;
            }

            sendEncryptedMessage(peer, plaintext);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to send secure message.");
        }
    }

    private void jComboBox1ActionPerformed(ActionEvent evt) {
        selectedReceiver = new Person(jComboBox1.getSelectedItem().toString());
    }

    private void btn_logoutActionPerformed(ActionEvent evt) {
        cleanupAndLogout();
        JOptionPane.showMessageDialog(this, "Logged out.");
    }

    /**
     * Starts the authenticated handshake as the initiator.
     */
    private void initiateHandshake(String peer) throws Exception {
        SecureSession existing = sessions.get(peer);
        if (existing != null && !existing.isEstablished()) {
            return;
        }

        SecureSession session = new SecureSession(peer);
        session.setSessionId(UUID.randomUUID().toString());

        KeyPair eph = CryptoUtils.generateEphemeralKeyPair();
        session.setMyEphemeralPrivateKey(eph.getPrivate());
        session.setMyEphemeralPublicKey(eph.getPublic());

        sessions.put(peer, session);

        String ephPubB64 = CryptoUtils.toBase64(eph.getPublic().getEncoded());

        String transcript = buildHandshakeTranscript(
                "KEY_INIT",
                currentUser.getUsername(),
                peer,
                session.getSessionId(),
                ephPubB64
        );

        byte[] signature = CryptoUtils.sign(transcript.getBytes(), signingKeyPair.getPrivate());
        String signatureB64 = CryptoUtils.toBase64(signature);

        Message msg = new Message(
                "KEY_INIT",
                currentUser.getUsername(),
                peer,
                session.getSessionId(),
                ephPubB64,
                signatureB64,
                ""
        );

        sendProtocolMessage(msg);
    }

    /**
     * Sends an already-established encrypted chat message.
     */
    private void sendEncryptedMessage(String peer, String plaintext) throws Exception {
        SecureSession session = sessions.get(peer);

        if (session == null || !session.isEstablished()) {
            throw new IllegalStateException("No secure session is established with " + peer);
        }

        String aad = buildAad(currentUser.getUsername(), peer, session.getSessionId());

        CryptoUtils.EncryptedPacket packet =
                CryptoUtils.encrypt(plaintext, session.getSendKey(), aad);

        Message msg = new Message(
                "SECMSG",
                currentUser.getUsername(),
                peer,
                session.getSessionId(),
                CryptoUtils.toBase64(packet.getNonce()),
                CryptoUtils.toBase64(packet.getCiphertext()),
                ""
        );

        boolean sent = sendProtocolMessage(msg);

        if (sent) {
            appendChatLine("Me", peer, plaintext);
            txt_msg.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Secure message could not be sent.");
        }
    }

    /**
     * Sends one protocol message through DatabaseServer.
     */
    private boolean sendProtocolMessage(Message msg) {
        try (
            Socket socket = new Socket(serverHost, 9998);
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            if (in.hasNextLine()) {
                in.nextLine(); // Connected
            }

            out.println(msg.toWireString());

            if (in.hasNextLine()) {
                String response = in.nextLine().trim();

                if ("Sent".equals(response)) {
                    return true;
                } else if ("ReceiverOffline".equals(response)) {
                    JOptionPane.showMessageDialog(this, msg.getReceiver() + " is not logged in.");
                } else if ("DeliveryFailed".equals(response)) {
                    JOptionPane.showMessageDialog(this,
                            "Message could not be delivered. Check the receiver's firewall/listen port.");
                } else {
                    JOptionPane.showMessageDialog(this, "Server returned: " + response);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to DatabaseServer.");
        }

        return false;
    }

    /**
     * Starts the background thread that listens for incoming protocol messages.
     */
    private void startMessageListener() throws Exception {
        listenerSocket = new ServerSocket(currentUser.getPortNumber());

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
                            System.out.println("Incoming protocol line: " + line);

                            Message msg = Message.fromWireString(line);
                            if (msg != null) {
                                handleIncomingProtocolMessage(msg);
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
     * Main protocol dispatcher.
     */
    private void handleIncomingProtocolMessage(Message msg) throws Exception {
        switch (msg.getType()) {
            case "KEY_INIT":
                handleKeyInit(msg);
                break;
            case "KEY_RESP":
                handleKeyResp(msg);
                break;
            case "SECMSG":
                handleSecureMessage(msg);
                break;
            default:
                appendSystemMessage("Unknown secure message type received: " + msg.getType());
        }
    }

    /**
     * Receiver-side processing of a handshake initiation.
     */
    private void handleKeyInit(Message msg) throws Exception {
        String peer = msg.getSender();
        String sessionId = msg.getSessionId();
        String peerEphemeralPubB64 = msg.getData1();
        String signatureB64 = msg.getData2();

        PublicKey peerSigningKey = fetchPeerSigningKey(peer);
        if (peerSigningKey == null) {
            appendSystemMessage("Could not retrieve signing key for " + peer);
            return;
        }

        String transcript = buildHandshakeTranscript(
                "KEY_INIT",
                msg.getSender(),
                msg.getReceiver(),
                sessionId,
                peerEphemeralPubB64
        );

        boolean valid = CryptoUtils.verify(
                transcript.getBytes(),
                CryptoUtils.fromBase64(signatureB64),
                peerSigningKey
        );

        if (!valid) {
            appendSystemMessage("Rejected KEY_INIT from " + peer + " بسبب invalid signature.");
            return;
        }

        SecureSession session = new SecureSession(peer);
        session.setSessionId(sessionId);
        session.setPeerEphemeralPublicKey(CryptoUtils.decodeX25519PublicKey(peerEphemeralPubB64));

        KeyPair myEph = CryptoUtils.generateEphemeralKeyPair();
        session.setMyEphemeralPrivateKey(myEph.getPrivate());
        session.setMyEphemeralPublicKey(myEph.getPublic());

        // Derive directional keys immediately on responder side
        deriveDirectionalSessionKeys(session, currentUser.getUsername(), peer);

        session.setEstablished(true);
        sessions.put(peer, session);

        String myEphemeralPubB64 = CryptoUtils.toBase64(myEph.getPublic().getEncoded());

        String responseTranscript = buildHandshakeTranscript(
                "KEY_RESP",
                currentUser.getUsername(),
                peer,
                sessionId,
                myEphemeralPubB64
        );

        byte[] responseSig = CryptoUtils.sign(responseTranscript.getBytes(), signingKeyPair.getPrivate());
        String responseSigB64 = CryptoUtils.toBase64(responseSig);

        Message response = new Message(
                "KEY_RESP",
                currentUser.getUsername(),
                peer,
                sessionId,
                myEphemeralPubB64,
                responseSigB64,
                ""
        );

        sendProtocolMessage(response);

        appendSystemMessage("Secure session established with " + peer);
    }

    /**
     * Initiator-side processing of handshake response.
     */
    private void handleKeyResp(Message msg) throws Exception {
        String peer = msg.getSender();
        SecureSession session = sessions.get(peer);

        if (session == null) {
            appendSystemMessage("Received KEY_RESP for unknown session from " + peer);
            return;
        }

        PublicKey peerSigningKey = fetchPeerSigningKey(peer);
        if (peerSigningKey == null) {
            appendSystemMessage("Could not retrieve signing key for " + peer);
            return;
        }

        String peerEphemeralPubB64 = msg.getData1();
        String signatureB64 = msg.getData2();

        String transcript = buildHandshakeTranscript(
                "KEY_RESP",
                msg.getSender(),
                msg.getReceiver(),
                msg.getSessionId(),
                peerEphemeralPubB64
        );

        boolean valid = CryptoUtils.verify(
                transcript.getBytes(),
                CryptoUtils.fromBase64(signatureB64),
                peerSigningKey
        );

        if (!valid) {
            appendSystemMessage("Rejected KEY_RESP from " + peer + " due to invalid signature.");
            return;
        }

        session.setPeerEphemeralPublicKey(CryptoUtils.decodeX25519PublicKey(peerEphemeralPubB64));
        deriveDirectionalSessionKeys(session, currentUser.getUsername(), peer);
        session.setEstablished(true);

        appendSystemMessage("Secure session established with " + peer);

        String pending = pendingPlaintexts.remove(peer);
        if (pending != null && !pending.isEmpty()) {
            sendEncryptedMessage(peer, pending);
        }
    }

    /**
     * Decrypts and displays a secure encrypted message.
     */
    private void handleSecureMessage(Message msg) throws Exception {
        String peer = msg.getSender();
        SecureSession session = sessions.get(peer);

        if (session == null || !session.isEstablished()) {
            appendSystemMessage("Received encrypted message from " + peer + " without an established session.");
            return;
        }

        if (!session.getSessionId().equals(msg.getSessionId())) {
            appendSystemMessage("Session ID mismatch on encrypted message from " + peer);
            return;
        }

        String aad = buildAad(msg.getSender(), msg.getReceiver(), msg.getSessionId());

        byte[] nonce = CryptoUtils.fromBase64(msg.getData1());
        byte[] ciphertext = CryptoUtils.fromBase64(msg.getData2());

        String plaintext = CryptoUtils.decrypt(
                nonce,
                ciphertext,
                session.getReceiveKey(),
                aad
        );

        

        SwingUtilities.invokeLater(() ->
                appendChatLine(msg.getSender(), msg.getReceiver(), plaintext)
        );
        logMillis("RECEIVED", msg.getSender() + " -> " + msg.getReceiver() + " : " + plaintext);
    }

    /**
     * Pulls a user's current signing public key from AuthenticationServer.
     */
    private PublicKey fetchPeerSigningKey(String username) {
        try (
            Socket socket = new Socket(serverHost, 9999);
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            if (in.hasNextLine()) {
                in.nextLine(); // Connected
            }

            out.println("GETKEY~" + username);

            if (!in.hasNextLine()) {
                return null;
            }

            String response = in.nextLine().trim();
            String[] parts = response.split("~", 3);

            if (parts.length == 3 && "KEY".equals(parts[0]) && username.equals(parts[1])) {
                return CryptoUtils.decodeEd25519PublicKey(parts[2]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Derives send/receive keys for the local user and peer.
     */
    private void deriveDirectionalSessionKeys(SecureSession session, String localUser, String peerUser) throws Exception {
        byte[] sharedSecret = CryptoUtils.deriveSharedSecret(
                session.getMyEphemeralPrivateKey(),
                session.getPeerEphemeralPublicKey()
        );

        SecretKey sendKey = CryptoUtils.deriveDirectionalKey(
                sharedSecret,
                session.getSessionId(),
                localUser + "->" + peerUser
        );

        SecretKey receiveKey = CryptoUtils.deriveDirectionalKey(
                sharedSecret,
                session.getSessionId(),
                peerUser + "->" + localUser
        );

        session.setSendKey(sendKey);
        session.setReceiveKey(receiveKey);
    }

    private String buildHandshakeTranscript(String type, String sender, String receiver,
                                            String sessionId, String ephPubB64) {
        return type + "|" + sender + "|" + receiver + "|" + sessionId + "|" + ephPubB64;
    }

    private String buildAad(String sender, String receiver, String sessionId) {
        return sender + "|" + receiver + "|" + sessionId;
    }

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
                    in.nextLine(); // Connected
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
        signingKeyPair = null;
        sessions.clear();
        pendingPlaintexts.clear();
    }

    private void appendSystemMessage(String text) {
        String timestamp = LocalTime.now().format(timeFormatter);
        jTextArea1.append("[" + timestamp + "] " + text + "\n");
    }

    private void appendChatLine(String sender, String receiver, String msg) {
        String timestamp = LocalTime.now().format(timeFormatter);
        jTextArea1.append("[" + timestamp + "] " + sender + " -> " + receiver + ": " + msg + "\n");
    }

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