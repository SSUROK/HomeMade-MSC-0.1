package server.gui;

import server.core.Server;
import server.core.ServerListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerGUI extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler, ServerListener {

    private static final int POS_X = 800;
    private static final int POS_Y = 200;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;

    static final String DB_URL = "jdbc:postgresql://127.0.0.1:5432/HomeMadeMSC";
    static final String USER = "ilavinogradov";
    static final String PASS = "";

    private final JTextField tfDBAdress = new JTextField(DB_URL);//"Адресс ДБ"
    private final JTextField tfDBLogin =  new JTextField(USER);//"Логин ДБ"
    private final JPasswordField tfDBPass = new JPasswordField(PASS);//"Пароль"

    private final Server chatServer = new Server(this);
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop = new JButton("Stop");
    private final JButton btnConnect = new JButton("Connect DB");
    private final JPanel panelDB = new JPanel(new GridLayout(4, 1));
    private final JPanel panelTop = new JPanel(new GridLayout(1, 2));
    private final JTextArea log = new JTextArea();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ServerGUI();
            }
        });
    }

    private ServerGUI() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(POS_X, POS_Y, WIDTH, HEIGHT);
        setResizable(false);
        setTitle("Chat server");
        log.setEditable(false);
        log.setLineWrap(true);
        log.setMinimumSize(new Dimension(300, 150));
        JScrollPane scrollLog = new JScrollPane(log);

        tfDBAdress.setToolTipText("Адресс ДБ");
        tfDBLogin.setToolTipText("Логин ДБ");
        tfDBPass.setToolTipText("Пароль");

        btnStart.addActionListener(this);
        btnStop.addActionListener(this);
        btnConnect.addActionListener(this);

        panelDB.add(tfDBAdress);
        panelDB.add(tfDBLogin);
        panelDB.add(tfDBPass);
        panelDB.add(btnConnect);

        panelTop.add(btnStart);
        panelTop.add(btnStop);

        add(panelDB, BorderLayout.WEST);
        add(panelTop, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnStop) {
            chatServer.stop();
        }else if (src == btnStart) {
            chatServer.start(8189);
        } else if (src == btnConnect){
            String password = new String(tfDBPass.getPassword());
            chatServer.dbConnect(tfDBAdress.getText(), tfDBLogin.getText(), password);
        }else {
            throw new RuntimeException("Unknown source: " + src);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        String msg;
        StackTraceElement[] ste = e.getStackTrace();
        msg = "Exception in " + t.getName() + " " +
                e.getClass().getCanonicalName() + ": " +
                e.getMessage() + "\n\t at " + ste[0];
        JOptionPane.showMessageDialog(this, msg, "Exception", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    @Override
    public void onChatServerMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

}
