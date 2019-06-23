/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 *
 * @author joseangel
 */
public class PanelFicha extends JPanel {

    String ficha;

    PanelFicha(String ficha) {
        this.ficha = ficha;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!ficha.isEmpty()) {
            Dimension d = getSize();
            ImageIcon img = new ImageIcon(getClass().getResource("/contents/" + ficha + ".jpg"));

            if (ficha.charAt(1) == 't') {
                g.drawImage(img.getImage(), 10, 10, d.width - 20, d.height - 20, null);
            } else {
                g.drawImage(img.getImage(), 30, 30, d.width / 2, d.height / 2, null);

            }
        }
    }
    
    public boolean isThis(String _ficha) {
        return ficha.equals(_ficha);
    }
    
    public void invisible(){
        this.setVisible(false);
    }
}
