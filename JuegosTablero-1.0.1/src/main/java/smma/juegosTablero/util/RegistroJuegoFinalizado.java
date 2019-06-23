/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero.util;

import jade.content.Predicate;
import jade.util.leap.Serializable;
import java.util.Objects;
import juegosTablero.dominio.elementos.Grupo;
import static smma.juegosTablero.Constantes.DIVISOR;
import static smma.juegosTablero.Constantes.IGUAL;
import static smma.juegosTablero.Constantes.MAYOR;
import static smma.juegosTablero.Constantes.MENOR;

/**
 *
 * @author pedroj
 */
public class RegistroJuegoFinalizado implements Serializable, Comparable<RegistroJuegoFinalizado> {
    
    private final String idJuego;
    private final Grupo grupoJuegos;
    private final RegistroJuego detalleJuego;
    private Predicate resultadoJuego;
    
    // Constantes
    private static final long serialVersionUID = 1L;
    private final int DIA = 1;
    private final int NUM_JUEGO = 2;
    
    public RegistroJuegoFinalizado(String idJuego, Grupo grupoJuegos, RegistroJuego detalleJuego) {
        this.idJuego = idJuego;
        this.grupoJuegos = grupoJuegos;
        this.detalleJuego = detalleJuego;
        this.resultadoJuego = null;
    }
    
    public RegistroJuegoFinalizado(String idJuego) {
        this.idJuego = idJuego;
        this.grupoJuegos = null;
        this.detalleJuego = null;
        this.resultadoJuego = null;
    }
    
    public String getIdJuego() {
        return idJuego;
    }

    public Grupo getGrupoJuegos() {
        return grupoJuegos;
    }

    public RegistroJuego getDetalleJuego() {
        return detalleJuego;
    }
    
    public Predicate getResultadoJuego() {
        return resultadoJuego;
    }
    
    public void setResultadoJuego(Predicate resultadoJuego) {
        this.resultadoJuego = resultadoJuego;
    }
    
    public int getDia() {
        String[] elementos = this.idJuego.split(DIVISOR);
        return Integer.parseInt(elementos[DIA]);
    }
    
    public int getNumJuego() {
        String[] elementos = this.idJuego.split(DIVISOR);
        return Integer.parseInt(elementos[NUM_JUEGO]);
    }
    
    @Override
    public String toString() {
        return "\nRegistroJuegoFinalizado{" + "idJuego= " + idJuego + 
                "\n\tgrupoJuegos= " + grupoJuegos + "\n\tdetalleJuego= " + detalleJuego + 
                "\n\tresultadoJuego= " + resultadoJuego + "}";
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.idJuego);
        hash = 67 * hash + Objects.hashCode(this.resultadoJuego);
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
        final RegistroJuegoFinalizado other = (RegistroJuegoFinalizado) obj;
        if (!Objects.equals(this.idJuego, other.idJuego)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(RegistroJuegoFinalizado other) {
        int resultado;
        
        String[] elmThis = this.idJuego.split(DIVISOR);
        int diaThis = Integer.parseInt(elmThis[DIA]);
        int numJuegoThis = Integer.parseInt(elmThis[NUM_JUEGO]);
        
        String[] elmOther = other.getIdJuego().split(DIVISOR);
        int diaOther = Integer.parseInt(elmOther[DIA]);
        int numJuegoOther = Integer.parseInt(elmOther[NUM_JUEGO]);
        
        if ( diaThis < diaOther ) {
            resultado = MAYOR; 
        } else if ( diaThis > diaOther ) {
            resultado = MENOR;
        } else {
            if ( numJuegoThis < numJuegoOther ) {
                resultado = MAYOR;
            } else if ( numJuegoThis > numJuegoOther ) {
                resultado = MENOR;
            } else {
                resultado = IGUAL;
            }
        }
        
        return resultado;
    }
}
