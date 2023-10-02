package cristiangs.handler.interfaces;


import cristiangs.handler.ClientSocketHandler;

/**
 * Esta interfaz define un listener para eventos de nuevas conexiones de socket.
 * Los objetos que implementen esta interfaz pueden registrarse para recibir notificaciones
 * cuando se establece una nueva conexión de socket.
 */
public interface SocketConnectionListener {

    /**
     * Este método se llama cuando se establece una nueva conexión de socket.
     *
     * @param clientSocketHandler El manejador de socket del cliente que representa la nueva conexión.
     */
    void newConnection(ClientSocketHandler clientSocketHandler);
}
