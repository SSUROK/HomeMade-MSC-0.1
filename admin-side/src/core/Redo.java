package core;

import gui.AdminGUI;
import net.core.SocketThread;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Redo extends Thread {

    private static DefaultListModel<String> dlm = new DefaultListModel<>();
    private static String[] pcs_names;
    private final AdminGUI gui;
    private final SocketThread socketThread;
    private static Integer askTime;

    public Redo(DefaultListModel<String> dlm, String name, SocketThread socketThread, AdminGUI gui) {
        super(name);
        this.dlm = dlm;
        this.socketThread = socketThread;
        this.gui = gui;
    }

    public void setAskTime(int time){
        askTime = time;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                pcListUpdater();
                getdrives();

                Thread.sleep(askTime); //1000 - 1 сек
            } catch (InterruptedException ex) {
                interrupt();
                System.out.println("Thread has been interrupted");
            }
        }
    }

    public void getdrives(){
        String msg = "getDrives";
        socketThread.sendMessage(msg.getBytes(StandardCharsets.UTF_8));
    }

    public void discAnalyzer(Map<String, Map<String, List<Integer>>> discList){
        List<String> overloadedDrives = new ArrayList<>();
        discList.forEach((k, v) -> {
            v.forEach((name, value) -> {
                if(value.get(0) < value.get(1)){
                    overloadedDrives.add(k);
                }
            });
        });
        gui.setDefectivePCs(overloadedDrives);
    }

    private void pcListUpdater(){
        pcs_names = gui.getPcs_names();
        dlm.removeAllElements();
        for(String name : pcs_names){
            dlm.add(0, name);
        }
        gui.setPcList(dlm);

        String chsnPC = gui.getChsnPC();
        if(chsnPC != null){
            socketThread.sendMessage(chsnPC.getBytes(StandardCharsets.UTF_8));
        }
    }
}
