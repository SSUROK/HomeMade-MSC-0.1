package gui;

import core.Redo;
import net.core.SocketThread;
import net.core.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;

public class AdminGUI extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler, SocketThreadListener {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;

    private static String[] pcs_names;
    private static String[] pc_specs;
    private static boolean active = false;

    private static JPanel pcList = new JPanel();

    private final JTextField pc_name = new JTextField("PC name");
    private final JTextField os = new JTextField("PC OS");
    private final JTextField ip = new JTextField("PC IP");
    private final JTextField ramspace = new JTextField("PC ram space");
    private final JTextField drives = new JTextField("PC drives");

    private final JTextField pc_name_spec = new JTextField();
    private final JTextField os_spec = new JTextField();
    private final JTextField ip_spec = new JTextField();
    private final JTextField ramspace_spec = new JTextField();
    private final JTextField drives_spec = new JTextField();

    private final JButton btnDrives = new JButton("Drives");
    private final JButton btnConnect = new JButton("Connect");
    private final JButton btnDisconnect = new JButton("Disconnect");
    private final JTextField tfIPAddress = new JTextField("127.0.0.1");
    private final JTextField tfPort = new JTextField("8189");

    private final JPanel panelButtons = new JPanel(new GridLayout(1, 3));
    private final JPanel panelMain = new JPanel(new GridLayout(1, 3));
    private final JPanel panelNames = new JPanel(new GridLayout(5, 1));
    private final JPanel panelSpecs = new JPanel(new GridLayout(5, 1));

    private static SocketThread socketThread;
    private static Redo redo;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { // Event Dispatching Thread
                new AdminGUI();
            }
        });
    }

    public AdminGUI(){
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setSize(WIDTH, HEIGHT);
        setTitle("Admin window");

        btnDrives.addActionListener(this);
        btnDisconnect.addActionListener(this);
        btnConnect.addActionListener(this);
        panelMain.add(panelNames);
        panelMain.add(panelSpecs);
        panelMain.add(pcList);

        panelButtons.add(tfIPAddress);
        panelButtons.add(tfPort);
        panelButtons.add(btnConnect);

        pc_name.setEnabled(false);
        pc_name.setDisabledTextColor(Color.BLACK);
        os.setEnabled(false);
        os.setDisabledTextColor(Color.BLACK);
        ip.setEnabled(false);
        ip.setDisabledTextColor(Color.BLACK);
        ramspace.setEnabled(false);
        ramspace.setDisabledTextColor(Color.BLACK);
//        drives.setEnabled(false);
//        drives.setDisabledTextColor(Color.BLACK);

        pc_name_spec.setEnabled(false);
        pc_name_spec.setDisabledTextColor(Color.BLACK);
        os_spec.setEnabled(false);
        os_spec.setDisabledTextColor(Color.BLACK);
        ip_spec.setEnabled(false);
        ip_spec.setDisabledTextColor(Color.BLACK);
        ramspace_spec.setEnabled(false);
        ramspace_spec.setDisabledTextColor(Color.BLACK);
//        drives_spec.setEnabled(false);
//        drives_spec.setDisabledTextColor(Color.BLACK);

        panelNames.add(pc_name);
        panelNames.add(os);
        panelNames.add(ip);
        panelNames.add(ramspace);
//        panelNames.add(drives);

        panelSpecs.add(pc_name_spec);
        panelSpecs.add(os_spec);
        panelSpecs.add(ip_spec);
        panelSpecs.add(ramspace_spec);
//        panelSpecs.add(drives_spec);
        panelSpecs.add(btnDrives);

        add(panelButtons, BorderLayout.NORTH);
        add(panelMain, BorderLayout.CENTER);

        pcList.setVisible(false);
        setVisible(true);
    }

    private void connect() {
        try {
            System.out.println("connecting");
            Socket socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Admin", socket);
            active = true;
            System.out.println("connection ok");
            change();
        } catch (IOException exception) {
            showException(Thread.currentThread(), exception);
        }
    }

    private void disconnect(){
        active = false;
        socketThread.close();
        pcList.removeAll();
        pcList.setVisible(false);
        redo.interrupt();
        change();
    }

    private void change(){
        if(active){
            panelButtons.add(btnDisconnect);
            panelButtons.remove(btnConnect);
        } else {
            panelButtons.add(btnConnect);
            panelButtons.remove(btnDisconnect);
        }
        panelButtons.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnConnect) {
            connect();
        } else if (src == btnDisconnect) {
            disconnect();
        } else if (src == btnDrives) {
            showDrives();
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

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {

    }

    @Override
    public void onSocketStop(SocketThread thread) {
        disconnect();
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        String msg = "adminauthtorize";
//        String msg = "adminauthtorizc=";
        socketThread.sendMessage(msg);
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String msg) {
        if(msg.equals("successauth")){
            update();
        } else if(msg.contains("pcs_names")){
            msg = msg.substring(10);
            pcs_names = msg.split(";");
            notify();
        } else if (msg.contains("pc_specs")){
            msg = msg.substring(9);
            pc_specs = msg.split(";");
            panelSpecs.setVisible(false);
            fillSpecs();
        } else if (msg.contains("drives")){
            msg = msg.substring(7);
            redo.discAnalyzer(msg);
        }else{
            System.out.println("Unknown source: " + msg);
        }

    }

    private void showDrives(){

    }

    private void update(){
        redo = new Redo(pcList, "pcList", socketThread, this);
        redo.start();
    }

    public synchronized String[] getPcs_names(){
        String msg = "getPcs";
        socketThread.sendMessage(msg);
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return pcs_names;
    }

    public void setPcList(JPanel pcList){
        this.pcList = pcList;
        this.pcList.setVisible(false);
        this.pcList.setVisible(true);
    }

    private void fillSpecs(){
        for(String spec:pc_specs) {
            System.out.println(spec);
        }
        pc_name_spec.setText(pc_specs[0]);
        os_spec.setText(pc_specs[1]);
        ip_spec.setText(pc_specs[2]);
        ramspace_spec.setText(pc_specs[3]);

        panelSpecs.setVisible(true);
    }

    private void checkCritical(){

    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {

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
}
