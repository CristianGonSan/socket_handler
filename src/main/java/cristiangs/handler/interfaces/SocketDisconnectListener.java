package cristiangs.handler.interfaces;

import cristiangs.handler.ClientSocketHandler;

/**
 * Esta interfaz define un listener para eventos de desconexión de socket.
 * Los objetos que implementen esta interfaz pueden registrarse para recibir notificaciones
 * cuando se produce una desconexión de socket.
 */
public interface SocketDisconnectListener {

    /**
     * Este método se llama cuando se produce una desconexión de socket.
     *
     * @param clientSocketHandler El manejador de socket del cliente que representa la desconexión.
     */
    void disconnected(ClientSocketHandler clientSocketHandler);
}
