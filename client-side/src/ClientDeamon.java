import net.core.SocketThread;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
                socketThread.sendMessage(msg);
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
            long freeSpace = new File(root.getAbsolutePath()).getFreeSpace(); //Here I am checking my d drive free space.
            int exitType = (int) Math.pow(1024, 3);
            msg = msg + root.getAbsolutePath() + "=" + freeSpace / exitType + "  ";
        }
        msg = msg + ";";
    }

    private void osChecker(){
         msg = msg + System.getProperty("os.name") + " " + System.getProperty("os.version") + ";";
    }

    private void netChecker(){
        InetAddress ip;
        String hostname;
        try {
            ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
            msg = msg + hostname + ";" + ip + ";";
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void ramChecker(){
        long memorySize = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getFreePhysicalMemorySize();
        int exitType = (int) Math.pow(1024, 2);
        msg = msg + memorySize / exitType + ";";
    }


}
