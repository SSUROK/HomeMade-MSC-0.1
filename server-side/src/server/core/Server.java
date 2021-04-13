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
import java.util.*;
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
        System.out.println(thread.getName());
        try {
            System.out.println("setting offline");
            db.setOffline(thread.getName());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
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
        } else if(msg.contains("getDrives")) {
            getDrives(client);
        } else if(msg.equals("getPcs")){
            try {
                client.sendMessage(db.getPcsNames());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }else if(client.getName().equals("Admin")) {
            sendToAdmin(client, msg);
        } else if(msg.contains("check")) {
            msg = msg.substring(6);
            checkExistence(msg, client);
        } else {
            dbSave(client, msg);
        }
    }

    private void getDrives(ClientThread client){
//        String overloadedDrives = "drives";
        List<String> overloadedDrives = new ArrayList<>();
        try {
            Map<String, Map<String, List<Integer>>> drives = db.getDrives();
//            Map<String, Map<String, List<Integer>>> constructor = new HashMap<>();
//            String[] d = drives.toString().split("},");
//            System.out.println(Arrays.toString(d));
//            for(String dr :d) {
//                String[] d1 = dr.split(("=\\{"));
//                for(int i = 0; i< d1.length; i++){
//                    if(i%2==0){
//                        constructor.put(d1[i], null);
//                    }
//                }
//                for(String d1r : d1){
//                    String[] d2 = d1r.split("],");
//                    for(String d2r : d2) {
//                        String[] d3 = d2r.split("=\\[");
//                        for(String d3r : d3){
//                            String[] d4 = d3r.split(", ");
//                            System.out.println(Arrays.toString(d4));
//                        }
//                    }
//                }
//            }
            drives.forEach((k, v) -> {
                v.forEach((name, value) -> {
                    if(value.get(0) < value.get(1)){
                        overloadedDrives.add(k);
                    }
                });
                System.out.println();
            });
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        String msg = "drives" + overloadedDrives;
        client.sendMessage(msg);
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
            client.setName(pc_specs[1]);
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

    private void checkExistence(String ip, ClientThread client){
        try {
            if(db.check(ip)){
                client.sendMessage("exist");
            } else {
                db.setOnline(ip);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }
}
