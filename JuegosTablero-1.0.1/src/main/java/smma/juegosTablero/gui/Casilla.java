/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import juegosTablero.Vocabulario;
import juegosTablero.dominio.elementos.Posicion;

/**
 *
 * @author joseangel
 */
public class Casilla extends JLabel {

    boolean tocado = false;
    Vocabulario.Efecto estadoBarco;
    Vocabulario.TipoBarco b;
    private final ImageIcon water = createImageIcon("/contents/water.jpg");
            
    private ImageIcon barco;
    private String nBarco = "";
    private final Posicion posi, inicial;
    private final Vocabulario.Orientacion orientacion;

    public Casilla(String name, Posicion pos, Posicion _inicial, Vocabulario.Orientacion _orientacion) {
        super();
        Dimension d = new Dimension(48, 48);
        setName(name);
        setSize(d);
        setPreferredSize(d);
        setText("");
        setIcon(water);
        setVisible(true);
        estadoBarco = Vocabulario.Efecto.valueOf("");
        nBarco = name;
        posi = pos;
        inicial = _inicial;
        orientacion = _orientacion;
    }

    public void setBarco(Vocabulario.TipoBarco barco) {
        this.b = barco;
        this.barco = new ImageIcon(getClass().getResource("/contents/" + nBarco + ".jpg"));
    }
    
    public Vocabulario.Orientacion getOrientacion(){
        return orientacion;
    }

    public Vocabulario.TipoBarco getBarco() {
        return this.b;
    }
    
    public String getnCasilla(){
        return nBarco;
    }
    
    public int getPosX(){
        return posi.getCoorX();
    }
    
    public int getPosY(){
        return posi.getCoorY();
    }
    
    public Posicion getPosInicial(){
        return inicial;
    }

    public void setDisparo(String nCasilla) {
        barco = new ImageIcon(getClass().getResource("/contents/" + nCasilla + ".jpg"));
        verBarco();
        this.tocado = true;
        if(!nCasilla.equals("water")){
            estadoBarco = Vocabulario.Efecto.TOCADO;
        }
    }

    public boolean getDisparo() {
        return this.tocado;
    }

    public void verBarco() {
        setIcon(barco);
    }

    public void ocultarBarco() {
        Dimension height = getSize();

        ImageIcon img = new ImageIcon(getClass().getResource("/contents/water.jpg"));
        Graphics g = getGraphics();
        g.drawImage(img.getImage(), 10, 10, height.width - 20, height.height - 20, null);
    }
    
    /** Returns an ImageIcon, or null if the path was invalid.
     * @param path
     * @param description
     * @return  */
    protected ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path + " en:" + System.getProperty("user.dir"));
            return null;
        }
    }
}
