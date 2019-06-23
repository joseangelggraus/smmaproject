/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero.util;

import java.util.Objects;
import juegosTablero.dominio.elementos.Jugador;
import static smma.juegosTablero.Constantes.IGUAL;
import static smma.juegosTablero.Constantes.MAYOR;
import static smma.juegosTablero.Constantes.MENOR;

/**
 *
 * @author pedroj
 */
public class RegistroJugador implements Comparable<RegistroJugador> {
    private final Jugador jugador;
    private int juegosAceptados;

    public RegistroJugador(Jugador jugador) {
        this.jugador = jugador;
        this.juegosAceptados = 1;
    }

    public Jugador getJugador() {
        return jugador;
    }
    
    public int getJuegosAceptados() {
        return juegosAceptados;
    }

    public void addJuego() {
        this.juegosAceptados++;
    }
    
    @Override
    public String toString() {
        return "RegistroJugador{" + "jugador=" + jugador + ", juegosAceptados=" + juegosAceptados + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.jugador);
        hash = 47 * hash + this.juegosAceptados;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RegistroJugador other = (RegistroJugador) obj;
        if (!Objects.equals(this.jugador.getAgenteJugador(), other.jugador.getAgenteJugador())) {
            return false;
        }
        return true;
    }

    /**
     * Se ordenarán de menor a mayor
     * @param jugador
     * @return orden de menor a mayor
     */
    @Override
    public int compareTo(RegistroJugador jugador) {
        int resultado;
        
        if ( this.juegosAceptados < jugador.getJuegosAceptados() )
            resultado = MAYOR;
        else if ( this.juegosAceptados == jugador.getJuegosAceptados() )
            resultado = IGUAL;
        else
            resultado = MENOR;
        
        return resultado;
    }
}
