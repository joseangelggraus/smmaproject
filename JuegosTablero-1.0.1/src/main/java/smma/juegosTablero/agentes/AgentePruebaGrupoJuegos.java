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
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ProposeResponder;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import static juegosTablero.Vocabulario.Motivo.JUEGOS_ACTIVOS_SUPERADOS;
import static juegosTablero.Vocabulario.Motivo.TIPO_JUEGO_NO_IMPLEMENTADO;
import static juegosTablero.Vocabulario.NombreServicio.GRUPO_JUEGOS;
import static juegosTablero.Vocabulario.TIPO_SERVICIO;
import static juegosTablero.Vocabulario.getOntologia;
import juegosTablero.dominio.elementos.ClasificacionJuego;
import juegosTablero.dominio.elementos.CompletarJuego;
import juegosTablero.dominio.elementos.Grupo;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import smma.juegosTablero.gui.Consola;
import static smma.juegosTablero.Constantes.AFIRMATIVA;
import static smma.juegosTablero.Constantes.D10;
import smma.juegosTablero.util.GestorSuscripciones;
import smma.juegosTablero.Constantes;

/**
 *
 * @author pedroj
 */
public class AgentePruebaGrupoJuegos extends Agent implements Vocabulario, Constantes {
    // Generador de números aleatorios
    private final Random aleatorio = new Random();
    
    // Para la generación y obtención del contenido de los mensages
    private ContentManager[] manager;
	
    // El lenguaje utilizado por el agente para la comunicación es SL 
    private final Codec codec = new SLCodec();

    // Las ontología que utilizará el agente
    private Ontology[] listaOntologias;
    
    // Constantes
    private final int JUEGOS_IMPLEMENTADOS = 1; //Implementamos dos juegos
    
    // Variables
    private TipoJuego[] listaJuegos;
    private Grupo grupoJuegos;
    private Consola guiConsola;
    private Map<String, List<Jugador>> registroJuegos;
    private TareaInformarJuego finalizacionJuegos;
    private GestorSuscripciones gestor;

    @Override
    protected void setup() {
        // Inicialización variables
        grupoJuegos = new Grupo("pruebaGrupoJuegos-" + getLocalName(), getAID());
        guiConsola = new Consola(this);
        guiConsola.mensaje("Comienza la ejecución " + grupoJuegos);
        registroJuegos = new HashMap();
        
        // Gestor de suscripciones
        gestor = new GestorSuscripciones();
        
        juegosImplementados();
        
        // Registro de las ontologías para los juegos de tablero
        listaOntologias = new Ontology[listaJuegos.length];
        manager = new ContentManager[listaJuegos.length];
        try {
            for ( int i = 0; i < listaJuegos.length; i++ ) {
                listaOntologias[i] = getOntologia(listaJuegos[i]);
                manager[i] = (ContentManager) getContentManager();
                manager[i].registerLanguage(codec);
                manager[i].registerOntology(listaOntologias[i]);
            }
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgenteCentralJuego.class.getName()).log(Level.SEVERE, null, ex);
            this.doDelete();
        }
        
        //Registro en páginas Amarrillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType(TIPO_SERVICIO);
	sd.setName(GRUPO_JUEGOS.name());
	dfd.addServices(sd);
	
        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {
            Logger.getLogger(AgentePruebaGrupoJuegos.class.getName()).log(Level.SEVERE, null, ex);
        }
	
        // Plantilla para la tarea Completar Juego
        MessageTemplate mtCompletarJuego = 
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        addBehaviour(new TareaCompletarJuego(this, mtCompletarJuego));
        
