package gui;

import core.Redo;
import net.core.SocketThread;
import net.core.SocketThreadListener;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AdminGUI extends JFrame implements ListSelectionListener, ActionListener, Thread.UncaughtExceptionHandler, SocketThreadListener {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;

    private static CardLayout cardLayout = new CardLayout();
    private static JPanel view = new JPanel(cardLayout);
    private static final DefaultViewController controller = new DefaultViewController(view, cardLayout);

    private static List<String> defectivePCs = new ArrayList<>();
    private static String[] pcs_names;
    private static List<String> pc_specs;

    private static JMenuItem drivesMenu = new JMenuItem("Drives");
    private static boolean active = false;
    private static String chsnPC;
    private static Integer askTime = 10000;
    private static String availableSpace = "";
    private static JLabel label = new JLabel("Available Space. Click on disk to display");
    private static JTextField criticalSize = new JTextField();

    private static DefaultListModel<String> dlm = new DefaultListModel<>();
    private static final JList<String> pcList = new JList<>(dlm);
    private static final JScrollPane scrollListOfPCs = new JScrollPane(pcList);

    private static DefaultListModel<String> drives = new DefaultListModel<>();
    private static final JList<String> driveList = new JList<>(drives);
    private static final JScrollPane scrollListOfDrives = new JScrollPane(driveList);

    private final JTextField pc_name = new JTextField("PC name");
    private final JTextField os = new JTextField("PC OS");
    private final JTextField ip = new JTextField("PC IP");
    private final JTextField ramspace = new JTextField("PC ram space");
    private final JTextField url = new JTextField();

    private final JButton btnCheckHost = new JButton("Check");
    private final JButton btnConnect = new JButton("Connect");
    private final JButton btnDisconnect = new JButton("Disconnect");
    private final JTextField tfIPAddress = new JTextField("127.0.0.1");
    private final JTextField tfPort = new JTextField("8189");

    private final JMenuBar menuBar = new JMenuBar();
    private static JPanel ledPanel = new JPanel(new GridLayout(1, 2));
    private final JPanel panelButtons = new JPanel(new GridLayout(1, 3));
    private final JPanel panelNames = new JPanel(new GridLayout(0, 1));
    private final JPanel panelSpecs = new JPanel(new GridLayout(0, 1));
    private final JScrollPane namesScroll = new JScrollPane(panelNames);
    private final JScrollPane specScroll = new JScrollPane(panelSpecs);

    private static SocketThread socketThread;
    private static Redo redo;

    private static Map<String, Map<String, List<Integer>>> discList;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { // Event Dispatching Thread
                new AdminGUI();
            }
        });
    }

    public AdminGUI(){
        try {
            System.out.println("setting look and feel");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Unable to set LookAndFeel");
        }

        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setLocationRelativeTo(null);
        setTitle("Admin window");
        controller.addView(createHome(), DefaultViewController.HOME);
        controller.addView(createDrives(), "drives");
        controller.addView(createCheckHost(), "checkHost");

        controller.goHome();

        btnCheckHost.addActionListener(this);
        btnDisconnect.addActionListener(this);
        btnConnect.addActionListener(this);
        drivesMenu.setEnabled(false);

        menuBar.add(settings());
        menuBar.add(services());

        pcList.setCellRenderer(new MyListCellRenderer(defectivePCs));
        pcList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pcList.addListSelectionListener(this);
        pcList.setFixedCellHeight(25);

        driveList.setCellRenderer(new MyListCellRenderer());
        driveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        driveList.addListSelectionListener(this);
        driveList.setFixedCellHeight(25);
        driveList.setFixedCellWidth(100);

        panelButtons.add(tfIPAddress);
        panelButtons.add(tfPort);
        panelButtons.add(btnConnect);

        createPanelName();

        setJMenuBar(menuBar);
        add(panelButtons, BorderLayout.NORTH);

        add(view, BorderLayout.CENTER);

        setVisible(true);
    }

    private JPanel createHome() {

        JPanel panel = new JPanel(new GridLayout(1, 3));
        panel.add(namesScroll);
        panel.add(specScroll);
        panel.add(scrollListOfPCs);
        return panel;

    }

    private JPanel createDrives(){

        JPanel panel = new JPanel();
        JPanel panelmini = new JPanel(new GridLayout(2, 1, 10, 60));
        JPanel panelSetSize = new JPanel(new GridLayout(2, 1, 10, 10));

        panel.add(scrollListOfDrives, BorderLayout.WEST);
        panelSetSize.add(new JLabel("Critical size:"));
        criticalSize.addActionListener(this);
        panelSetSize.add(criticalSize);
        panelmini.add(label);
        panelmini.add(panelSetSize);
        panel.add(panelmini, BorderLayout.EAST);

        return panel;
    }

    private JPanel createCheckHost(){

        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 20));
        panel.add(new JLabel("Check host by URL"));
        panel.add(url);
        url.setPreferredSize(new Dimension(0, 30));
        url.addActionListener(this);
        ledPanel.add(btnCheckHost);
        JPanel contentPane = new JPanel(){
            Graphics2D g2;

            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                g2=(Graphics2D)g;
                g2.setColor(Color.black);
                g2.drawRoundRect(20, 20, 21, 21, 15, 15);
            }
        };
        ledPanel.add(contentPane);
        panel.add(ledPanel);

        return panel;
    }

    private void createPanelName(){
        pc_name.setEnabled(false);
        pc_name.setDisabledTextColor(Color.BLACK);
        os.setEnabled(false);
        os.setDisabledTextColor(Color.BLACK);
        ip.setEnabled(false);
        ip.setDisabledTextColor(Color.BLACK);
        ramspace.setEnabled(false);
        ramspace.setDisabledTextColor(Color.BLACK);

        panelNames.add(pc_name);
        panelNames.add(os);
        panelNames.add(ip);
        panelNames.add(ramspace);
        panelNames.repaint();
    }

    private void connect() {
        try {
            System.out.println("connecting");
            Socket socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Admin", socket);
            active = true;
            System.out.println("connection ok");
            change();
            validate();
        } catch (IOException exception) {
            showException(Thread.currentThread(), exception);
        }
    }

    private void disconnect(){
        active = false;
        socketThread.close();
        dlm.removeAllElements();
        defectivePCs.clear();
        validate();
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
        } else if (src == criticalSize) {
            setCritical();
        } else if (src == btnCheckHost) {
            if(!chsnPC.equals("") && !url.getText().equals("")) {
                System.out.println(pc_specs.get(2));
                String ip = pc_specs.get(2);
                socketThread.sendMessage(
                        ("checkHost"+ip+";"+url.getText()).getBytes(StandardCharsets.UTF_8));
            }
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
        socketThread.sendMessage(msg.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, byte[] bytemsg){
        String msg = new String(bytemsg);
        if(msg.equals("successauth")){
            update();
        } else if(msg.contains("pcs_names")){
            msg = msg.substring(10);
            pcs_names = msg.split(";");
            notify();
        } else if (msg.contains("pc_specs")) {
            msg = msg.substring(9);
            pc_specs = Arrays.asList(msg.split(";"));
            if (!pc_specs.get(0).equals(""))
                fillSpecs();
        } else if(msg.equals("true") || msg.equals("false")){
            ledSetter(msg);
        }else{
            try {
                // Parse byte array to Map
                ByteArrayInputStream byteIn = new ByteArrayInputStream(bytemsg);
                ObjectInputStream in = new ObjectInputStream(byteIn);
                discList = (Map<String, Map<String, List<Integer>>>) in.readObject();
                redo.discAnalyzer(discList);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }

    }


    private void showDrives(){
        Map<String, List<Integer>> disc = discList.get(chsnPC);
        drives.removeAllElements();
        disc.forEach((v, k) -> {
            int i =0;
            drives.add(i, v);
            i++;
        });
        validate();
    }

    private void ledSetter(String msg){
        if(msg.equals("true")){
            ledPanel.remove(1);
            JPanel contentPane = new JPanel(){
                Graphics2D g2;

                protected void paintComponent(Graphics g){
                    super.paintComponent(g);
                    g2=(Graphics2D)g;
                    g2.setColor(Color.green);
                    g2.fillRoundRect(20, 20, 21, 21, 15, 15);
                }
            };
            ledPanel.add(contentPane);
            ledPanel.repaint();
        } else {
            ledPanel.remove(1);
            JPanel contentPane = new JPanel(){
                Graphics2D g2;

                protected void paintComponent(Graphics g){
                    super.paintComponent(g);
                    g2=(Graphics2D)g;
                    g2.setColor(Color.red);
                    g2.fillRoundRect(20, 20, 21, 21, 15, 15);
                }
            };
            ledPanel.add(contentPane);
            ledPanel.repaint();
        }
    }

    private void setCritical(){
        socketThread.sendMessage(
                ("setLimit" + chsnPC + ";" + driveList.getSelectedValue() + ";" + criticalSize.getText())
                        .getBytes(StandardCharsets.UTF_8));
    }

    public void setDefectivePCs(List<String> defectivePCs){
        this.defectivePCs.clear();
        defectivePCs.forEach(v -> this.defectivePCs.add(v));
    }

    private void update(){
        redo = new Redo(dlm, "pcList", socketThread, this);
        redo.setAskTime(askTime);
        redo.start();
    }

    public synchronized String[] getPcs_names(){
        String msg = "getPcs";
        socketThread.sendMessage(msg.getBytes(StandardCharsets.UTF_8));
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return pcs_names;
    }

    public void setPcList(DefaultListModel<String> dlm){
        this.dlm = dlm;
        validate();
    }

    private void fillSpecs(){
        panelSpecs.removeAll();
        pc_specs.forEach(spec ->{
        JTextField temp = new JTextField(spec);
        temp.setEnabled(false);
        temp.setDisabledTextColor(Color.BLACK);
        panelSpecs.add(temp);
        });
        revalidate();
    }

    private void checkCritical(){

    }

    public String getChsnPC(){
        return chsnPC;
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

    @Override
    public void valueChanged(ListSelectionEvent e) {
        Object src = e.getSource();
        if (src == pcList) {
            int selected = ((JList<?>) src).getSelectedIndex();
            if (selected != -1) {
                chsnPC = dlm.get(selected);
                System.out.println(chsnPC);
                drivesMenu.setEnabled(true);
                socketThread.sendMessage(dlm.get(selected).getBytes(StandardCharsets.UTF_8));
            }
        } else if(src == driveList){
            int selected = ((JList<?>) src).getSelectedIndex();
            if (selected != -1) {
                availableSpace = discList.get(chsnPC).get(drives.get(selected)).get(0).toString();
                label.setText("Available Space: " + availableSpace + "GB");
                criticalSize.setText(discList.get(chsnPC).get(drives.get(selected)).get(1).toString());
                repaint();
                System.out.println(drives.get(selected));
            }
        } else {
            throw new RuntimeException("Unknown source: " + src);
        }
    }

    private JMenu settings() {
        // Создание выпадающего меню
        JMenu settings = new JMenu("Settings");
        // Пункт меню "Открыть" с изображением
        JMenuItem change_ask_time = new JMenuItem("Change ask time");
        // Добавление к пункту меню изображения
        // Добавим в меню пункта open
        settings.add(change_ask_time);
        // Добавление разделителя
//        settings.addSeparator();

        change_ask_time.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panelSpecs.removeAll();
                JTextField text = new JTextField("Current ask time (in sec)");
                text.setEnabled(false);
                text.setDisabledTextColor(Color.BLACK);
                panelSpecs.add(text);
                Integer i = askTime/1000;
                String time = i.toString();
                text = new JTextField(time);
                text.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        JTextField t = (JTextField) actionEvent.getSource();
                        askTime = Integer.parseInt(t.getText()) * 1000;
                        redo.setAskTime(askTime);
                    }
                });
                panelSpecs.add(text);
                panelSpecs.repaint();
            }
        });
        return settings;
    }

    private JMenu services(){
        JMenu services = new JMenu("Services");
        JMenuItem goHome = new JMenuItem("Home");
        JMenuItem checkHost = new JMenuItem("Check Host");

        services.add(goHome);
        services.addSeparator();
        services.add(drivesMenu);
        services.addSeparator();
        services.add(checkHost);

        drivesMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controller.goToMapByName("drives");
                showDrives();
            }
        });
        goHome.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controller.goHome();
            }
        });
        checkHost.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controller.goToMapByName("checkHost");
            }
        });

        return services;
    }

}
