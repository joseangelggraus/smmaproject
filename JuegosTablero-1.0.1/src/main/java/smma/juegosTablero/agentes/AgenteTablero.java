/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero.agentes;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.Estado;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.barcos.EstadoJuego;
import juegosTablero.aplicacion.barcos.Localizacion;
import juegosTablero.aplicacion.barcos.MovimientoEntregado;
import juegosTablero.dominio.elementos.ClasificacionJuego;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.PedirMovimiento;
import juegosTablero.dominio.elementos.Posicion;
import smma.juegosTablero.gui.Final;
import smma.juegosTablero.gui.Tablero;

/**
 *
 * @author joseangel
 */
public class AgenteTablero extends Agent {

    //Content Manager
    private final ContentManager manager = (ContentManager) getContentManager();

    //Agent Languaje
    private final Codec codec = new SLCodec();

    //Ontology
    private Ontology ontology;

    //Attributes
    Tablero tablero;
    AID centralJuego;
    Queue<Object> disparos;
    Queue<MovimientoEntregado> jugadasrealizadas;
    Final finPartida;

    Jugador[] jugadores;
    Jugador ganador;
    AID abandono;
    Juego partida;
    int turno;

    String ReproducirPartida;
    boolean fin;

    /**
     * Se crea el AgenteTablero que se encarga de dirigir una partida entre dos
     * jugadores
     *
     * @param central Es el AgenteCentralJuego que lanza este mismo Agente
     * @param _jugadores Son los jugadores de la partida
     * @param _partida Es la partida que hay en juego
     */
    public AgenteTablero(AID central, Jugador[] _jugadores, Juego _partida) {
        jugadores = _jugadores;
        centralJuego = central;
        partida = _partida;

        ReproducirPartida = null;
        fin = false;
    }

    /**
     * Se crea un AgenteTablero que se encarga de dirigir una partida entre dos
     * jugadores
     *
     * @param central Es el AgenteCentralJuego que lanza este mismo Agente
     * @param partida Es la partida que hay en juego
     */
    public AgenteTablero(AID central, String partida) {
        centralJuego = central;

        jugadores = null;
        this.partida = null;

        ReproducirPartida = partida;
        fin = false;
    }

    /**
     * Se lanza al iniciar al agente
     */
    @Override
    public void setup() {
        //inicializamos variables
        tablero = new Tablero(this);
        tablero.setLocationRelativeTo(null);
        disparos = new LinkedList<>();
        jugadasrealizadas = new LinkedList<>();
        turno = 0;

        //Se configura la interfaz
        if (jugadores != null) {
            tablero.setJugador1(jugadores[0].getNombre());
            tablero.setJugador2(jugadores[1].getNombre());
        }

        //Registramos la ontología
        try {
            ontology = OntologiaJuegoBarcos.getInstance();
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgenteCentralJuego.class.getName()).log(Level.SEVERE, null, ex);
        }
        manager.registerLanguage(codec);
        manager.registerOntology(ontology);

