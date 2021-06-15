package server.core;

import net.core.ServerSocketThread;
import net.core.ServerSocketThreadListener;
import net.core.SocketThread;
import net.core.SocketThreadListener;
import ru.sur.msc.common.Common;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        String name = thread.getName();
//        System.out.println(thread.getName());
        try {
            System.out.println("setting offline");
            String ip = name.substring(name.lastIndexOf("/")+1, name.lastIndexOf(":"));
            System.out.println(ip);
            db.setOffline(ip);
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
    public void onReceiveString(SocketThread thread, Socket socket, byte[] bytemsg) {
        String msg = new String(bytemsg);
        ClientThread client = (ClientThread) thread;
        if(msg.equals("adminauthtorize")){
            client.setName("Admin");
            client.sendMessage("successauth".getBytes());
        } else if(msg.contains("getPcs")){
            client.sendMessage(db.getPcsNames().getBytes());
        }else if(msg.contains("setLimit")){
            msg = msg.substring(8);
            setLimit(msg);
        } else if(msg.contains("getDrives")) {
            getDrives(client);
        } else if(msg.contains("getRam")) {
            getRam(client);
        } else if(msg.contains("checkHost")) {
            msg = msg.substring(9);
            checkHost(msg, client);
        } else if(msg.equals("true") || msg.equals("false")){
            String send = msg;
            clients.forEach(v ->{
                if(v.toString().contains("Admin")){
                    v.sendMessage(send.getBytes(StandardCharsets.UTF_8));
                }
            });
        } else if(msg.contains("check")) {
            msg = msg.substring(6);
            checkExistence(msg, client);
        } else if(client.getName().equals("Admin")) {
            sendToAdmin(client, msg);
        } else {
//            System.out.println(msg);
            dbSave(client, msg);
        }
    }

    private void checkHost(String msg, ClientThread client){
        String[] temp = msg.split(";");
        try {
            clients.forEach(v -> {
                if (v.toString().contains(temp[0])) {
                    v.sendMessage(("ping" + temp[1] + ";" + temp[2]).getBytes(StandardCharsets.UTF_8));
                } else {
                    client.sendMessage("false".getBytes(StandardCharsets.UTF_8));
                }
            });
        } catch (ArrayIndexOutOfBoundsException e){
            System.out.println("Port is not here");
        }
    }

    private void setLimit(String msg){
        try {
            db.setLimit(msg);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void getDrives(ClientThread client){
        try {
            Map<String, Map<String, List<Integer>>> drives = db.getDrives();

            // Convert Map to byte array
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(drives);

            client.sendMessage(byteOut.toByteArray());
        } catch (SQLException | IOException throwables) {
            throwables.printStackTrace();
        }
    }

    private void getRam(ClientThread client){
        try {
            Map<String, List<Integer>> ram = db.getRam();

            // Convert Map to byte array
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(ram);

            client.sendMessage(byteOut.toByteArray());
        } catch (SQLException | IOException throwables) {
            throwables.printStackTrace();
        }
    }

    public void dbConnect(String DB_URL, String USER, String PASS){
        db = new DBconnection(DB_URL, USER, PASS, this);
    }

    public void succConnectDB(){
        logger.log(Level.SEVERE, "DB connected");
        putLog("DB connected");
    }

    public void errorConnectDB(){
        logger.log(Level.SEVERE, "DB connection error");
        putLog("DB connection error");
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
            client.sendMessage(db.dbGet(msg).getBytes());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void checkExistence(String ip, ClientThread client){
        try {
            if(db.check(ip)){
                client.sendMessage("exist".getBytes());
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