        // Anadimos la tarea para las suscripciones
        // Plantilla del mensaje de suscripción
        MessageTemplate plantilla = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
        finalizacionJuegos = new TareaInformarJuego(this, plantilla, gestor);
        addBehaviour(finalizacionJuegos);
    }

    @Override
    protected void takeDown() {
        // Liberar recursos
        guiConsola.dispose();
        
        //Desregistro de las Páginas Amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            Logger.getLogger(AgentePruebaGrupoJuegos.class.getName()).log(Level.SEVERE, null, ex);
        }
	
        //Se despide
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    
    /**
     * Eliminamos uno de los juegos disponibles al azar para simular las
     * posibles respuesta del agente a un juego no implementado
     */
    private void juegosImplementados() {
        int eliminado = aleatorio.nextInt(TipoJuego.values().length);
        
        listaJuegos = new TipoJuego[JUEGOS_IMPLEMENTADOS];
        
        int j = 0;
        for ( int i = 0; i < TipoJuego.values().length; i++ )
            if ( i != eliminado ) {
                listaJuegos[j] = TipoJuego.values()[i];
                j++;
            }
             
        guiConsola.mensaje("Juegos implementados: " + Arrays.toString(listaJuegos));
    }
    
    private int buscarIndiceJuego(String nombreOntologia) {
        int resultado = NO_HAY_ELEMENTO;
        
        boolean encontrado = false;
        int indice = 0;
        while ( (indice < listaJuegos.length) && !encontrado ) {
            if ( listaOntologias[indice].getName().equals(nombreOntologia) ) {
                encontrado = true;
                resultado = indice;
            } else {
                indice++;
            } 
        }
        
        return resultado;
    }
    
    private int buscarIndiceJuego(TipoJuego tipo) {
        int resultado = NO_HAY_ELEMENTO;
        
        boolean encontrado = false;
        int indice = 0;
        while( (indice < listaJuegos.length) && !encontrado ) {
            if ( listaJuegos[indice].equals(tipo) ) {
                encontrado = true;
                resultado = indice;
            } else {
                indice++;
            }
        }
        
        return resultado;
    }
    
    public boolean aceptarJuego(Juego juego) {
        int tiradaDado = aleatorio.nextInt(D10);
        guiConsola.mensaje("Tirada de dado: " + tiradaDado);
        return tiradaDado < AFIRMATIVA;
    }
    
    public void registrarJuego(String nombreCentral, CompletarJuego completarJuego) {
        List listaJugadores = new ArrayList();
        Iterator it = completarJuego.getListaJugadores().iterator();
        while( it.hasNext() ) {
            // Conversión de la lista de la ontología a lista de datos
            listaJugadores.add((Jugador)it.next());
        }
        registroJuegos.put(completarJuego.getJuego().getIdJuego(), listaJugadores);
        this.addBehaviour(new TareaSimulaClasificacion(this,SIMULA_JUEGO, nombreCentral,
                          completarJuego.getJuego()));
    }
    
    class TareaSimulaClasificacion extends WakerBehaviour {
        private final String nombreCentral;
        private final Juego juego;

        public TareaSimulaClasificacion(Agent a, long timeout, String nombreCentral, Juego juego) {
            super(a, timeout);
            this.nombreCentral = nombreCentral;
            this.juego = juego;
        }

        /**
         * Simula la clasificación y completa la suscripción con el agente
         * central juego
         */
        @Override
        protected void onWake() {
            // Recuperamos el objeto de suscripción
            Subscription suscripcion = gestor.getSuscripcion(nombreCentral);
            
            // Simula Clasificacion
            List<Jugador> listaJugadores = registroJuegos.get(juego.getIdJuego());
            List<Integer> puntosJugadores = new ArrayList();
            for( Jugador jugador : listaJugadores ) {
                int puntos = aleatorio.nextInt(D10);
                puntosJugadores.add(puntos);
            }
            
            // Creamos el mensaje para enviar a los jugadores
            int indiceJuego = buscarIndiceJuego(juego.getTipoJuego());
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setLanguage(codec.getName());
            msg.setOntology(listaOntologias[indiceJuego].getName());
            
            if ( suscripcion == null ) {
                guiConsola.mensaje("No se ha creado bien la suscripcion");
            } else {
                // Creamos la clasificación
                ClasificacionJuego clasificacion = new ClasificacionJuego();
                clasificacion.setJuego(juego);
                clasificacion.setListaJugadores(new jade.util.leap.ArrayList((ArrayList) listaJugadores));
                clasificacion.setListaPuntuacion(new jade.util.leap.ArrayList((ArrayList) puntosJugadores));
                
                 try {
                    manager[indiceJuego].fillContent(msg, clasificacion);
                } catch (Codec.CodecException | OntologyException ex) {
                    guiConsola.mensaje("Error al crear el Informe del Juego \n" + ex);
                }
            
                // Enviamos el mensaje al suscriptor
                suscripcion.notify(msg);
            }
        }
    }
    
    class TareaCompletarJuego extends ProposeResponder {
        
        public TareaCompletarJuego(Agent agente, MessageTemplate mt) {
            super(agente, mt);
        }
        
        @Override
        protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException, RefuseException {
            ACLMessage respuesta = propose.createReply();
            int indiceJuego = buscarIndiceJuego(respuesta.getOntology());
            
            if ( indiceJuego == NO_HAY_ELEMENTO ) {
                guiConsola.mensaje("EXCEPCION " + TIPO_JUEGO_NO_IMPLEMENTADO);
                throw new NotUnderstoodException(TIPO_JUEGO_NO_IMPLEMENTADO.name());   
            } else {
                guiConsola.mensaje("Indice juego " + indiceJuego + " " + listaJuegos[indiceJuego]);
                
                try {
                    Action ac = (Action) manager[indiceJuego].extractContent(propose);
                    CompletarJuego completarJuego = (CompletarJuego) ac.getAction();
                    Juego juego = completarJuego.getJuego();
                    if ( aceptarJuego(juego) ) {
                        // Aceptamos
                        registrarJuego(propose.getSender().getName(), completarJuego);
                        respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        JuegoAceptado juegoAceptado = new JuegoAceptado(juego, grupoJuegos);

                        manager[indiceJuego].fillContent(respuesta, juegoAceptado);
                        
                    } else {
                        // Rechazamos
                        respuesta.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        Motivacion motivacion = new Motivacion(juego, JUEGOS_ACTIVOS_SUPERADOS);
                        
                        manager[indiceJuego].fillContent(respuesta, motivacion);
                    }
                } catch (Codec.CodecException | OntologyException ex) {
                    guiConsola.mensaje("Error en la composición del mensaje \n" + propose + "\n" + ex);
                }
            }
            
            guiConsola.mensaje(respuesta.toString());
            
            return respuesta;
        }
    }
    
    /**
     * Tarea que gestiona la suscripción para informar al agente cental de juegos cuando
     * un juego ha terminado con la clasificación de ese juego
     */
    class TareaInformarJuego extends SubscriptionResponder {
        private SubscriptionResponder.Subscription suscripcionJugador;
        
        public TareaInformarJuego(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        public TareaInformarJuego(Agent a, MessageTemplate mt, SubscriptionResponder.SubscriptionManager sm) {
            super(a, mt, sm);
        }
        
        @Override
        protected ACLMessage handleSubscription(ACLMessage subscription) throws NotUnderstoodException, RefuseException {
            
            String nombreAgente = subscription.getSender().getName();
            
            // Registra la suscripción del Jugador si no hay una previa
            suscripcionJugador = createSubscription(subscription);
            if (!gestor.haySuscripcion(nombreAgente)) {
                mySubscriptionManager.register(suscripcionJugador);            
                guiConsola.mensaje("Suscripción registrada al agente: " +
                        nombreAgente + "\nnúmero de suscripciones: " +
                        gestor.numSuscripciones());
            } else {
                // Ya tenemos una suscripción anterior del jugador y no 
                // volvemos a registrarlo.
                guiConsola.mensaje("Suscripción ya registrada al agente: " +
                        nombreAgente);
            }
            
            // Responde afirmativamente con la operación
            ACLMessage agree = subscription.createReply();
            agree.setPerformative(ACLMessage.AGREE);
            return agree;
        }
        
        @Override
        protected ACLMessage handleCancel(ACLMessage cancel) throws FailureException {
            
            // Eliminamos la suscripción del agente jugador
            String nombreAgente = cancel.getSender().getName();
            suscripcionJugador = gestor.getSuscripcion(nombreAgente);
            mySubscriptionManager.deregister(suscripcionJugador);
                        
            guiConsola.mensaje("Suscripción cancelada del agente: " + 
                    cancel.getSender().getLocalName()+ 
                    "\nsuscripciones restantes: " + gestor.numSuscripciones());
            return null; // no hay que enviar mensaje de confirmación
        }
    }
}
