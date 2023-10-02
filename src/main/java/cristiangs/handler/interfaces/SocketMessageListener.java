package cristiangs.handler.interfaces;


import cristiangs.handler.ClientSocketHandler;

/**
 * Esta interfaz define un listener para eventos de mensajes recibidos en un socket.
 * Los objetos que implementen esta interfaz pueden registrarse para recibir notificaciones
 * cuando se recibe un nuevo mensaje en un socket.
 */
public interface SocketMessageListener {

    /**
     * Este método se llama cuando se recibe un nuevo mensaje en un socket.
     *
     * @param clientSocketHandler El manejador de socket del cliente que recibió el mensaje.
     * @param object El objeto que representa el mensaje recibido.
     */
    void newMessage(ClientSocketHandler clientSocketHandler, Object object);

}
