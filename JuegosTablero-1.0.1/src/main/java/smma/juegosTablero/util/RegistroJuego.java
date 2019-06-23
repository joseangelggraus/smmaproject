/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero.util;

import jade.core.AID;
import jade.util.leap.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.ProponerJuego;
import static smma.juegosTablero.Constantes.NO_HAY_ELEMENTO;

/**
 *
 * @author pedroj
 */
public class RegistroJuego implements Serializable {
    private final ProponerJuego juegoPropuesto;
    private final List<Jugador> listaJugadores;
    
    // Constantes
    private static final long serialVersionUID = 1L;

    public RegistroJuego(ProponerJuego juegoPropuesto) {
        this.juegoPropuesto = juegoPropuesto;
        this.listaJugadores = new ArrayList();
    }

    public ProponerJuego getJuegoPropuesto() {
        return juegoPropuesto;
    }

    public List<Jugador> getListaJugadores() {
        return listaJugadores;
    }
    
    public boolean jugadorAusente(AID agenteJugador) {
        
        Jugador jugador = new Jugador("",agenteJugador);
        return !listaJugadores.contains(jugador);
    }
    
    public boolean jugadorAusente(Jugador jugador) {
        return !listaJugadores.contains(jugador);
    }
    
    public Jugador eliminaJugador(AID agenteJugador) {
        Jugador jugador = null;
        
        Iterator it = listaJugadores.iterator();
        boolean encontrado = false;
        while ( it.hasNext() && !encontrado ) {
            jugador = (Jugador) it.next();
            encontrado = jugador.getAgenteJugador().equals(agenteJugador);
        }
        
        if ( encontrado ) {
            listaJugadores.remove(jugador);
        }
        
        return jugador;
    }
    
    public Jugador eliminarJugador(Jugador jugador) {
        Jugador resultado = null;
        
        int index = listaJugadores.indexOf(jugador);
        if ( index != NO_HAY_ELEMENTO ) {
            resultado = listaJugadores.remove(index);
        }    
        
        return resultado; 
    }

    /**
     * Jugadores registrados para el juego
     * @return número de agentes jugador
     */
    public int numJugadores() {
        return listaJugadores.size();
    }
    
    @Override
    public String toString() {
        return "RegistroJuego{" + "juegoPropuesto=" + juegoPropuesto + ", listaJugadores=" + listaJugadores + '}';
    }
}
