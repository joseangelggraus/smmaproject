/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero;

/**
 *
 * @author pedroj
 */
public interface Constantes {
    public static final long TIME_OUT = 2000; // 2segundos
    public static final long SALIDA = 3; // 3 segundos
    public static final long RETARDO_PRESENTACION = 2000; // 2 segundos
    public static final long SIMULA_JUEGO = 5000; // 5 segundos
    public static final String DIRECTORIO = "./data/";
    public static final String ARCHIVO = "juegos.dat";
    public static final String DIVISOR = "-";
    public static final int VACIO = 0;
    public static final int NO_HAY_ELEMENTO = -1;
    public static final int PRIMERO = 0;
    public static final int DOS_JUGADORES = 2;
    public static final int CUATRO_JUGADORES = 4;
    public static final int MENOR = -1;
    public static final int IGUAL = 0;
    public static final int MAYOR = 1;
    public static final int D10 = 10;
    public static final int AFIRMATIVA = 7;
    public static final String PREFIJO_ID = "Dyy"; // Formato fecha para el idJuego
    
    public static enum Enfrentamiento {
        MEJOR_UNO(1), MEJOR_TRES(2), MEJOR_CINCO(3);
        
        private final int numVictorias;

        private Enfrentamiento(int numVictorias) {
            this.numVictorias = numVictorias;
        }

        public int victorias() {
            return numVictorias;
        }
    }
}
