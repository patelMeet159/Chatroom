//Name = Meet Patel

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Chatroom extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(Chatroom.class.getName());

    private boolean auth = false;
    private Person currentUser;
    private Person selectedReceiver;
    private ServerSocket listenerSocket;
    private Thread listenerThread;

    public Chatroom() {
        initComponents();
        jTextArea1.setEditable(false);
        txt_msg.setText("");
        txt_ipAdd.setText("localhost");
    }

    private void initComponents() {

        lbl_usrName = new javax.swing.JLabel();
        txt_usrName = new javax.swing.JTextField();
        lbl_pass = new javax.swing.JLabel();
        txt_pass = new javax.swing.JTextField();
        lbl_ipAdd = new javax.swing.JLabel();
        txt_ipAdd = new javax.swing.JTextField();
        lbl_port = new javax.swing.JLabel();
        txt_port = new javax.swing.JTextField();
        btn_login = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        lbl_to = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        txt_msg = new javax.swing.JTextField();
        btn_send = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        lbl_usrName.setText("User name");
        lbl_pass.setText("Password");
        lbl_ipAdd.setText("IP Address");
        lbl_port.setText("Port Number");

        btn_login.setText("Login");
        btn_login.addActionListener(this::btn_loginActionPerformed);

        lbl_to.setText("To:");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[] { "Alice", "Bob", "Eve", "John" }));
        jComboBox1.addActionListener(this::jComboBox1ActionPerformed);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        btn_send.setText("Send");
        btn_send.addActionListener(this::btn_sendActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane1)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(txt_msg)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btn_send))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(lbl_to)
                                    .addGap(18, 18, 18)
                                    .addComponent(jComboBox1,
                                            javax.swing.GroupLayout.PREFERRED_SIZE,
                                            javax.swing.GroupLayout.DEFAULT_SIZE,
                                            javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(lbl_usrName)
                                    .addGap(12, 12, 12)
                                    .addComponent(txt_usrName,
                                            javax.swing.GroupLayout.PREFERRED_SIZE, 95,
                                            javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(lbl_pass)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(txt_pass,
                                            javax.swing.GroupLayout.PREFERRED_SIZE, 95,
                                            javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(lbl_ipAdd)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(txt_ipAdd,
                                            javax.swing.GroupLayout.PREFERRED_SIZE, 95,
                                            javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(lbl_port)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(txt_port,
                                            javax.swing.GroupLayout.PREFERRED_SIZE, 95,
                                            javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(btn_login)))
                            .addGap(0, 20, Short.MAX_VALUE)))
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
                        .addComponent(lbl_ipAdd)
                        .addComponent(txt_ipAdd, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbl_port)
                        .addComponent(txt_port, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btn_login))
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
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
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

    private void btn_loginActionPerformed(java.awt.event.ActionEvent evt) {
        if (auth) {
            JOptionPane.showMessageDialog(this, "Already logged in.");
            return;
        }

        String username = txt_usrName.getText().trim();
        String password = txt_pass.getText().trim();
        String ip = txt_ipAdd.getText().trim();
        String portText = txt_port.getText().trim();

        if (username.isEmpty() || password.isEmpty() || ip.isEmpty() || portText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill in all login fields.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port must be a number.");
            return;
        }

        try (
            Socket authSocket = new Socket(ip, 9999);
            InputStream inStream = authSocket.getInputStream();
            OutputStream outStream = authSocket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            if (in.hasNextLine()) {
                System.out.println(in.nextLine().trim());
            }

            out.println(username + "~" + password);

            if (!in.hasNextLine()) {
                JOptionPane.showMessageDialog(this, "No response from AuthenticationServer.");
                return;
            }

            String response = in.nextLine().trim();

            if (!"Authenticated!".equals(response)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Either username or password is incorrect.",
                        "ERROR",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            currentUser = new Person(username, password, ip, port);

            startMessageListener();

            boolean registered = registerWithDatabaseServer(currentUser);
            if (!registered) {
                JOptionPane.showMessageDialog(this, "Login succeeded, but registration with chat server failed.");
                return;
            }

            auth = true;
            selectedReceiver = new Person(jComboBox1.getSelectedItem().toString());

            jTextArea1.append("Logged in as " + currentUser.getUsername()
                    + " on " + currentUser.getIpAddress()
                    + ":" + currentUser.getPortNumber() + "\n");

            JOptionPane.showMessageDialog(this, "Login successful.");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to AuthenticationServer.");
        }
    }

    private boolean registerWithDatabaseServer(Person p) {
        try (
            Socket socket = new Socket("localhost", 9998);
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            if (in.hasNextLine()) {
                System.out.println(in.nextLine().trim());
            }

            out.println("REGISTER~" + p.getUsername() + "~" + p.getIpAddress() + "~" + p.getPortNumber());

            if (in.hasNextLine()) {
                String response = in.nextLine().trim();
                System.out.println("Register response: " + response);
                return "Registered".equals(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void btn_sendActionPerformed(java.awt.event.ActionEvent evt) {
        if (!auth || currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please log in first.");
            return;
        }

        selectedReceiver = new Person(jComboBox1.getSelectedItem().toString());
        String messageText = txt_msg.getText().trim();

        if (messageText.isEmpty()) {
            return;
        }

        try (
            Socket socket = new Socket("localhost", 9998);
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
        ) {
            if (in.hasNextLine()) {
                System.out.println(in.nextLine().trim());
            }

            out.println("MESSAGE~" + currentUser.getUsername()
                    + "~" + selectedReceiver.getUsername()
                    + "~" + messageText);

            String response = "";
            if (in.hasNextLine()) {
                response = in.nextLine().trim();
                System.out.println("Message response: " + response);
            }

            if ("Sent".equals(response)) {
                jTextArea1.append("Me -> " + selectedReceiver.getUsername() + ": " + messageText + "\n");
                txt_msg.setText("");
            } else if ("ReceiverOffline".equals(response)) {
                JOptionPane.showMessageDialog(this, selectedReceiver.getUsername() + " is not logged in.");
            } else {
                JOptionPane.showMessageDialog(this, "Message could not be sent.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to DatabaseServer.");
        }
    }

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {
        selectedReceiver = new Person(jComboBox1.getSelectedItem().toString());
    }

    private void startMessageListener() throws Exception {
        listenerSocket = new ServerSocket(currentUser.getPortNumber());

        listenerThread = new Thread(() -> {
            try {
                System.out.println("Listening on port " + currentUser.getPortNumber());

                while (true) {
                    Socket incoming = listenerSocket.accept();

                    try (
                        Socket socket = incoming;
                        Scanner in = new Scanner(socket.getInputStream())
                    ) {
                        while (in.hasNextLine()) {
                            String line = in.nextLine().trim();
                            String[] parts = line.split("~", 3);

                            if (parts.length == 3) {
                                String sender = parts[0];
                                String receiver = parts[1];
                                String msg = parts[2];

                                SwingUtilities.invokeLater(() ->
                                    jTextArea1.append(sender + " -> " + receiver + ": " + msg + "\n")
                                );
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
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

    private javax.swing.JButton btn_login;
    private javax.swing.JButton btn_send;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel lbl_pass;
    private javax.swing.JLabel lbl_ipAdd;
    private javax.swing.JLabel lbl_port;
    private javax.swing.JLabel lbl_to;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField txt_usrName;
    private javax.swing.JTextField txt_pass;
    private javax.swing.JTextField txt_ipAdd;
    private javax.swing.JTextField txt_port;
    private javax.swing.JTextField txt_msg;
    private javax.swing.JLabel lbl_usrName;
}