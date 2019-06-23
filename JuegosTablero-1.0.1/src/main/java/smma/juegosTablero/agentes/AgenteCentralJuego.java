/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smma.juegosTablero.agentes;

import jade.content.Concept;
import jade.content.ContentManager;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFSubscriber;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ProposeInitiator;
import jade.proto.SubscriptionInitiator;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import juegosTablero.Vocabulario;
import static juegosTablero.Vocabulario.ModoJuego.TORNEO;
import static juegosTablero.Vocabulario.ModoJuego.UNICO;
import static juegosTablero.Vocabulario.NombreServicio.GRUPO_JUEGOS;
import static juegosTablero.Vocabulario.TipoJuego.DOMINO;
import juegosTablero.aplicacion.barcos.JuegoBarcos;
import juegosTablero.aplicacion.conecta4.JuegoConecta4;
import juegosTablero.aplicacion.domino.JuegoDomino;
import juegosTablero.dominio.elementos.ClasificacionJuego;
import juegosTablero.dominio.elementos.Gestor;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.ProponerJuego;
import smma.juegosTablero.gui.Consola;
import smma.juegosTablero.gui.JuegosTableroJFrame;
import static juegosTablero.Vocabulario.getOntologia;
import juegosTablero.dominio.elementos.CompletarJuego;
import juegosTablero.dominio.elementos.Grupo;
import juegosTablero.dominio.elementos.IncidenciaJuego;
import juegosTablero.dominio.elementos.InformarJuego;
import smma.juegosTablero.gui.AgentesJuegoJFrame;
import smma.juegosTablero.gui.ClasificacionJuegoJFrame;
import smma.juegosTablero.util.RegistroGrupoJuegos;
import smma.juegosTablero.util.RegistroJuego;
import smma.juegosTablero.util.RegistroJugador;
import smma.juegosTablero.Constantes;
import static smma.juegosTablero.Constantes.ARCHIVO;
import static smma.juegosTablero.Constantes.DIRECTORIO;
import static smma.juegosTablero.Constantes.PREFIJO_ID;
import static smma.juegosTablero.Constantes.PRIMERO;
import static smma.juegosTablero.Constantes.SALIDA;
import smma.juegosTablero.util.RegistroJuegoFinalizado;

/**
 *
 * @author pedroj
 */
public class AgenteCentralJuego extends Agent implements Constantes, Vocabulario {

    // Generador de números aleatorios
    private final Random aleatorio = new Random();

    // Para la generación y obtención del contenido de los mensages
    private ContentManager[] manager;

    // El lenguaje utilizado por el agente para la comunicación es SL 
    private final Codec codec = new SLCodec();

    // Las ontología que utilizará el agente
    private Ontology[] listaOntologias;

    // Servicios y juegos
    private NombreServicio[] tiposAgentes;
    private TipoJuego[] listaJuegos;

    // Variables agente
    private String diaJuego;
    private int numJuego;
    private Gestor agenteCentralJuego;
    private List<AID>[] listaAgentes;
    private List<RegistroGrupoJuegos>[] registroGrupoJuegos;
    private List<RegistroJugador>[] registroJugadores;
    private Map<String, RegistroJuego> registroJuegos;
    private List<RegistroJuego> juegosPendientes;
    private List<RegistroJuegoFinalizado> juegosFinalizados;
    private int[] minJugadores;
    private List<TipoJuego> tipoJuegosActivos;

    // GUI del agente
    private Consola guiConsola;
    private JuegosTableroJFrame guiAgente;
    private AgentesJuegoJFrame guiAgentesJuego;

    @Override
    protected void setup() {
        // Inicialización de las variables
        agenteCentralJuego = new Gestor("Agente Central Juego", this.getAID());
        guiConsola = new Consola(this);
        guiAgente = new JuegosTableroJFrame(this);
        guiConsola.mensaje("Comienza la ejecución " + agenteCentralJuego);
        guiAgentesJuego = new AgentesJuegoJFrame(this);

        tiposAgentes = NombreServicio.values();
        listaJuegos = TipoJuego.values();
        tipoJuegosActivos = new ArrayList();
        registroJuegos = new HashMap();
        juegosPendientes = new ArrayList();

        // Tipos de Agentes
        listaAgentes = new List[tiposAgentes.length];
        for (NombreServicio servicio : tiposAgentes) {
            listaAgentes[servicio.ordinal()] = new ArrayList();
        }

        registroGrupoJuegos = new List[listaJuegos.length];
        registroJugadores = new List[listaJuegos.length];
        minJugadores = new int[listaJuegos.length];
        for (TipoJuego tipo : listaJuegos) {
            registroGrupoJuegos[tipo.ordinal()] = new ArrayList();
            registroJugadores[tipo.ordinal()] = new ArrayList();
            if (tipo.equals(DOMINO)) {
                minJugadores[tipo.ordinal()] = CUATRO_JUGADORES;
            } else {
                minJugadores[tipo.ordinal()] = DOS_JUGADORES;
            }
        }

        // Registro de las ontologías para los juegos de tablero
        listaOntologias = new Ontology[listaJuegos.length];
        manager = new ContentManager[listaJuegos.length];
        try {
            for (int i = 0; i < listaJuegos.length; i++) {
                listaOntologias[i] = getOntologia(listaJuegos[i]);
                manager[i] = (ContentManager) getContentManager();
                manager[i].registerLanguage(codec);
                manager[i].registerOntology(listaOntologias[i]);
            }
        } catch (BeanOntologyException ex) {
            guiConsola.mensaje("Error al registrar la ontología \n" + ex);
            this.doDelete();
        }
        
        try {
            // Cargar los datos de juegos finalizados anteriormente
            cargarDatos();
        } catch (Exception ex) {
            numJuego = 1;
            juegosFinalizados = new ArrayList();
            guiConsola.mensaje("Inicializamos el registro de juegos finalizados\n" + ex 
                    + "\n\tdia " + diaJuego + " numJuego " + numJuego + "\n\t" + juegosFinalizados);
        }

        // Añadir tareas principales
        // Suscripción a las páginas amarillas para saber los agentes del juego
        // que se registran o finalizan el registro
        DFAgentDescription template = new DFAgentDescription();
	ServiceDescription templateSd = new ServiceDescription();
        templateSd.setType(TIPO_SERVICIO);
        template.addServices(templateSd);
        addBehaviour(new TareaSuscripcionDF(this,template)); 
        
    }

