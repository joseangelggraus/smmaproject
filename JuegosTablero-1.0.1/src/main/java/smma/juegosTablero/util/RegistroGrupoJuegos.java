/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import juegosTablero.dominio.elementos.Grupo;
import static smma.juegosTablero.Constantes.IGUAL;
import static smma.juegosTablero.Constantes.MAYOR;
import static smma.juegosTablero.Constantes.MENOR;
import static smma.juegosTablero.Constantes.PRIMERO;

/**
 *
 * @author pedroj
 */
public class RegistroGrupoJuegos implements Comparable<RegistroGrupoJuegos> {
    private final Grupo grupoJuegos;
    private final List<RegistroJuegoFinalizado> listaJuegos;

    public RegistroGrupoJuegos(Grupo grupoJuegos) {
        this.grupoJuegos = grupoJuegos;
        this.listaJuegos = new ArrayList();
    }

    public Grupo getGrupoJuegos() {
        return grupoJuegos;
    }

    public int getNumJuegos() {
        return listaJuegos.size();
    }
    
    public void addJuego( RegistroJuegoFinalizado juego ) {
        listaJuegos.add(juego);
    }
    
    public RegistroJuegoFinalizado getJuego() {
        return listaJuegos.get(PRIMERO);
    }
    
    public RegistroJuegoFinalizado getJuego(RegistroJuegoFinalizado juego) {
        int indice = listaJuegos.indexOf(juego);
        return listaJuegos.get(indice);
    }
    

    @Override
    public String toString() {
        return "RegistroGrupoJuegos{" + "grupoJuegos=" + grupoJuegos + ", listaJuegos=" + listaJuegos + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.grupoJuegos);
        hash = 53 * hash + Objects.hashCode(this.listaJuegos);
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
        final RegistroGrupoJuegos other = (RegistroGrupoJuegos) obj;
        if (!Objects.equals(this.grupoJuegos.getAgenteGrupoJuegos(), other.grupoJuegos.getAgenteGrupoJuegos())) {
            return false;
        }
        
        return true;
    }

    /**
     * Se ordenará de menor a mayor número de juegos en la lista
     * @param grupoJuegos
     * @return orden de menor a mayor
     */
    @Override
    public int compareTo(RegistroGrupoJuegos registroGrupo) {
        int resultado;
        
        if ( this.getNumJuegos() < registroGrupo.getNumJuegos() )
            resultado = MAYOR;
        else if ( this.getNumJuegos() == registroGrupo.getNumJuegos() )
            resultado = IGUAL;
        else
            resultado = MENOR;
        
        return resultado;
    }   
}
