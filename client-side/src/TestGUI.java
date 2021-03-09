import net.core.SocketThread;
import net.core.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;

public class TestGUI extends JFrame implements Thread.UncaughtExceptionHandler, ActionListener, SocketThreadListener {

    private final JPanel panel = new JPanel(new GridLayout(1, 2));
    private final JPanel connectPannnel  = new JPanel(new GridLayout(1, 3));

    private final JButton threadStart = new JButton("start");
    private final JButton threadEnd = new JButton("end");
    private final JButton btnconnect = new JButton("Connect");
    private final JTextField tfIPAddress = new JTextField("127.0.0.1");
    private final JTextField tfPort = new JTextField("8189");

    private ClientDeamon clD;

    private SocketThread socketThread;

    private TestGUI(){
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setSize(300, 100);
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
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { // Event Dispatching Thread
                new TestGUI();
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
            Socket socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Client", socket);
            System.out.println("connection ok");
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

    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {

    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {

    }
}