    @Override
    protected void takeDown() {
        // Liberación de los recursos del agente
        guiConsola.mensaje("Finalizamos la ejecución del agente " + this.getLocalName() +
                " en " + SALIDA + " segundos");
        try {
            // Almacenar los datos de juegos finalizados
            guardarDatos();
            
            // Esperamos un momento antes de finalizar
            TimeUnit.SECONDS.sleep(SALIDA);
        } catch (Exception ex) {
            guiConsola.mensaje("Error al guardar los datos de juegos finalizados\n" + ex);
        } finally { 
            guiConsola.dispose();
            guiAgente.dispose();
            guiAgentesJuego.dispose();
            System.out.println("Finaliza la ejecución de " + this.getName());
        }
    }
    
    private void cargarDatos() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(PREFIJO_ID);
        diaJuego = sdf.format(new Date()).toString();
        int dia = Integer.parseInt(diaJuego);
        
        File directorio = new File(DIRECTORIO);
        File archivo = new File(DIRECTORIO+ARCHIVO);
        
        
        if ( archivo.exists() ) {
            FileInputStream fis = new FileInputStream(archivo);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            juegosFinalizados = (List<RegistroJuegoFinalizado>) ois.readObject();
            ois.close();
            
            if ( juegosFinalizados != null ) {
                Collections.sort(juegosFinalizados);
                RegistroJuegoFinalizado primero = juegosFinalizados.get(PRIMERO);
                if ( dia == primero.getDia() ) {
                    numJuego = primero.getNumJuego() + 1;
                } else {
                    numJuego = 1;
                }
                guiAgente.activaReproduccion(juegosFinalizados);
                guiConsola.mensaje("El último almacenado " + primero.getDia() + " el día de hoy: " + dia +
                        " el siguiente juego para el día es: " + numJuego +
                        "\ndivision " + Arrays.toString(primero.getIdJuego().split(DIVISOR)));
            } else {
                throw new Exception("Inicialización");
            }
        } else {
            directorio.mkdir();
            throw new Exception("Inicialización");
        }
    }
    
    private void guardarDatos() throws Exception {
        File archivo = new File(DIRECTORIO+ARCHIVO);
        
        FileOutputStream fos = new FileOutputStream(archivo);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        
        oos.writeObject(juegosFinalizados);
        oos.close();
        guiConsola.mensaje("Se ha guardado la información de los juegos finalizados");
    }

    private void addAgentesJugador(ACLMessage msg, Juego juego, int numAgentes) {
        AID agenteJugador;
        
        RegistroJuego juegoActual = registroJuegos.get(juego.getIdJuego());
        TipoJuego tipoJuego = juego.getTipoJuego();
        ModoJuego modo = juego.getModoJuego();
        List<RegistroJugador> listaJugadores = registroJugadores[tipoJuego.ordinal()];
        Iterator it = listaAgentes[tipoJuego.ordinal() + 1].iterator();
        
        if ( modo.equals(TORNEO) ) {
            while ( it.hasNext() ) {
                agenteJugador = (AID) it.next();
                if ( juegoActual.jugadorAusente(agenteJugador) ) {
                    msg.addReceiver(agenteJugador);
                }
            }
        } else {
            int encontrados = 0;
            while ( (encontrados < numAgentes) && it.hasNext() ) {
                // Primero añadimos agentes no contactados previamente
                agenteJugador = (AID) it.next();
                RegistroJugador registroJugador = new RegistroJugador(new Jugador("",agenteJugador));
                int indice = listaJugadores.indexOf(registroJugador);
                if ( (indice == NO_HAY_ELEMENTO) && juegoActual.jugadorAusente(agenteJugador) ) {
                    msg.addReceiver(agenteJugador);
                    encontrados++;
                }
            }
            
            // Completamos con agentes ya contactados previamente
            // Priorizando los que menos juegos han aceptado
            Collections.sort(listaJugadores);
            numAgentes = numAgentes - encontrados;
            it = listaJugadores.iterator();
            while ( it.hasNext() && (encontrados < numAgentes) ) {
                RegistroJugador registroJugador = (RegistroJugador) it.next();
                if ( juegoActual.jugadorAusente(registroJugador.getJugador()) ) {
                    msg.addReceiver(registroJugador.getJugador().getAgenteJugador());
                    encontrados++;
                }
            }
        }
    }

    private void addRegistroJuego(ProponerJuego proponerJuego) {
        String idJuego = proponerJuego.getJuego().getIdJuego();

        if (!registroJuegos.containsKey(idJuego)) {
            registroJuegos.put(idJuego, new RegistroJuego(proponerJuego));
        }
    }

    /**
     * El juego queda pendiente para localizar los agentes que fantan más
     * adelante
     * @param juego 
     */
    private void addJuegoPendiente(Juego juego) {
        RegistroJuego registroJuego = registroJuegos.remove(juego.getIdJuego());
        juegosPendientes.add(registroJuego);
        guiAgente.activaPendiente(juegosPendientes);
    }
    
    /**
     * Elimina el agente jugador de los juegos que han sido propuestos y aceptados
     * por el agente jugador
     * @param agenteJugador 
     */
    private void eliminarJugador(AID agenteJugador) {
        Iterator it;
        Jugador jugador;
        
        // Lo eliminamos de los juegos pendientes
        for( RegistroJuego pendiente : juegosPendientes ) {
            jugador = pendiente.eliminaJugador(agenteJugador);
            if ( jugador != null ) {
                TipoJuego tipo = pendiente.getJuegoPropuesto().getJuego().getTipoJuego();
                registroJugadores[tipo.ordinal()].remove(new RegistroJugador(jugador));
                guiConsola.mensaje("Jugador eliminado: " + jugador + 
                            " del juego " + pendiente + "\n" + registroJugadores[tipo.ordinal()] +
                            "\njugadores que quedan " + pendiente.numJugadores());
            }
                
        } 
        
        // Comprobamos también los que están para completar
        it = registroJuegos.values().iterator();
        while ( it.hasNext() ) {
            RegistroJuego juegoCompleto = (RegistroJuego) it.next();
            String idJuego = juegoCompleto.getJuegoPropuesto().getJuego().getIdJuego();
            TipoJuego tipo = juegoCompleto.getJuegoPropuesto().getJuego().getTipoJuego();
            jugador = juegoCompleto.eliminaJugador(agenteJugador);
            if ( jugador != null ) {
                registroJugadores[tipo.ordinal()].remove(new RegistroJugador(jugador));
                guiConsola.mensaje("Jugador eliminado: " + jugador + 
                                " del juego " + juegoCompleto + "\n" + registroJugadores[tipo.ordinal()] +
                                "\njugadores que quedan " + juegoCompleto.numJugadores());
                
                // Comprobamos que le queden el mínimo de jugadores
                if ( juegoCompleto.numJugadores() < minJugadores[tipo.ordinal()] ) {
                    // El juego pasa a pendientes por no tener el mínimo de jugadores
                    juegosPendientes.add(juegoCompleto);
                    it.remove(); // Lo eliminamos del registro de juegos 
                    guiConsola.mensaje("El juego " + idJuego + "\nno tiene el mínimo de jugadores");
                }
            }
        }
        
        guiAgente.activaPendiente(juegosPendientes);
        guiAgente.activaCompletarJuego(registroJuegos.keySet());
    }
    
    /**
     * Contacta con nuevos jugadores para completar la lista de jugadores con 
     * el mínumo necesario para un juego que no alcanzó ese mínimo cuando se intentó
     * anteriormente.
     * @param index 
     */
    public void juegoPendiente(int index) {
        RegistroJuego pendiente = juegosPendientes.remove(index);
        ProponerJuego proponerJuego = pendiente.getJuegoPropuesto();
        Juego juego = proponerJuego.getJuego();
        TipoJuego tipoJuego = juego.getTipoJuego();
        int numJugadores = minJugadores[tipoJuego.ordinal()] - pendiente.numJugadores();
        guiConsola.mensaje("Se vuelve a proponer el juego pendiente\n" + pendiente + 
                            "\nbuscamos " + numJugadores + " nuevos jugadores");
        
        // Registramos el juego
        registroJuegos.put(juego.getIdJuego(), pendiente);
        
        ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        msg.setSender(getAID());
        addAgentesJugador(msg, juego, numJugadores);
        msg.setLanguage(codec.getName());
        msg.setOntology(listaOntologias[tipoJuego.ordinal()].getName());
        msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_OUT));
        
        if ( msg.getAllReceiver().hasNext() ) {
            Action ac = new Action(this.getAID(), proponerJuego);
        
            try {
                manager[tipoJuego.ordinal()].fillContent(msg, ac);
            } catch (Codec.CodecException | OntologyException ex) {
                guiConsola.mensaje("Error en la construcción del mensaje en Proponer Juego \n" + ex);
            }

            // Creamos la tarea para buscar jugadores
            addBehaviour(new TareaProponerJuego(this, msg, pendiente, minJugadores[tipoJuego.ordinal()]));
            guiConsola.mensaje(msg.toString());
        } else {
            // No hay agentes disponibles para el juego
            guiConsola.mensaje("No hay jugadores para " + juego);
            addJuegoPendiente(juego);
        } 
    }
    
    /**
     * Elimina de la lista de juegos pendientes de completar el que se indica en el 
     * índice
     * @param index 
     */
    public void eliminarJuego(int index) {
        RegistroJuego pendiente = juegosPendientes.remove(index);
        guiConsola.mensaje("Se ha eliminado el juego pendiente\n" + pendiente);
    }
    
    private void addRegistroJugador(Juego juego, Jugador jugador) {
        String idJuego = juego.getIdJuego();
        TipoJuego tipoJuego = juego.getTipoJuego();
        List listaAgentes = registroJuegos.get(idJuego).getListaJugadores();

        listaAgentes.add(jugador);
        RegistroJugador registroJugador = new RegistroJugador(jugador);
        int indice = registroJugadores[tipoJuego.ordinal()].indexOf(registroJugador);
        if (indice == NO_HAY_ELEMENTO) {
            // Es la primera aceptación para participar en un juego de este jugador
            registroJugadores[tipoJuego.ordinal()].add(registroJugador);
        } else {
            registroJugador = registroJugadores[tipoJuego.ordinal()].get(indice);
            registroJugador.addJuego();
        }
        guiConsola.mensaje(registroJugadores[tipoJuego.ordinal()].toString());
    }

    private void addRegistroGrupoJuegos(Grupo grupoJuego, Juego juego) {
        TipoJuego tipoJuego = juego.getTipoJuego();
        RegistroGrupoJuegos registroGrupo = new RegistroGrupoJuegos(grupoJuego);
        int indice = registroGrupoJuegos[tipoJuego.ordinal()].indexOf(registroGrupo);
        if (indice == NO_HAY_ELEMENTO) {
            // No se ha contactado antes con este agente de grupo de juegos
            registroGrupoJuegos[tipoJuego.ordinal()].add(registroGrupo);
            addSuscripcion(grupoJuego.getAgenteGrupoJuegos(),juego.getTipoJuego());
        }
        
        // Eliminamos el juego del registro pendiente y espera a que sea completado
        String idJuego = juego.getIdJuego();
        RegistroJuego detalleJuego = registroJuegos.remove(idJuego);
        indice = juegosFinalizados.indexOf(new RegistroJuegoFinalizado(idJuego));
        if ( indice == NO_HAY_ELEMENTO ) {
            RegistroJuegoFinalizado juegoActivo = new RegistroJuegoFinalizado(idJuego, grupoJuego, detalleJuego);
            juegosFinalizados.add(juegoActivo);
        }
    }
    
    /**
     * Comprueba si el agente central tiene localizado al agente que representa
     * al grupo de juego.
     * 
     * @param grupoJuego
     * @param tipoJuego
     * @return true si el grupo de juego está presente
     */
    public boolean disponibleGrupoJuego(Grupo grupoJuego, TipoJuego tipoJuego) {
        boolean resultado = false;
        
        int indice = registroGrupoJuegos[tipoJuego.ordinal()].indexOf(grupoJuego);
        
        guiConsola.mensaje("Grupo Juego " + grupoJuego + " encontrado " + indice);
        
        if ( indice != NO_HAY_ELEMENTO ) {
            resultado = true;
        }
        
        return resultado;
    }
    
    private void addResultadoJuego(Juego juego, Predicate elemento) {
        int indice;
        String idJuego = juego.getIdJuego();
        
        indice = juegosFinalizados.indexOf(new RegistroJuegoFinalizado(idJuego));
        if ( indice == NO_HAY_ELEMENTO ) {
            guiConsola.mensaje("ERROR: no se ha encontrado el juego " + juego);
        } else if ( juegosFinalizados.get(indice).getResultadoJuego() != null ) {
            guiConsola.mensaje("Finalizada la REPRODUCCION del juego " + idJuego);
        } else {  
            juegosFinalizados.get(indice).setResultadoJuego(elemento);
            guiAgente.activaReproduccion(juegosFinalizados);
            guiConsola.mensaje("Se completa el juego " + idJuego + " con el resultado");
        }
    }

    /**
     * Controla el botón para completar juegos
     */
    private void juegosDisponibles() {
        if (!registroJuegos.isEmpty() && (listaAgentes[GRUPO_JUEGOS.ordinal()].size() != VACIO)) {
            guiAgente.activaCompletarJuego(registroJuegos.keySet());
        } else {
            guiAgente.anulaCompletarJuego(registroJuegos.keySet());
        }
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
    
    public void proponerJuego(TipoJuego tipoJuego, ModoJuego modoJuego, int victorias) {
        ProponerJuego proponerJuego;
        Juego juego;
        
        String idJuego = tipoJuego.name() + "-" + diaJuego + "-"+ numJuego;
        proponerJuego = new ProponerJuego();
        juego = new Juego(idJuego, victorias, modoJuego, tipoJuego);
        proponerJuego.setJuego(juego);
        switch (tipoJuego) { // Condiciones estandar para el juego
            case BARCOS:
                proponerJuego.setTipoJuego(new JuegoBarcos());
                break;
            case CONECTA_4:
                proponerJuego.setTipoJuego(new JuegoConecta4());
                break;
            case DOMINO:
                proponerJuego.setTipoJuego(new JuegoDomino());
                break;
        }
        
        // Registramos el juego
        addRegistroJuego(proponerJuego);
        numJuego++;

        ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        msg.setSender(getAID());
        addAgentesJugador(msg, juego, minJugadores[tipoJuego.ordinal()]);
        msg.setLanguage(codec.getName());
        msg.setOntology(listaOntologias[tipoJuego.ordinal()].getName());
        msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_OUT));

        if ( msg.getAllReceiver().hasNext() ) {
            Action ac = new Action(this.getAID(), proponerJuego);
        
            try {
                manager[tipoJuego.ordinal()].fillContent(msg, ac);
            } catch (Codec.CodecException | OntologyException ex) {
                guiConsola.mensaje("Error en la construcción del mensaje en Proponer Juego \n" + ex);
            }

            // Creamos la tarea para buscar jugadores
            addBehaviour(new TareaProponerJuego(this, msg, registroJuegos.get(idJuego), minJugadores[tipoJuego.ordinal()]));
            guiConsola.mensaje(msg.toString());
        } else {
            // No hay agentes disponibles para el juego
            guiConsola.mensaje("No hay jugadores para " + juego);
            addJuegoPendiente(juego);
        } 
    }
    
    public void reproducirJuego(int indiceJuego) {
        RegistroJuegoFinalizado finalizado = juegosFinalizados.get(indiceJuego);
        RegistroJuego juegoRegistrado = finalizado.getDetalleJuego();
        Grupo grupoJuego = finalizado.getGrupoJuegos();
        Juego juego = juegoRegistrado.getJuegoPropuesto().getJuego();
        TipoJuego tipoJuego = juego.getTipoJuego();
        Concept condicionesJuego = juegoRegistrado.getJuegoPropuesto().getTipoJuego();
        List listaJugadores = juegoRegistrado.getListaJugadores();
        CompletarJuego completarJuego = new CompletarJuego(juego, condicionesJuego, 
                                                     new jade.util.leap.ArrayList((ArrayList) listaJugadores));
        
        // Creamos el mensaje
        ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        msg.setSender(getAID());
        msg.setLanguage(codec.getName());
        msg.setOntology(listaOntologias[tipoJuego.ordinal()].getName());
        AID agenteGrupo = grupoJuego.getAgenteGrupoJuegos();
        if( listaAgentes[GRUPO_JUEGOS.ordinal()].contains(agenteGrupo) ) {
            msg.addReceiver(agenteGrupo);
        }
        msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_OUT));
        
        if ( msg.getAllReceiver().hasNext() ) {
            Action ac = new Action(this.getAID(), completarJuego);
            
            try {
                manager[tipoJuego.ordinal()].fillContent(msg, ac);
            } catch (Codec.CodecException | OntologyException ex) {
                guiConsola.mensaje("Error en la construcción del mensaje en Completar Juego \n" + ex);
            }

            guiConsola.mensaje("Reproducir " + msg);
            addBehaviour(new TareaCompletarJuego(this, msg, juego, true));
        } else {
            // No hay agentes disponibles para el juego
            guiConsola.mensaje("No está disponible el grupo de juego: " + grupoJuego + 
                    " para reproducir el juego: " + juego);
        }
        
    } 

    public void completarJuego(int indiceAgente, String idJuego) {
        RegistroJuego juegoRegistrado = registroJuegos.get(idJuego);
        Juego juego = juegoRegistrado.getJuegoPropuesto().getJuego();
        TipoJuego tipoJuego = juego.getTipoJuego();
        Concept condicionesJuego = juegoRegistrado.getJuegoPropuesto().getTipoJuego();
        List listaJugadores = juegoRegistrado.getListaJugadores();
        CompletarJuego completarJuego = new CompletarJuego(juego, condicionesJuego, 
                                                     new jade.util.leap.ArrayList((ArrayList) listaJugadores));
            
        // Creamos el mensaje
        ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        msg.setSender(getAID());
        msg.setLanguage(codec.getName());
        msg.setOntology(listaOntologias[tipoJuego.ordinal()].getName());
        msg.addReceiver(listaAgentes[GRUPO_JUEGOS.ordinal()].get(indiceAgente));
        msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_OUT));

        if ( msg.getAllReceiver().hasNext() ) {
            Action ac = new Action(this.getAID(), completarJuego);
            
            try {
                manager[tipoJuego.ordinal()].fillContent(msg, ac);
            } catch (Codec.CodecException | OntologyException ex) {
                guiConsola.mensaje("Error en la construcción del mensaje en Completar Juego \n" + ex);
            }

            guiConsola.mensaje(msg.toString());
            addBehaviour(new TareaCompletarJuego(this, msg, juego, false));
        } else {
            // No hay agentes disponibles para el juego
            guiConsola.mensaje("No hay grupo de juegos posibles para completar: " + juego);
        }
    }

    /**
     * Añade la suscripción para recibir las calsificaciones de los juegos que
     * completará el agente grupo de juegos.
     *
     * @param agenteGrupoJuegos
     */
    private void addSuscripcion(AID agente, TipoJuego tipo) {
        InformarJuego informarJuego = new InformarJuego(agenteCentralJuego);
        
        //Creamos el mensaje para lanzar el protocolo Subscribe
        ACLMessage msg = new ACLMessage(ACLMessage.SUBSCRIBE);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
        msg.setSender(this.getAID());
        msg.setLanguage(codec.getName());
        msg.setOntology(listaOntologias[tipo.ordinal()].getName());
        msg.addReceiver(agente);
        
        Action ac = new Action(this.getAID(), informarJuego);
        
        try {
            manager[tipo.ordinal()].fillContent(msg, ac);
        } catch (Codec.CodecException | OntologyException ex) {
            guiConsola.mensaje("Error en la construcción del mensaje en Informar Juego \n" + ex);
        }
        
        // Añadimos la tarea de suscripción
        addBehaviour(new TareaInformarJuego(this,msg));
    }
    
    /**
     * Tarea para suscripción a las páginas aparillas y que el agente DF notifique
     * el registro de un agente relacionado con los Juegos de Tablero
     */
    class TareaSuscripcionDF extends DFSubscriber {

        public TareaSuscripcionDF(Agent a, DFAgentDescription template) {
            super(a, template);
        }

        /**
         * Cada vez que se registra un agente del juego se activa y se presentará
         * en guiAgentesJuego
         * @param dfad agente que entra al registro
         */
        @Override
        public void onRegister(DFAgentDescription dfad) {
            Iterator it = dfad.getAllServices();
            while ( it.hasNext() ) {
                ServiceDescription sd = (ServiceDescription) it.next();
                for ( NombreServicio servicio : tiposAgentes ) {
                    if ( sd.getName().equals(servicio.name()) ) {
                        listaAgentes[servicio.ordinal()].add(dfad.getName());
                        guiAgentesJuego.addAgente(listaAgentes[servicio.ordinal()], servicio);
                        
                        if ( !servicio.equals(GRUPO_JUEGOS) && 
                             (listaAgentes[servicio.ordinal()].size() >= minJugadores[servicio.ordinal() - 1]) &&
                             !tipoJuegosActivos.contains(listaJuegos[servicio.ordinal() - 1]) ) {
                            // Juegos que tienen el mínimo de jugadores necesarios
                            tipoJuegosActivos.add(listaJuegos[servicio.ordinal() - 1]);
                        } else if ( servicio.equals(GRUPO_JUEGOS) ) {
                            guiAgente.agentesGrupoJuegos(listaAgentes[servicio.ordinal()]);
                        } else {
                            juegosDisponibles();
                        }
                        break;
                    }
                }        
            }
            
            if ( !tipoJuegosActivos.isEmpty() )
                guiAgente.activaProponerJuego(tipoJuegosActivos);   
        }

        /**
         * Cada vez que se elimina del registro un agente del juego se activa y
         * actualiza el guiAgentesJuego
         * @param dfad agente que deja el registro
         */
        @Override
        public void onDeregister(DFAgentDescription dfad) {
            AID agente = dfad.getName();
            
            for ( NombreServicio servicio : tiposAgentes ) {
                if ( listaAgentes[servicio.ordinal()].remove(agente) ) {
                    guiAgentesJuego.addAgente(listaAgentes[servicio.ordinal()], servicio);
                    
                    if( !servicio.equals(GRUPO_JUEGOS) ) {
                        // Eliminamos el jugador de los juegos que tenga aceptados y pendientes
                        eliminarJugador(agente);
                    }
                        
                    if ( !servicio.equals(GRUPO_JUEGOS) && 
                        (listaAgentes[servicio.ordinal()].size() < minJugadores[servicio.ordinal() - 1]) ) {
                        // Juegos que tienen el mínimo de jugadores necesarios
                        tipoJuegosActivos.remove(listaJuegos[servicio.ordinal() - 1]);
                     
                    } else if ( servicio.equals(GRUPO_JUEGOS) ) {
                        guiAgente.agentesGrupoJuegos(listaAgentes[servicio.ordinal()]);
                    } else {
                        juegosDisponibles();
                    }
                    break;
                }
            }
            
            if ( !tipoJuegosActivos.isEmpty() )
                guiAgente.activaProponerJuego(tipoJuegosActivos);
            else 
                guiAgente.anulaProponerJuego();   
        }
    }

    class TareaProponerJuego extends ProposeInitiator {
        
        private final Juego juego;
        private final int numJugadores;
        private final List<AID> agentesContactados;

        public TareaProponerJuego(Agent a, ACLMessage msg, RegistroJuego juegoActual, int numJugadores) {
            super(a, msg);
            this.juego = juegoActual.getJuegoPropuesto().getJuego();
            this.numJugadores = numJugadores;
            this.agentesContactados = new ArrayList();
            
            Iterator it = msg.getAllIntendedReceiver();
            while ( it.hasNext() ) {
                agentesContactados.add((AID)it.next());
            }
            
            it = juegoActual.getListaJugadores().iterator();
            while( it.hasNext() ) {
                Jugador jugador = (Jugador) it.next();
                agentesContactados.add(jugador.getAgenteJugador());
            }
        }

        @Override
        protected void handleOutOfSequence(ACLMessage msg) {
            // Ha llegado un mensaje fuera de la secuencia del protocolo
            guiConsola.mensaje("ERROR en Proponer Juego_____________________\n" + msg);
        }

        /**
         * Revisamos las propuestas recibidas, tanto afirmativas como negativas.
         *
         * @param responses
         */
        @Override
        protected void handleAllResponses(Vector responses) {
            String idJuego = juego.getIdJuego();
            TipoJuego tipoJuego = juego.getTipoJuego();

            Iterator it = responses.iterator();

            while (it.hasNext()) {
                ACLMessage msg = (ACLMessage) it.next();
                switch ( msg.getPerformative() ) {
                    case ACLMessage.ACCEPT_PROPOSAL:
                        try {
                            // Acepta la propuesta
                            JuegoAceptado juegoAceptado = (JuegoAceptado) manager[tipoJuego.ordinal()].extractContent(msg);
                            Jugador jugador = (Jugador) juegoAceptado.getAgenteJuego();
                            addRegistroJugador(juego, jugador);
                            guiConsola.mensaje("El agente: " + jugador.getNombre() + " acepta el juego " + juego);

                        } catch (Codec.CodecException | OntologyException ex) {
                            guiConsola.mensaje("Error en la construcción del mensaje de " + msg.getSender().getLocalName() +
                                    "\n" + msg + "\n" + ex);
                        } catch ( Exception ex ) {
                            guiConsola.mensaje("Error inesperado de " + msg.getSender().getLocalName() + "\n" + ex);
                        } break;
                    case ACLMessage.REJECT_PROPOSAL:
                        try {
                            // Rechaza la propuesta
                            Motivacion motivacion = (Motivacion) manager[tipoJuego.ordinal()].extractContent(msg);
                            guiConsola.mensaje("El agente: " + msg.getSender().getLocalName()
                                    + " rechaza el juego: " + juego + " por: " + motivacion);
                        } catch (Codec.CodecException | OntologyException ex) {
                            guiConsola.mensaje("Error en la construcción del mensaje de " + msg.getSender().getLocalName() +
                                    "\n" + msg + "\n" + ex);
                        } catch ( Exception ex ) {
                            guiConsola.mensaje("Error inesperado de " + msg.getSender().getLocalName() + "\n" + ex);
                        } break;
                    default:
                        break;
                }
            }

            RegistroJuego juegoRegistrado = registroJuegos.get(idJuego);
            if ( juegoRegistrado.numJugadores() < numJugadores) {
                completarJuego();
            } else {
                guiConsola.mensaje("Juego(" + juego.getIdJuego() + ") se ha registrado\n" + juegoRegistrado);
                juegosDisponibles();
            }
        }

        /**
         * No hay jugadores suficientes y se repetirá la tarea hasta conseguir
         * el mínimo de jugadores. No se incluyen los que ya han contestado
         * afirmativamente
         *
         * @param juego
         */
        private void completarJuego() {
            String idJuego = juego.getIdJuego();
            TipoJuego tipoJuego = juego.getTipoJuego();
            ModoJuego modoJuego = juego.getModoJuego();
            RegistroJuego juegoPropuesto = registroJuegos.get(idJuego);
            AID jugador;

            ProponerJuego proponerJuego = juegoPropuesto.getJuegoPropuesto();
            ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
            msg.setSender(myAgent.getAID());
            msg.setLanguage(codec.getName());
            msg.setOntology(listaOntologias[tipoJuego.ordinal()].getName());
            msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_OUT));

            // Añadimos a los jugadores que nos faltan para completar el juego
            if (modoJuego.equals(UNICO)) {
                // Contactamos con nuevos jugadores para podere completar el juego
                // si es un juego individual
                int numAgentes = numJugadores - juegoPropuesto.numJugadores();
                int total = 0;
                Iterator it = listaAgentes[tipoJuego.ordinal() + 1].iterator();
                while ( it.hasNext() && (total < numAgentes) ) {
                    jugador = (AID) it.next();
                    if ( juegoPropuesto.jugadorAusente(jugador) && 
                         !agentesContactados.contains(jugador) ) {
                        msg.addReceiver(jugador);
                        agentesContactados.add(jugador);
                        total++;
                    }
                }
                
                Action ac = new Action(myAgent.getAID(), proponerJuego);

                try {
                    manager[tipoJuego.ordinal()].fillContent(msg, ac);
                } catch (Codec.CodecException | OntologyException ex) {
                    guiConsola.mensaje("Error en la construcción del mensaje en Proponer Juego \n" + ex);
                }
            }
            
            // Reiniciamos la tarea hasta que consigamos la cantidad de jugadores necesarios
            // si hay nuevos jugadores
            if ( msg.getAllReceiver().hasNext() ) {
                reset(msg);
            } else {
                guiConsola.mensaje("No se han encontrado suficientes jugadores para el juego: " + juego);
                addJuegoPendiente(juego);
            }
        }
    }

    class TareaCompletarJuego extends ProposeInitiator {
        private final Juego juego;
        private final List agentesContactados;
        private final boolean reproduccion;
        private boolean nuevoAgente;

        public TareaCompletarJuego(Agent a, ACLMessage msg, Juego juego, boolean reproduccion) {
            super(a, msg);
            this.juego = juego;
            this.reproduccion = reproduccion;
            agentesContactados = new ArrayList();

            // Almacenamos con el primero que contactamos inicialmente 
            agentesContactados.add(msg.getAllReceiver().next());

            // Quedan agentes para intentarlo
            nuevoAgente = true;
        }

        @Override
        protected void handleOutOfSequence(ACLMessage msg) {
            // Ha llegado un mensaje fuera de la secuencia del protocolo
            guiConsola.mensaje("ERROR en Completar Juego_____________________\n" + msg);
        }

        @Override
        protected void handleAllResponses(Vector responses) {

            Iterator it = responses.iterator();
            TipoJuego tipoJuego = juego.getTipoJuego();
            while (it.hasNext()) {
                ACLMessage msg = (ACLMessage) it.next();
                switch ( msg.getPerformative() ) {
                    case ACLMessage.ACCEPT_PROPOSAL:
                        try {
                            JuegoAceptado juegoAceptado = (JuegoAceptado) manager[tipoJuego.ordinal()].extractContent(msg);
                            Grupo agenteGrupo = (Grupo) juegoAceptado.getAgenteJuego();
                            addRegistroGrupoJuegos(agenteGrupo, juego);
                            guiConsola.mensaje(juegoAceptado.toString());
                            juegosDisponibles();
                        } catch (Codec.CodecException | OntologyException ex) {
                            guiConsola.mensaje("Error en la construcción del mensaje de " + msg.getSender().getLocalName() +
                                    "\n" + msg);
                        } catch ( Exception ex ) {
                            guiConsola.mensaje("Error inesperado de " + msg.getSender().getLocalName() + "\n" + ex);
                        } break;
                    case ACLMessage.REJECT_PROPOSAL:
                        try {
                            Motivacion motivacion = (Motivacion) manager[tipoJuego.ordinal()].extractContent(msg);
                            if ( !reproduccion ) {
                                guiConsola.mensaje(motivacion.toString() + "\nBuscamos un agente grupo de juego alternativo");
                                completarPropuesta();
                            } else {
                                guiConsola.mensaje("Error al reproducir el juego: " + juego);
                            }
                        } catch (Codec.CodecException | OntologyException ex) {
                            guiConsola.mensaje("Error en la construcción del mensaje de " + msg.getSender().getLocalName() +
                                    "\n" + msg);
                        } catch ( Exception ex ) {
                            guiConsola.mensaje("Error inesperado de " + msg.getSender().getLocalName() + "\n" + ex);
                        } break;
                    case ACLMessage.NOT_UNDERSTOOD:
                        // Juego no implementado en el agente Grupo Juegos seleccionado
                        // buscamos si hay alternativa posible
                        if ( !reproduccion ) {
                            completarPropuesta();
                            if (!nuevoAgente) {
                                guiConsola.mensaje("No hay agentes para atender la propuesta de juego");
                            }
                        } else {
                            guiConsola.mensaje("Error al reproducir el juego: " + juego);
                        } break;
                    default:
                        break;
                }
            }
        }

        /**
         * Contacta con el siguiente agente conocido para completar el juego
         */
        private void completarPropuesta() {
            Iterator it = listaAgentes[GRUPO_JUEGOS.ordinal()].iterator();
            AID agente = null;
            nuevoAgente = false;
            // Comprobamos que quedan agentes con los que no hemos contactado
            while ( it.hasNext() && !nuevoAgente ) {
                agente = (AID) it.next();
                if ( !agentesContactados.contains(agente) ) {
                    agentesContactados.add(agente);
                    nuevoAgente = true;
                }
            }
            
            if ( nuevoAgente ) {
                RegistroJuego juegoRegistrado = registroJuegos.get(juego.getIdJuego());
                TipoJuego tipoJuego = juego.getTipoJuego();
                Concept condicionesJuego = juegoRegistrado.getJuegoPropuesto().getTipoJuego();
                List listaJugadores = juegoRegistrado.getListaJugadores();
                CompletarJuego completarJuego = new CompletarJuego(juego, condicionesJuego, 
                                                         new jade.util.leap.ArrayList((ArrayList) listaJugadores));
            
                // Creamos el mensaje
                ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
                msg.setSender(myAgent.getAID());
                msg.setLanguage(codec.getName());
                msg.setOntology(listaOntologias[tipoJuego.ordinal()].getName());
                msg.addReceiver(agente);
                msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_OUT));

                Action ac = new Action(myAgent.getAID(), completarJuego);
            
                try {
                    manager[tipoJuego.ordinal()].fillContent(msg, ac);
                } catch (Codec.CodecException | OntologyException ex) {
                    guiConsola.mensaje("Error en la construcción del mensaje en Completar Juego \n" + ex);
                }
                
                reset(msg);
            } else {
                guiConsola.mensaje("No hay agentes para atender la propuesta de juego");
            }
        }
    }
    
    class TareaInformarJuego extends SubscriptionInitiator {
        
        public TareaInformarJuego(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected void handleOutOfSequence(ACLMessage msg) {
            // Ha llegado un mensaje fuera de la secuencia del protocolo
            guiConsola.mensaje("ERROR en Informar Juego___________________\n" + msg);
        }

        /**
         * Llega la clasificación de los juegos del agente central juego
         * @param inform 
         */
        @Override
        protected void handleInform(ACLMessage inform) {
            // Versión preliminar
            Predicate elemento;
            Juego juego;
            
            // Buscamos la ontología del mensaje
            int indiceJuego = buscarIndiceJuego(inform.getOntology());
            
            try {
                elemento = (Predicate) manager[indiceJuego].extractContent(inform);
                
                if ( elemento instanceof ClasificacionJuego ) {
                    // Presentamos la clasificación del juego
                    ClasificacionJuegoJFrame guiClasificacion;
                    ClasificacionJuego clasificacion = (ClasificacionJuego) elemento;
                    juego = clasificacion.getJuego();
                    guiClasificacion = new ClasificacionJuegoJFrame(clasificacion);
                    guiConsola.mensaje("Fin Juego \n" + juego + "\n" +
                            clasificacion.getListaJugadores() + "\n" + clasificacion.getListaPuntuacion());
                } else {
                    // Ha ocurrido un problema en el juego
                    IncidenciaJuego incidencia = (IncidenciaJuego) elemento;
                    juego = incidencia.getJuego();
                    guiConsola.mensaje("Error en el juego " + juego + "\n" 
                                       + " por " + incidencia.getDetalle());
                }
                
                // Guardamos el resultado del juego finalizado
                addResultadoJuego(juego, elemento);
            } catch (Codec.CodecException | OntologyException ex) {
                guiConsola.mensaje("Error en el formato del mensaje del agente " + 
                                inform.getSender().getLocalName());
            } catch ( Exception ex ) {
               guiConsola.mensaje("Error inesperado de\n" + ex);      
            }
        }

        /**
         * El agente grupo de juegos rechaza la suscripción
         * @param refuse 
         */
        @Override
        protected void handleRefuse(ACLMessage refuse) {
            // Versión preliminar
            guiConsola.mensaje("El agente: " + refuse.getSender().getName() + " rechaza la suscripción");
            
        }

        /**
         * El agente grupo juegos acepta la suscripción
         * @param agree 
         */
        @Override
        protected void handleAgree(ACLMessage agree) {
            // Versión preliminar
            guiConsola.mensaje("El agente: " + agree.getSender().getName() + " acepta la suscripción");
        }
    }
}
