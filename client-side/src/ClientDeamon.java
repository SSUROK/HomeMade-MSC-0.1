import net.core.SocketThread;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ClientDeamon extends Thread {

    private static File[] roots;
    private SocketThread socketThread;
    private String msg;

    public ClientDeamon(String name, SocketThread socketThread){
        super(name);
        this.socketThread = socketThread;
        setDaemon(true);
    }

    @Override
    public void run() {
        while(!isInterrupted()) {
            try {
                msg = "";
                netChecker();
                osChecker();
                ramChecker();
                diskChecker();
                socketThread.sendMessage(msg.getBytes());
                Thread.sleep(10000); //1000 - 1 сек
            } catch (InterruptedException ex) {
                interrupt();
                System.out.println("Thread has been interrupted");
            }
        }
    }

    private void diskChecker(){
        roots = File.listRoots();

        for (File root: roots) {
            long freeSpace = new File(root.getAbsolutePath()).getFreeSpace(); //Here I am checking my drive free space.
            int exitType = (int) Math.pow(1024, 3);
            msg = msg + root.getAbsolutePath() + "=" + freeSpace / exitType + " ";
        }
        msg = msg + ";";
    }

    private void osChecker(){
         msg = msg + System.getProperty("os.name") + " " + System.getProperty("os.version") + ";";
    }

    public void pingHost(String host) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, 80), 2000);
            socketThread.sendMessage("true".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            socketThread.sendMessage("false".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void netChecker(){
        InetAddress ip;
        String ip_send;
        String hostname;
        Socket socket = new Socket();
        try {
            ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
            socket.connect(new InetSocketAddress("google.com", 80));
            ip_send = socket.getLocalAddress().toString().substring(1);
            System.out.println(ip_send);
            msg = msg + hostname + ";" + ip_send + ";";
        } catch (IOException e) {
            e.printStackTrace();
        }
//        try(final DatagramSocket socket = new DatagramSocket()){
//            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
//            ip = socket.getLocalAddress().getHostAddress();
//            System.out.println(ip);
//        } catch (SocketException | UnknownHostException e) {
//            e.printStackTrace();
//        }
//        try {
//            ip = InetAddress.getLocalHost();
//            hostname = ip.getHostName();
//            msg = msg + hostname + ";" + ip + ";";
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
    }

    private void ramChecker(){
        long memorySize = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getFreePhysicalMemorySize();
        int exitType = (int) Math.pow(1024, 2);
        msg = msg + memorySize / exitType + ";";
    }


}
