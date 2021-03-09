package core;

import gui.AdminGUI;
import net.core.SocketThread;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Redo extends Thread implements ActionListener{

    private final JPanel pcList;
    private static JButton[] computers;
    private static String[] pcs_names;
    private final AdminGUI gui;
    private final SocketThread socketThread;
    private static String chsnPC;

    public Redo(JPanel pcList, String name, SocketThread socketThread, AdminGUI gui){
        super(name);
        this.pcList = pcList;
        this.socketThread = socketThread;
        this.gui = gui;
    }

    @Override
    public void run() {
        while(!isInterrupted()) {
            try {
                pcs_names = gui.getPcs_names();
                computers = new JButton[pcs_names.length];
                pcList.removeAll();
                for (int i = 0; i < pcs_names.length; i++) {
                    computers[i] = new JButton(pcs_names[i]);
                    computers[i].addActionListener(this);
                    pcList.add(computers[i]);
                }
                gui.setPcList(pcList);
                if(chsnPC != null){
                    socketThread.sendMessage(chsnPC);
                }
                Thread.sleep(10000); //1000 - 1 сек
            } catch (InterruptedException ex) {
                interrupt();
                System.out.println("Thread has been interrupted");
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        for (JButton button : computers){
            if(src == button){
                chsnPC = button.getText();
                socketThread.sendMessage(button.getText());
            }
        }
    }
}
