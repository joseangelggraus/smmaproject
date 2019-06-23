/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero.agentes;

import jade.content.Concept;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ProposeResponder;
import jade.proto.SubscriptionInitiator;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import static juegosTablero.Vocabulario.Motivo.PARTICIPACION_EN_JUEGOS_SUPERADA;
import static juegosTablero.Vocabulario.getOntologia;
import juegosTablero.aplicacion.barcos.MovimientoEntregado;
import juegosTablero.aplicacion.barcos.ResultadoMovimiento;
import juegosTablero.dominio.elementos.ClasificacionJuego;
import juegosTablero.dominio.elementos.Gestor;
import juegosTablero.dominio.elementos.InformarJuego;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.PedirMovimiento;
import juegosTablero.dominio.elementos.Posicion;
import juegosTablero.dominio.elementos.ProponerJuego;
//import juegosTablero.dominio.elementos.Tablero;
import smma.juegosTablero.gui.Consola;
import smma.juegosTablero.Constantes;
import smma.juegosTablero.gui.Casilla;
import smma.juegosTablero.gui.Tablero;
import smma.juegosTablero.gui.Tablerom;

/**
 *
 * @author pedroj
 */
public class AgentePruebaJugador extends Agent implements Vocabulario, Constantes {

    // Generador de números aleatorios
    private final Random aleatorio = new Random();

    // Para la generación y obtención del contenido de los mensages
    private final ContentManager manager = (ContentManager) getContentManager();

    // El lenguaje utilizado por el agente para la comunicación es SL 
    private final Codec codec = new SLCodec();

    // Las ontología que utilizará el agente
    private Ontology ontologia;

    // Variables
    private TipoJuego tipoJuego;
    private Jugador jugador;
    private Consola guiConsola;
    Map<AID, TareaSuscripcion> centrales;
    Map<String, Partidas> partidas_jugando;
    int aciertos;
    Juego juego;
    Tablerom tablero_jugador, tablero_oponente;
    Casilla[][] tab_jug, tab_oponente;

