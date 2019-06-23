/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import juegosTablero.Vocabulario;
import juegosTablero.aplicacion.barcos.ResultadoMovimiento;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.Posicion;

/**
 *
 * @author joseangel
 */
public class Tablerom extends JInternalFrame {

    private final int filas = 10;
    private final int columnas = 10;
    private final int anchoAlto = 48;
    private final Casilla[][] matriz = new Casilla[filas][columnas];

    public Tablerom() {
        super();
        this.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.setLayout(new java.awt.GridLayout(filas, columnas));
        Dimension d = new Dimension((anchoAlto * columnas), (anchoAlto * filas));
        this.setSize(d);
        this.setPreferredSize(d);
        int cont = 0;
        Posicion pos = new Posicion();
        for (int i = 1; i < (filas * columnas); i++) {
            Casilla p = new Casilla("", pos, pos, Vocabulario.Orientacion.HORIZONTAL);
            
            p.setBarco(Vocabulario.TipoBarco.valueOf(""));
            cont++;
            cont = (cont >= (filas*columnas)/2) ? 0 : cont++;
            p.verBarco();
            this.add(p);
        }
        //inicializamos la matriz interna
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                matriz[i][j] = new Casilla("", pos, pos, Vocabulario.Orientacion.valueOf(""));
            }

        }
        this.setVisible(true);
    }

    public void empezarJuego() {

        Component[] comp = this.getComponents();

        //Limpiamos barcos
        for (Component comp1 : comp) {
            ((Casilla) comp1).ocultarBarco();
            ((Casilla) comp1).setBarco(Vocabulario.TipoBarco.valueOf(""));
        }

        //Colocamos barcos de forma aleatoria
        colocarBarcos();

    }

    public ResultadoMovimiento disparo(Juego juego, Posicion pos) {
        Casilla act = matriz[pos.getCoorX()][pos.getCoorY()];
        String n = act.getnCasilla();
        Vocabulario.Orientacion ori = act.getOrientacion();
        ResultadoMovimiento rm = new ResultadoMovimiento();
        rm.setJuego(juego);
        rm.setMovimiento(pos);
        if (!n.equals("water")){
            Casilla castocada = new Casilla("t_" + n, pos, act.getPosInicial(), ori);
            matriz[pos.getCoorX()][pos.getCoorY()] = castocada;
            castocada.setDisparo("t_" + n); 
            boolean tochund = comprobarBarco(pos, ori);
            if(tochund == false){
                rm.setResultado(Vocabulario.Efecto.TOCADO);
                return rm;
            }else{
                rm.setResultado(Vocabulario.Efecto.HUNDIDO);
                return rm;
            }
        }else{
            rm.setResultado(Vocabulario.Efecto.AGUA);
            return rm;
        }    
    }

    public void colocarBarcos() {
        Vocabulario.TipoBarco barco;
        int tamBarco = 0;
        Random rand1 = new Random();
        int posX, posY;
        String b = "";
        for (Vocabulario.TipoBarco i : Vocabulario.TipoBarco.values()) {
            //generamos la posición entre 0 y 9 en la que colocar el barco
            posX = rand1.nextInt(10);
            posY = rand1.nextInt(10);
            //Vemos qué tipo de barco es y las casillas que ocupa
            barco = i;
            switch (barco) {
                case ACORAZADO:
                    tamBarco = Vocabulario.TipoBarco.ACORAZADO.getCasillas();
                    b = Vocabulario.TipoBarco.ACORAZADO.name();
                    break;
                    
                case DESTRUCTOR:
                    tamBarco = Vocabulario.TipoBarco.DESTRUCTOR.getCasillas();
                    b = Vocabulario.TipoBarco.DESTRUCTOR.name();
                    break;
                    
                case FRAGATA:
                    tamBarco = Vocabulario.TipoBarco.FRAGATA.getCasillas();
                    b = Vocabulario.TipoBarco.FRAGATA.name();
                    break;
                    
                case PORTAAVIONES:
                    tamBarco = Vocabulario.TipoBarco.PORTAAVIONES.getCasillas();
                    b = Vocabulario.TipoBarco.PORTAAVIONES.name();
                    break;
            }

            if (matriz[posX][posY].getnCasilla().equals("")) {
                //Si estamos en los laterales superior o inferior de la matriz,
                //colocamos el barco en horizontal
                if (posX == 0 || posX == 9) {

                    if (10 - posY >= tamBarco) {
                        Posicion inicial = new Posicion(posX, posY);
                        //Si hay hueco en la derecha colocamos a partir de aquí
                        for (int j = 0; j < tamBarco; j++) {
                            Posicion pos = new Posicion(posX, posY + j);
                            Casilla c = new Casilla(b + j + 1 + "_" + tamBarco, pos, inicial, Vocabulario.Orientacion.HORIZONTAL);
                            c.setBarco(barco);
                            this.add(c);
                            matriz[posX][posY + j] = c;
                        }
                        //Colocado el barco, lo rodeamos por agua para no colocar ahí más barcos
                        rodearDeAgua(inicial, tamBarco, "h");

                    } else if (posY >= tamBarco) {
                        Posicion inicial = new Posicion(posX, posY - tamBarco + 1);
                        //Si hay hueco a la izquierda, colocamos a partir de aquí
                        for (int j = tamBarco; j > 0; j++) {
                            Posicion pos = new Posicion(posX, posY - j);
                            Casilla c = new Casilla(b + j + 1 + "_" + tamBarco, pos, inicial, Vocabulario.Orientacion.HORIZONTAL);
                            c.setBarco(barco);
                            this.add(c);
                        }
                        //Colocado el barco, lo rodeamos por agua para no colocar ahí más barcos
                        rodearDeAgua(inicial, tamBarco, "h");
                    }

                } else if (posY == 0 || posY == 9) {
                    //Si estamos en los laterales derecho o izquierdo de la matriz,
                    //colocamos el barco en vertical

                    if (10 - posX >= tamBarco) {
                        //Si hay hueco para abajo colocamos a partir de aquí
                        Posicion inicial = new Posicion(posX, posY);
                        for (int j = 0; j < tamBarco; j++) {
                            Posicion pos = new Posicion(posX + j, posY);
                            Casilla c = new Casilla(b + j + 1 + "_" + tamBarco, pos, inicial, Vocabulario.Orientacion.VERTICAL);
                            c.setBarco(barco);
                            this.add(c);
                        }
                        //Colocado el barco, lo rodeamos por agua para no colocar ahí más barcos
                        rodearDeAgua(inicial, tamBarco, "v");

                    } else if (posX >= tamBarco) {
                        //Si hay hueco a la arriba, colocamos a partir de aquí
                        Posicion inicial = new Posicion(posX - tamBarco + 1, posY);
                        for (int j = tamBarco; j > 0; j++) {
                            Posicion pos = new Posicion(posX - j, posY);
                            Casilla c = new Casilla(b + j + 1 + "_" + tamBarco, pos, inicial, Vocabulario.Orientacion.VERTICAL);
                            c.setBarco(barco);
                            this.add(c);
                        }
                        //Colocado el barco, lo rodeamos por agua para no colocar ahí más barcos
                        rodearDeAgua(inicial, tamBarco, "v");
                    }

                } else {
                    /**
                     * Usamos un random para colocar el barco en vertical o en
                     * horizontal.
                     *
                     * @params 
                     *  0: horizontal 
                     *  1: vertical
                     */
                    Random rand = new Random();
                    int VoH = rand.nextInt(1);

                    if (VoH == 0) {
                        if (10 - posY >= tamBarco) {
                            //Si hay hueco en la derecha colocamos a partir de aquí
                            Posicion inicial = new Posicion(posX, posY);
                            for (int j = 0; j < tamBarco; j++) {
                                Posicion pos = new Posicion(posX, posY + j);
                                Casilla c = new Casilla(b + j + 1 + "_" + tamBarco, pos, inicial, Vocabulario.Orientacion.HORIZONTAL);
                                c.setBarco(barco);
                                this.add(c);
                            }
                            //Colocado el barco, lo rodeamos por agua para no colocar ahí más barcos
                            rodearDeAgua(inicial, tamBarco, "h");

                        } else if (posY >= tamBarco) {
                            //Si hay hueco a la izquierda, colocamos a partir de aquí
                            Posicion inicial = new Posicion(posX, posY - tamBarco + 1);
                            for (int j = tamBarco; j > 0; j++) {
                                Posicion pos = new Posicion(posX, posY - j);
                                Casilla c = new Casilla(b + j + 1 + "_" + tamBarco, pos, inicial, Vocabulario.Orientacion.HORIZONTAL);
                                c.setBarco(barco);
                                this.add(c);
                            }
                            //Colocado el barco, lo rodeamos por agua para no colocar ahí más barcos
                            rodearDeAgua(inicial, tamBarco, "h");

                        }
                    } else {
                        if (10 - posX >= tamBarco) {
                            //Si hay hueco en la abajo colocamos a partir de aquí
                            Posicion inicial = new Posicion(posX, posY);
                            for (int j = 0; j < tamBarco; j++) {
                                Posicion pos = new Posicion(posX + j, posY);
                                Casilla c = new Casilla(b + j + 1 + "_" + tamBarco, pos, inicial, Vocabulario.Orientacion.VERTICAL);
                                c.setBarco(barco);
                                this.add(c);
                            }
                            //Colocado el barco, lo rodeamos por agua para no colocar ahí más barcos
                            rodearDeAgua(inicial, tamBarco, "v");

                        } else if (posX >= tamBarco) {
                            //Si hay hueco a la arriba, colocamos a partir de aquí
                            Posicion inicial = new Posicion(posX - tamBarco + 1, posY);
                            for (int j = tamBarco; j > 0; j++) {
                                Posicion pos = new Posicion(posX - j, posY);
                                Casilla c = new Casilla(b + j + 1 + "_" + tamBarco, pos, inicial, Vocabulario.Orientacion.VERTICAL);
                                c.setBarco(barco);
                                this.add(c);
                            }
                            //Colocado el barco, lo rodeamos por agua para no colocar ahí más barcos
                            rodearDeAgua(inicial, tamBarco, "v");
                        }
                    }
                }
            }
        }
        //Completamos el tablero con agua
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (matriz[f][c].getnCasilla().equals("")) {
                    Posicion posw = new Posicion(f, c);
                    Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                    this.add(w);
                    matriz[f][c] = w;
                }
            }
        }

    }

    private void rodearDeAgua(Posicion pos, int tamBarco, String orientacion) {
        /**
         * Para rodear un barco colocado en horizontal hay que comprobar como
         * casos excepcionales que no esté en la primera o última columna
         *
         * Para rodear un barco colocado en vertical hay que comprobar como
         * casos excepcionales que no esté en la primera o última fila
         */

        int posX = pos.getCoorX();
        int posY = pos.getCoorY();

        if (orientacion.equals("h")) {
            if (posX == 0 && posY == 0) {
                for (int f = 0; f < 2; f++) {
                    for (int c = 0; c < tamBarco + 1; c++) {
                        if (matriz[f][c].getnCasilla().equals("")) {
                            Posicion posw = new Posicion(f, c);
                            Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                            this.add(w);
                            matriz[f][c] = w;
                        }
                    }
                }
            } else if (posX == 0 && posY == 9) {
                for (int f = 0; f < 2; f++) {
                    for (int c = posY - 1; c < tamBarco; c++) {
                        if (matriz[f][c].getnCasilla().equals("")) {
                            Posicion posw = new Posicion(f, c);
                            Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                            this.add(w);
                            matriz[f][c] = w;
                        }
                    }
                }
            } else if (posX == 9 && posY == 0) {
                for (int f = 8; f < 10; f++) {
                    for (int c = 0; c < tamBarco + 1; c++) {
                        if (matriz[f][c].getnCasilla().equals("")) {
                            Posicion posw = new Posicion(f, c);
                            Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                            this.add(w);
                            matriz[f][c] = w;
                        }
                    }
                }
            } else if (posX == 9 && posY == 9) {
                for (int f = 8; f < 10; f++) {
                    for (int c = posY - 1; c < tamBarco; c++) {
                        if (matriz[f][c].getnCasilla().equals("")) {
                            Posicion posw = new Posicion(f, c);
                            Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                            this.add(w);
                            matriz[f][c] = w;
                        }
                    }
                }
            } else {
                if (posX == 0) {
                    for (int f = 0; f < 2; f++) {
                        for (int c = posY - 1; c < posY + tamBarco + 1; c++) {
                            if (matriz[f][c].getnCasilla().equals("")) {
                                Posicion posw = new Posicion(f, c);
                                Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                                this.add(w);
                                matriz[f][c] = w;
                            }
                        }
                    }
                } else if (posX == 9) {
                    for (int f = 8; f < 10; f++) {
                        for (int c = posY - 1; c < posY + tamBarco + 1; c++) {
                            if (matriz[f][c].getnCasilla().equals("")) {
                                Posicion posw = new Posicion(f, c);
                                Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                                this.add(w);
                                matriz[f][c] = w;
                            }
                        }
                    }
                } else if (posY == 0) {
                    for (int f = posX - 1; f <= posX + 1; f++) {
                        for (int c = posY; c < tamBarco + 1; c++) {
                            if (matriz[f][c].getnCasilla().equals("")) {
                                Posicion posw = new Posicion(f, c);
                                Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                                this.add(w);
                                matriz[f][c] = w;
                            }
                        }
                    }
                } else if (posY == 9) {
                    for (int f = posX - 1; f <= posX + 1; f++) {
                        for (int c = posY - tamBarco; c <= posY; c++) {
                            if (matriz[f][c].getnCasilla().equals("")) {
                                Posicion posw = new Posicion(f, c);
                                Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                                this.add(w);
                                matriz[f][c] = w;
                            }
                        }
                    }
                } else {
                    for (int f = posX - 1; f <= posX + 1; f++) {
                        for (int c = posY - 1; c < posY + tamBarco + 1; c++) {
                            if (matriz[f][c].getnCasilla().equals("")) {
                                Posicion posw = new Posicion(f, c);
                                Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                                this.add(w);
                                matriz[f][c] = w;
                            }
                        }
                    }
                }
            }
        } else {
            if (posX == 0 && posY == 0) {
                for (int f = 0; f < tamBarco + 1; f++) {
                    for (int c = 0; c < 2; c++) {
                        if (matriz[f][c].getnCasilla().equals("")) {
                            Posicion posw = new Posicion(f, c);
                            Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                            this.add(w);
                            matriz[f][c] = w;
                        }
                    }
                }
            } else if (posX == 0 && posY == 9) {
                for (int f = 0; f < tamBarco + 1; f++) {
                    for (int c = 8; c < 10; c++) {
                        if (matriz[f][c].getnCasilla().equals("")) {
                            Posicion posw = new Posicion(f, c);
                            Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                            this.add(w);
                            matriz[f][c] = w;
                        }
                    }
                }
            } else if (posX == 9 && posY == 0) {
                for (int f = posX - tamBarco; f <= posX; f++) {
                    for (int c = 0; c < 2; c++) {
                        if (matriz[f][c].getnCasilla().equals("")) {
                            Posicion posw = new Posicion(f, c);
                            Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                            this.add(w);
                            matriz[f][c] = w;
                        }
                    }
                }
            } else if (posX == 9 && posY == 9) {
                for (int f = posX - tamBarco; f <= posX; f++) {
                    for (int c = 8; c < 10; c++) {
                        if (matriz[f][c].getnCasilla().equals("")) {
                            Posicion posw = new Posicion(f, c);
                            Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                            this.add(w);
                            matriz[f][c] = w;
                        }
                    }
                }
            } else {
                if (posX == 0) {
                    for (int f = posX; f < posX + tamBarco + 1; f++) {
                        for (int c = posY - 1; c <= posY + 1; c++) {
                            if (matriz[f][c].getnCasilla().equals("")) {
                                Posicion posw = new Posicion(f, c);
                                Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                                this.add(w);
                                matriz[f][c] = w;
                            }
                        }
                    }
                } else if (posX == 9) {
                    for (int f = posX - tamBarco; f <= posX; f++) {
                        for (int c = posY - 1; c <= posY + 1; c++) {
                            if (matriz[f][c].getnCasilla().equals("")) {
                                Posicion posw = new Posicion(f, c);
                                Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                                this.add(w);
                                matriz[f][c] = w;
                            }
                        }
                    }
                } else if (posY == 0) {
                    for (int f = posX - 1; f <= posX + tamBarco + 1; f++) {
                        for (int c = posY; c <= posY + 1; c++) {
                            if (matriz[f][c].getnCasilla().equals("")) {
                                Posicion posw = new Posicion(f, c);
                                Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                                this.add(w);
                                matriz[f][c] = w;
                            }
                        }
                    }
                } else if (posY == 9) {
                    for (int f = posX - 1; f <= posX + tamBarco + 1; f++) {
                        for (int c = posY - 1; c <= posY; c++) {
                            if (matriz[f][c].getnCasilla().equals("")) {
                                Posicion posw = new Posicion(f, c);
                                Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                                this.add(w);
                                matriz[f][c] = w;
                            }
                        }
                    }
                } else {
                    for (int f = posX - 1; f <= posX + tamBarco + 1; f++) {
                        for (int c = posY - 1; c <= posY + 1; c++) {
                            if (matriz[f][c].getnCasilla().equals("")) {
                                Posicion posw = new Posicion(f, c);
                                Casilla w = new Casilla("water", posw, posw, Vocabulario.Orientacion.valueOf(""));
                                this.add(w);
                                matriz[f][c] = w;
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Nos devuelve si un barco ha sido tocado o hundido
     * @param pos
     * @return 
     *  false: tocado
     *  true: hundido
     */
    private boolean comprobarBarco(Posicion pos, Vocabulario.Orientacion orientacion){
        Casilla act = matriz[pos.getCoorX()][pos.getCoorY()];
        int tamBarco = matriz[pos.getCoorX()][pos.getCoorY()].getBarco().getCasillas();
        int cont = 0;
        String nCasilla = matriz[pos.getCoorX()][pos.getCoorY()].getnCasilla();
        String[] spliteado = nCasilla.split("_");
        if (orientacion.equals(Vocabulario.Orientacion.HORIZONTAL)) {
            for (int i = act.getPosInicial().getCoorX(); i < act.getPosInicial().getCoorX() + tamBarco; i++) {
                if(!matriz[i][pos.getCoorY()].getnCasilla().equals("water") && spliteado[0].equals("t")){
                    cont++;
                }
            }
            return cont == tamBarco;
        }else{
            for (int i = act.getPosInicial().getCoorY(); i < act.getPosInicial().getCoorY() + tamBarco; i++) {
                if(!matriz[pos.getCoorX()][i].getnCasilla().equals("water") && spliteado[0].equals("t")){
                    cont++;
                }
            }
            return cont == tamBarco;
        }
    }
    
    public Casilla getCasilla(Posicion pos){
        return matriz[pos.getCoorX()][pos.getCoorY()];
    }
    
    public Casilla[][] getTablero(){
        return matriz;
    }

}
