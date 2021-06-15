package ru.sur.msc.gui;

import ru.sur.msc.core.ClientDeamon;
import net.core.SocketThread;
import net.core.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class ClientGUI extends JFrame implements Thread.UncaughtExceptionHandler, ActionListener, SocketThreadListener {

    private final JPanel panel = new JPanel(new GridLayout(1, 2));
    private final JPanel connectPannnel = new JPanel(new GridLayout(1, 3));

    private final JButton threadStart = new JButton("start");
    private final JButton threadEnd = new JButton("end");
    private final JButton btnconnect = new JButton("Connect");
    private final JTextField tfIPAddress = new JTextField("127.0.0.1");
    private final JTextField tfPort = new JTextField("8189");

    private final File serverIP = new File("server.txt");

    private ClientDeamon clD;

    private SocketThread socketThread;

    private TrayIcon trayIcon;
    private SystemTray tray;

    private ClientGUI() {

        System.out.println("creating instance");
        try {
            System.out.println("setting look and feel");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Unable to set LookAndFeel");
        }
        if (SystemTray.isSupported()) {
            System.out.println("system tray supported");
            tray = SystemTray.getSystemTray();

            Image image = Toolkit.getDefaultToolkit().getImage(ClientGUI.class.getResource("library/6wheel.png"));
            ActionListener exitListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("Exiting....");
                    System.exit(0);
                }
            };
            PopupMenu popup = new PopupMenu();
            MenuItem defaultItem = new MenuItem("Exit");
            defaultItem.addActionListener(exitListener);
            popup.add(defaultItem);
            defaultItem = new MenuItem("Open");
            defaultItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setExtendedState(JFrame.NORMAL);
                    setVisible(true);
                    tray.remove(trayIcon);
                }
            });
            popup.add(defaultItem);
            trayIcon = new TrayIcon(image, "SystemTray Demo", popup);
            trayIcon.setImageAutoSize(true);
        } else {
            System.out.println("system tray not supported");
        }
        addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                if (e.getNewState() == ICONIFIED) {
                    try {
                        tray.add(trayIcon);
                        setVisible(false);
                        System.out.println("added to SystemTray");
                    } catch (AWTException ex) {
                        System.out.println("unable to add to tray");
                    }
                }
            }
        });
        setIconImage(Toolkit.getDefaultToolkit().getImage(ClientGUI.class.getResource("library/6wheel.png")));

        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(300, 100);
        setLocationRelativeTo(null);
        setTitle("Client window");

        btnconnect.addActionListener(this);
        threadEnd.addActionListener(this);
        threadStart.addActionListener(this);

        panel.add(threadStart);
        panel.add(threadEnd);

        connectPannnel.add(tfIPAddress);
        connectPannnel.add(tfPort);
        connectPannnel.add(btnconnect);

        add(panel, BorderLayout.CENTER);
        add(connectPannnel, BorderLayout.NORTH);

        setResizable(false);
        setVisible(true);

        if(serverIP.exists()) {
            connect();
            if (socketThread != null) {
                try {
                    tray.add(trayIcon);
                } catch (AWTException e) {
                    e.printStackTrace();
                }
                setVisible(false);
                clD = new ClientDeamon("test", socketThread);
                clD.start();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { // Event Dispatching Thread
                new ClientGUI();
            }
        });

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == threadStart) {
            clD = new ClientDeamon("test", socketThread);
            clD.start();
        } else if (src == threadEnd) {
            clD.interrupt();
        } else if (src == btnconnect) {
            connect();
        } else {
            throw new RuntimeException("Unknown source: " + src);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        showException(t, e);
        System.exit(1);
    }

    private void showException(Thread t, Throwable e) {
        String msg;
        StackTraceElement[] ste = e.getStackTrace();
        if (ste.length == 0)
            msg = "Empty Stacktrace";
        else {
            msg = String.format("Exception in \"%s\" %s: %s\n\tat %s",
                    t.getName(), e.getClass().getCanonicalName(), e.getMessage(), ste[0]);
            JOptionPane.showMessageDialog(this, msg, "Exception", JOptionPane.ERROR_MESSAGE);
        }
        JOptionPane.showMessageDialog(null, msg, "Exception", JOptionPane.ERROR_MESSAGE);
    }

    private void connect() {
        try {
            if(serverIP.exists()){
                String ip = "";
                String port = "";
                FileInputStream fis = new FileInputStream(serverIP);
                int i;

                while((i=fis.read())!= (int)':'){
                    ip = ip + (char)i;
                    System.out.print((char)i);
                }
                while ((i=fis.read()) != -1){
                    port = port + (char)i;
                    System.out.print((char)i);
                }
                System.out.println();
                Socket socket = new Socket(ip, Integer.parseInt(port));
                socketThread = new SocketThread(this, "Client", socket);
                System.out.println("connection ok");
                fis.close();
            }else{
                Socket socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
                socketThread = new SocketThread(this, "Client", socket);
                System.out.println("connection ok");
                boolean create = serverIP.createNewFile();
                if (create) {
                    FileOutputStream fio = new FileOutputStream(serverIP);
                    fio.write(tfIPAddress.getText().getBytes(StandardCharsets.UTF_8));
                    fio.write(":".getBytes(StandardCharsets.UTF_8));
                    fio.write(tfPort.getText().getBytes(StandardCharsets.UTF_8));
                    fio.close();
                } else {
                    System.out.println("File is not created");
                }
            }
        } catch (IOException exception) {

            showException(Thread.currentThread(), exception);
        }
    }

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {

    }

    @Override
    public void onSocketStop(SocketThread thread) {

    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        try(Socket sok = new Socket()) {
            sok.connect(new InetSocketAddress("google.com", 80));
            String ip_send = sok.getLocalAddress().toString().substring(1);
            thread.sendMessage(("check " + ip_send).getBytes());
        } catch (UnknownHostException e ) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, byte[] bytemsg) {
        String msg = new String(bytemsg);
        if (msg.equals("exist")) {
            System.exit(0);
        } else if(msg.contains("ping")){
            msg = msg.substring(4);
            clD.pingHost(msg);
        } else {
            throw new RuntimeException("Unknown source: " + msg);
        }
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {

    }
}