    @Override
    protected void setup() {
        // Inicialización variables
        guiConsola = new Consola(this);
        seleccionaJuego();
        jugador = new Jugador(this.getLocalName(), this.getAID());
        guiConsola.mensaje("Comienza la ejecución " + jugador);
        aciertos = 0;
        File miDir = new File(".");
        try {
            System.out.println("Directorio actual: " + miDir.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        //Creamos nuestro tablero con los barcos ya colocados
        tablero_jugador = new Tablerom();
        tablero_jugador.colocarBarcos();
        tab_jug = tablero_jugador.getTablero();
        //Nos hacemos un tablero ciego del oponente para ir rellenámndolo sobre la marcha
        //conforme vayamos disparando.
        tablero_oponente = new Tablerom();
        tab_oponente = tablero_oponente.getTablero();

        // Regisro de la Ontología
        try {
            ontologia = getOntologia(tipoJuego);
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgentePruebaJugador.class.getName()).log(Level.SEVERE, null, ex);
        }
        manager.registerLanguage(codec);
        manager.registerOntology(ontologia);

        //Registro en páginas Amarrillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(TIPO_SERVICIO);
        sd.setName(NombreServicio.values()[tipoJuego.ordinal() + 1].name());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {
            Logger.getLogger(AgentePruebaJugador.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Plantilla para la tarea Completar Juego
        MessageTemplate mtProponerJuego
                = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        addBehaviour(new TareaProponerJuego(this, mtProponerJuego));
    }

    @Override
    protected void takeDown() {
        // Liberar recursos
        guiConsola.dispose();

        //Desregistro de las Páginas Amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            Logger.getLogger(AgentePruebaJugador.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Se despide
        System.out.println("Finaliza la ejecución de " + this.getName());
    }

    public class TareaSuscripcion extends SubscriptionInitiator {

        public TareaSuscripcion(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected void handleAgree(ACLMessage agree) {
            System.out.println(myAgent.getLocalName() + ": Suscripción aceptada");
        }

        @Override
        protected void handleInform(ACLMessage inform) {

            if (inform.getSender() != null) {
                System.out.println(myAgent.getLocalName() + ": Inform recibido de " + inform.getSender().getLocalName());
            } else {
                System.out.println(myAgent.getLocalName() + ": Inform recibido de un Sender desconocido.");
            }

            try {
                ProponerJuego detalles = (ProponerJuego) manager.extractContent(inform);
                Concept detalleFinal = detalles.getTipoJuego();

            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgentePruebaJugador.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Seleccionamos el juego de los barcos
     */
    private void seleccionaJuego() {
        tipoJuego = TipoJuego.BARCOS;
    }

    /**
     * Toma la decisión para aceptar el juego
     *
     * @return true si el agente quiere participar del juego
     */
    public boolean aceptarJugar(Juego juego) {
        int tiradaDado = aleatorio.nextInt(D10);
        guiConsola.mensaje("Tirada de dado: " + tiradaDado);
        return tiradaDado < AFIRMATIVA;
    }

    public void registrarJuego(ProponerJuego proponerJuego, AID central) {
        if (centrales.get(central) == null) {
            ACLMessage suscribe = new ACLMessage(ACLMessage.SUBSCRIBE);
            suscribe.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);

            suscribe.setSender(this.getAID());
            suscribe.setLanguage(codec.getName());
            suscribe.setOntology(ontologia.getName());
            suscribe.addReceiver(central);

            //Metemos el contenido del mensaje
            Gestor gestor = new Gestor("central", central);
            InformarJuego info = new InformarJuego(gestor);
            Action action = new Action(this.getAID(), info);

            try {
                manager.fillContent(suscribe, action);
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgentePruebaJugador.class.getName()).log(Level.SEVERE, null, ex);
            }

            //Creamos la tarea y la añadimos a la lista de tareas
            AgentePruebaJugador.TareaSuscripcion tarea = new AgentePruebaJugador.TareaSuscripcion(this, suscribe);
            centrales.put(central, tarea);
            this.addBehaviour(tarea);
        }
    }

    class TareaProponerJuego extends ProposeResponder {

        public TareaProponerJuego(Agent agente, MessageTemplate mt) {
            super(agente, mt);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException, RefuseException {
            ACLMessage respuesta = propose.createReply();

            try {
                Action ac = (Action) manager.extractContent(propose);
                ProponerJuego proponerJuego = (ProponerJuego) ac.getAction();
                juego = proponerJuego.getJuego();
                if (aceptarJugar(juego)) {
                    // Decisión afirmativa
                    registrarJuego(proponerJuego, propose.getSender());
                    respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    JuegoAceptado juegoAceptado = new JuegoAceptado(juego, jugador);
                    manager.fillContent(respuesta, juegoAceptado);
                } else {
                    // Decisión negativa
                    respuesta.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    Motivacion motivacion = new Motivacion(juego, PARTICIPACION_EN_JUEGOS_SUPERADA);
                    manager.fillContent(respuesta, motivacion);
                }
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgentePruebaJugador.class.getName()).log(Level.SEVERE, null, ex);
            }

            guiConsola.mensaje(respuesta.toString());

            return respuesta;
        }
    }

    private class Partidas {

        ArrayList<Posicion> posiciones_tocados = new ArrayList();
        boolean victoriaAlcanzada = false;

        public Estado verEstado(Casilla[][] tablero) {
            if (aciertos == 21) { //21 es el número de partes en las que se componen la totalidad de los barcos
                victoriaAlcanzada = true;
                return Estado.GANADOR;
            }

            return Estado.SEGUIR_JUGANDO;
        }

        public Integer nAciertos() {
            return aciertos;
        }

        private ArrayList<Posicion> posiblesJugadas() {
            MovimientoEntregado mov = new MovimientoEntregado();
            Casilla[][] tab_aux = tab_oponente.clone();

            //Sabemos las posiciones que hemos tocado para atacar a sus alrededores hasta hundirlo
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (tab_aux[i][j].getDisparo()) {
                        Posicion pos = new Posicion(i, j);
                        posiciones_tocados.add(pos);
                    }
                }
            }
            return posiciones_tocados;
        }

        public void realizarDisparo() {
            ResultadoMovimiento Resdisparo = new ResultadoMovimiento();
            MovimientoEntregado disparo = new MovimientoEntregado();
            disparo.setJuego(juego);
            Resdisparo.setJuego(juego);
            if (!posiciones_tocados.isEmpty()) {
                Posicion aComprobar = posiciones_tocados.get(0);

                if (aComprobar.getCoorX() == 0 && aComprobar.getCoorY() == 0) {
                    if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() + 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() + 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
                        

                    } else if (tab_oponente[aComprobar.getCoorX() + 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() + 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
                    }
                } else if (aComprobar.getCoorX() == 0 && aComprobar.getCoorY() == 9) {
                    if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() - 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() - 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() + 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() + 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
                    }
                } else if (aComprobar.getCoorX() == 9 && aComprobar.getCoorY() == 0) {
                    if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() + 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() + 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() - 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() - 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
                    }
                } else if (aComprobar.getCoorX() == 9 && aComprobar.getCoorY() == 9) {
                    if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() - 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() - 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() - 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() - 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
                    }
                } else if (aComprobar.getCoorX() == 0) {
                    if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() - 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() - 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() + 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() + 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() + 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() + 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
                    }
                } else if (aComprobar.getCoorX() == 9) {
                    if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() - 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() - 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() + 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() + 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() - 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() - 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
                    }
                } else if (aComprobar.getCoorY() == 0) {
                    if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() + 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() + 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() - 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() - 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() + 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() + 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
                    }
                } else if (aComprobar.getCoorY() == 9) {
                    if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() - 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() - 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() - 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() - 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() + 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() + 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
                    }
                } else {
                    if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() - 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() - 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX()][aComprobar.getCoorY() + 1].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX(), aComprobar.getCoorY() + 1);
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() - 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() - 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());

                    } else if (tab_oponente[aComprobar.getCoorX() + 1][aComprobar.getCoorY()].getnCasilla().equals("")) {
                        Posicion posDisparo = new Posicion(aComprobar.getCoorX() + 1, aComprobar.getCoorY());
                        disparo = new MovimientoEntregado(juego, posDisparo);
                        disparo.setMovimiento(posDisparo);
                        Resdisparo.setMovimiento(posDisparo);
                        Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
                    }
                }
            } else {
                Random rand = new Random();
                Posicion posDisparo = new Posicion(rand.nextInt(9), rand.nextInt(9));
                disparo = new MovimientoEntregado(juego, posDisparo);
                disparo.setMovimiento(posDisparo);
                Resdisparo.setMovimiento(posDisparo);
                Resdisparo.setResultado(tablero_oponente.disparo(juego, posDisparo).getResultado());
            }
            
            if (Resdisparo.getResultado().equals(Efecto.TOCADO)) {
                aciertos++;
            }
            PedirMovimiento pm = new PedirMovimiento(juego, jugador);
        }
        
    }
}
