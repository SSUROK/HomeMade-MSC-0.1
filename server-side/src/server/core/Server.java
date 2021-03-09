package server.core;

import net.core.ServerSocketThread;
import net.core.ServerSocketThreadListener;
import net.core.SocketThread;
import net.core.SocketThreadListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.logging.*;

public class Server implements ServerSocketThreadListener, SocketThreadListener {

    private static final Logger logger = Logger.getLogger("MyFirstLogger");
    private ServerSocketThread server;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    private Vector<SocketThread> clients = new Vector<>();
    private ServerListener listener;
    private DBconnection db;

    public Server(ServerListener listener) {
        this.listener = listener;
        try {
            Handler h = new FileHandler("mylogsimple.log");
            h.setFormatter(new SimpleFormatter());
            logger.addHandler(h);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(int port) {
        if (server != null && server.isAlive()) {
            logger.log(Level.SEVERE, "Server already stared");
            putLog("Server already stared");
        } else {
            server = new ServerSocketThread(this, "Server", port, 2000);
        }
    }

    public void stop() {
        if (server == null || !server.isAlive()) {
            logger.log(Level.SEVERE, "Server is not running");
            putLog("Server is not running");
        } else {
            server.interrupt();
        }
    }

    private void putLog(String msg) {
        msg = DATE_FORMAT.format(System.currentTimeMillis()) +
                Thread.currentThread().getName() + ": " + msg;
        listener.onChatServerMessage(msg);
    }

    /**
     * Server Socket Thread Listener methods
     * */

    @Override
    public void onServerStart(ServerSocketThread thread) {
        logger.log(Level.SEVERE, "Server started");
        putLog("Server started");
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        logger.log(Level.SEVERE, "Server stopped");
        putLog("Server stopped");
        db.disconnect();
    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        logger.log(Level.SEVERE, "Listening to port");
        putLog("Listening to port");
    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {
//        putLog("Ping? Pong!");
    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket) {
        // client connected
        String name = "Client " + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, name, socket);
    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
    }

    /**
     * Socket Thread Listener methods
     * */

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        logger.log(Level.SEVERE, "Client thread started");
        putLog("Client thread started");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        logger.log(Level.SEVERE, "Client thread stopped");
        putLog("Client thread stopped");
        clients.remove(thread);
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        logger.log(Level.SEVERE, "Client is ready to chat");
        putLog("Client is ready to chat");
        clients.add(thread);
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        ClientThread client = (ClientThread) thread;
        if(msg.equals("adminauthtorize")){
            client.setName("Admin");
            client.sendMessage("successauth");
        } else if(msg.equals("getPcs")){
            try {
                client.sendMessage(db.getPcsNames());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }else if(client.getName().equals("Admin")){
            sendToAdmin(client, msg);
        } else {
            System.out.println(msg);
            dbSave(client, msg);
        }
    }

    public void dbConnect(String DB_URL, String USER, String PASS){
        db = new DBconnection(DB_URL, USER, PASS);
        try {
            if (db.test()){
                logger.log(Level.SEVERE, "DB connected");
                putLog("DB connected");
            }else{
                logger.log(Level.SEVERE, "DB connection error");
                putLog("DB connection error");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void dbSave(ClientThread client, String msg){
        String[] pc_specs =  msg.split(";");
        try {
            db.dbSave(pc_specs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendToAdmin(ClientThread client, String msg){
        try {
            client.sendMessage(db.dbGet(msg));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }
}
