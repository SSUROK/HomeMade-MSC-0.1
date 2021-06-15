package ru.sur.msc.gui;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class MyListCellRenderer extends JLabel implements ListCellRenderer {

    private static List<String> match;

    public MyListCellRenderer(List<String> match) {
        setOpaque(true);
        this.match = match;
    }

    public MyListCellRenderer() {
        setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setText(value.toString());
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        SwingWorker worker = new SwingWorker() {
            @Override
            public Object doInBackground() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) { /*Who cares*/ }
                return null;
            }
            @Override
            public void done() {
                list.repaint();
            }
        };
        if(isSelected){
            setBackground(Color.BLUE);
        }else if(match.contains(value)){
            setBackground(Color.red);
            worker.execute();
        }else if(index % 2 !=0) {
            setBackground(Color.LIGHT_GRAY);
        }else{
            setBackground(Color.WHITE);
        }
        return this;
    }
}
