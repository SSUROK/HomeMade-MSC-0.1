package gui;

import javax.swing.*;
import java.awt.*;

public class LED extends JComponent {

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.RED);
        g2.fillRoundRect(140, 10, 20, 20, 10, 10);
    }

}