        //Se establecen las tareas principales
        addBehaviour(new TareaIniciarPartida());
    }

    @Override
    public void takeDown() {
        tablero.dispose();
        finPartida.dispose();
    }

    public boolean acabado() {
        return fin;
    }

    public void quitarTablero() {
        doWait(5000);
        doDelete();
    }

    //Métodos privados
    private ACLMessage crearMensajeNuevoTurno(MovimientoEntregado jugadaAnterior) {
        //Construimos el mensaje
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        cfp.setSender(this.getAID());
        cfp.setOntology(ontology.getName());
        cfp.setLanguage(codec.getName());

        //Establecemos a los jugadores de la partida como receptores del mensaje
        for (Jugador jugador : jugadores) {
            cfp.addReceiver(jugador.getAgenteJugador());
        }

        //Ontología
        if (jugadaAnterior == null) {
            //Si no ha habido partida anterior, se manda una posición nula
            Posicion posicion = new Posicion(-1, -1);
            jugadaAnterior = new MovimientoEntregado(null, posicion);
        }
        PedirMovimiento pm = new PedirMovimiento(partida, turnoActivoActual());

        //Añadimos el contenido al mensaje
        try {
            Action action = new Action(this.getAID(), pm);
            manager.fillContent(cfp, action);
        } catch (Codec.CodecException | OntologyException ex) {
            Logger.getLogger(AgenteCentralJuego.class.getName()).log(Level.SEVERE, null, ex);
        }
        return cfp;
    }

    private void turnoSiguiente() {
        turno = (turno + 1) % jugadores.length;
    }

    private Jugador turnoActivoActual() {
        return jugadores[turno];
    }

    private Jugador turnoInactivoActual() {
        int aux = (turno + 1) % jugadores.length;
        return jugadores[aux];
    }

    private boolean hayAbandono(Vector responses) {
        Iterator it = responses.iterator();
        while (it.hasNext()) {
            ACLMessage msg = (ACLMessage) it.next();
            if (msg.getPerformative() == ACLMessage.REFUSE) {
                abandono = msg.getSender();
                return true;
            }
        }
        return false;
    }

    //TAREAS
    /**
     * En esta tarea se crea el mensaje con el que se inicia el juego entre dos
     * jugadores
     */
    public class TareaIniciarPartida extends OneShotBehaviour {

        @Override
        public void action() {
            if (partida != null) {
                ACLMessage cfp = crearMensajeNuevoTurno(null);
                addBehaviour(new TareaJugarPartida(myAgent, cfp));
            } else {
                addBehaviour(new TareaCargarPartida());
            }
        }
    }

    /**
     * Esta tarea se encarga de representar de forma visual los movimientos de
     * la partida
     */
    public class TareaRepresentarMovimiento extends TickerBehaviour {

        public TareaRepresentarMovimiento(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            Object obj = disparos.poll();

            if (!fin) {
                if (obj instanceof Localizacion) {
                    Localizacion loc = (Localizacion) obj;
                    tablero.disparo(partida, loc.getPosicion());
                    tablero.cambioTurno();
                } else if (obj instanceof Jugador) {
                    Jugador jug = (Jugador) obj;
                    finPartida = new Final(jug.getNombre(), myAgent);
                    finPartida.setVisible(true);
                    finPartida.setLocationRelativeTo(tablero);
                    fin = true;

                    if (partida != null) {
                        myAgent.addBehaviour(new TareaComunicarResultado());
                    }
                }
            }
        }
    }

    /**
     * Lee una partida almacenada para posteriormente reproducirla
     */
    public class TareaCargarPartida extends OneShotBehaviour {

        @Override
        public void action() {
            try {
                FileReader fr = new FileReader(ReproducirPartida);
                BufferedReader br = new BufferedReader(fr);

                String linea = br.readLine();
                String[] movs = linea.split("/");

                tablero.setJugador1(movs[0]);
                tablero.setJugador2(movs[1]);

                for (int i = 2; i < movs.length - 1; i++) {
                    String[] datos = movs[i].split(",");
                    Posicion pos = new Posicion(Integer.parseInt(datos[1]), Integer.parseInt(datos[2]));
                    Localizacion jugada = new Localizacion();
                    switch (datos[0]) {
                        case "PORTAAVIONES":
                            if (datos[3] == "h") {
                                jugada = new Localizacion(Vocabulario.TipoBarco.PORTAAVIONES, pos, Vocabulario.Orientacion.HORIZONTAL);
                            } else {
                                jugada = new Localizacion(Vocabulario.TipoBarco.PORTAAVIONES, pos, Vocabulario.Orientacion.VERTICAL);
                            }
                            break;

                        case "ACORAZADO":
                            if (datos[3] == "h") {
                                jugada = new Localizacion(Vocabulario.TipoBarco.ACORAZADO, pos, Vocabulario.Orientacion.HORIZONTAL);
                            } else {
                                jugada = new Localizacion(Vocabulario.TipoBarco.ACORAZADO, pos, Vocabulario.Orientacion.VERTICAL);
                            }
                            break;
                        case "DESTRUCTOR":
                            if (datos[3] == "h") {
                                jugada = new Localizacion(Vocabulario.TipoBarco.DESTRUCTOR, pos, Vocabulario.Orientacion.HORIZONTAL);
                            } else {
                                jugada = new Localizacion(Vocabulario.TipoBarco.DESTRUCTOR, pos, Vocabulario.Orientacion.VERTICAL);
                            }
                            break;
                        case "FRAGATA":
                            if (datos[3] == "h") {
                                jugada = new Localizacion(Vocabulario.TipoBarco.FRAGATA, pos, Vocabulario.Orientacion.HORIZONTAL);
                            } else {
                                jugada = new Localizacion(Vocabulario.TipoBarco.FRAGATA, pos, Vocabulario.Orientacion.VERTICAL);
                            }
                            break;
                    }
                    disparos.add(jugada);
                }

                Jugador jug = new Jugador(movs[movs.length - 1], myAgent.getAID());
                disparos.add(jug);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(AgenteTablero.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(AgenteTablero.class.getName()).log(Level.SEVERE, null, ex);
            }

            myAgent.addBehaviour(new TareaRepresentarMovimiento(myAgent, 500));
        }
    }

    /**
     * Comunicará el resultado de la partida al AgenteCentralJuego
     */
    public class TareaComunicarResultado extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
            msg.setSender(myAgent.getAID());
            msg.setOntology(ontology.getName());
            msg.setLanguage(codec.getName());
            msg.addReceiver(centralJuego);

            ClasificacionJuego clasificacion = new ClasificacionJuego();
            jade.util.leap.List listajug = clasificacion.getListaJugadores();
            ganador = (Jugador) listajug.get(0);

            EstadoJuego detalle = new EstadoJuego(partida, Estado.GANADOR);
            try {
                manager.fillContent(msg, detalle);
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteTablero.class.getName()).log(Level.SEVERE, null, ex);
            }

            myAgent.send(msg);
        }

    }

    /**
     * Protocolo de comunicación que se usará entre dos jugadores de una partida
     */
    //REHACER
    public class TareaJugarPartida extends ContractNetInitiator {

        public TareaJugarPartida(Agent a, ACLMessage cfp) {
            super(a, cfp);
            myAgent.addBehaviour(new TareaRepresentarMovimiento(myAgent, 500));
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            //Recogemos el movimiento
            String barco = null;
            Localizacion loc = null;
            ACLMessage activo = null;
            ACLMessage inactivo = null;

            // Recogemos el movimiento que ha hecho el jugador anterior
            if (hayAbandono(responses)) {
                //Si no hay, significa que ha abandonado y se le da la victoria al jugador
                //que no ha abandonado y se manda un mensaje de Reject a los dos
                Iterator it = responses.iterator();

                while (it.hasNext()) {
                    ACLMessage msg = (ACLMessage) it.next();

                    ACLMessage reject = msg.createReply();
                    reject.setPerformative(ACLMessage.REJECT_PROPOSAL);

                    acceptances.add(reject);
                }

                //Vemos quien ha abandonado la partida
                if (turnoActivoActual().getAgenteJugador().getName().equals(abandono.getName())) {
                    ganador = turnoInactivoActual();
                } else if (turnoInactivoActual().getAgenteJugador().getName().equals(abandono.getName())) {
                    ganador = turnoActivoActual();
                }
                disparos.add(ganador);
            } else {
                //Si no hay abandono, distribuimos mensaje

                Iterator it = responses.iterator();
                while (it.hasNext()) {
                    ACLMessage msg = (ACLMessage) it.next();

                    if (msg.getSender().getName().equals(turnoInactivoActual().getAgenteJugador().getName())) {
                        //Jugador inactivo. Acaba el turno
                        ACLMessage reject = msg.createReply();
                        reject.setPerformative(ACLMessage.REJECT_PROPOSAL);

                        MovimientoEntregado me = new MovimientoEntregado();

                        try {
                            manager.fillContent(reject, me);
                        } catch (Codec.CodecException | OntologyException ex) {
                            Logger.getLogger(AgenteTablero.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        acceptances.add(reject);
                    } else if (msg.getSender().getName().equals(turnoActivoActual().getAgenteJugador().getName())) {
                        ACLMessage accept = msg.createReply();
                        accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

                        Posicion pos = new Posicion();
                        MovimientoEntregado me = new MovimientoEntregado(partida, pos);
                        try {
                            manager.fillContent(accept, me);
                        } catch (Codec.CodecException | OntologyException ex) {
                            Logger.getLogger(AgenteTablero.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        acceptances.add(accept);
                    }
                }
            }
        }
        /**
         * REHACER BIEN
         *
         * @Override protected void handleInform(ACLMessage inform){ try{
         * MovimientoEntregado me = (MovimientoEntregado)
         * manager.extractContent(inform);
         *
         * disparos.add(me.getMovimiento()); jugadasrealizadas.add(me);
         *
         * if(me.getJuego().equals(Estado.GANADOR)){ disparos.add(me) }
         *
         * } catch (Codec.CodecException | OntologyException ex) {
         * Logger.getLogger(AgenteTablero.class.getName()).log(Level.SEVERE,
         * null, ex); } }
         */
    }

    /**
     * Tarea encargada de almacenar una partida en un archivo
     */
    public class TareaAlmacenarPartida extends OneShotBehaviour {

        String ruta;
        String partida;

        public TareaAlmacenarPartida(Agent agente) {
            myAgent = agente;
            ruta = "partidas/" + myAgent.getLocalName() + ".pt";
        }

        @Override
        public void action() {
            partida = jugadores[0].getNombre() + "/" + jugadores[1].getNombre() + "/";
            MovimientoEntregado me;
            
            while (!jugadasrealizadas.isEmpty()) {
                me = jugadasrealizadas.poll();

                partida = partida + me.getMovimiento().getCoorX() + ",";
                partida = partida + me.getMovimiento().getCoorY() + "/";

            }
            //Recoger ganador HACER

            File file = new File(ruta);
            try {
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.write(partida);
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(AgenteTablero.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
