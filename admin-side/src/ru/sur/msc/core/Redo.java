package ru.sur.msc.core;

import ru.sur.msc.gui.AdminGUI;
import net.core.SocketThread;

import javax.swing.*;
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
    private static Map<String, Map<String, List<Integer>>> discList;
    private static Map<String, List<Integer>> ramList;

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
                getDrives();
                getRam();
                Thread.sleep(100);
                analyzer();

                Thread.sleep(askTime); //1000 - 1 сек
            } catch (InterruptedException ex) {
                interrupt();
                System.out.println("Thread has been interrupted");
            }
        }
    }

    public void getDrives(){
        String msg = "getDrives";
        socketThread.sendMessage(msg.getBytes(StandardCharsets.UTF_8));
    }

    public void getRam(){
        String msg = "getRam";
        socketThread.sendMessage(msg.getBytes(StandardCharsets.UTF_8));
    }

    public List<String> discAnalyzer(Map<String, Map<String, List<Integer>>> discList){
        List<String> overloadedDrives = new ArrayList<>();
        discList.forEach((k, v) -> {
            v.forEach((name, value) -> {
                if(value.get(0) < value.get(1)){
                    overloadedDrives.add(k);
                }
            });
        });


        return overloadedDrives;
    }

    public List<String> ramAnalyzer(Map<String, List<Integer>> ramList){
        List<String> overloadedRam = new ArrayList<>();
        ramList.forEach((k, v) -> {
            if(v.get(0) < v.get(1)){
                overloadedRam.add(k);
            }
        });

        return overloadedRam;
    }

    public void analyzer(){
        List<String> defectivePCs = new ArrayList<>();

        defectivePCs.addAll(discAnalyzer(discList));
        defectivePCs.addAll(ramAnalyzer(ramList));

        gui.setDefectivePCs(defectivePCs);
    }

    public static void setDiscList(Map<String, Map<String, List<Integer>>> discList) {
        Redo.discList = discList;
    }

    public static void setRamList(Map<String, List<Integer>> ramList) {
        Redo.ramList = ramList;
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
