package cristiangs.handler;

import cristiangs.handler.interfaces.SocketConnectionListener;
import cristiangs.handler.interfaces.SocketDisconnectListener;
import cristiangs.handler.interfaces.SocketMessageListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Esta clase representa un controlador de servidor de sockets.
 */
public class ServerSocketHandler {
    /**
     * El servicio de ejecución para administrar hilos. Se utiliza para procesar conexiones entrantes.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    /**
     * El objeto ServerSocket asociado a esta instancia.
     */
    private final ServerSocket serverSocket;

    /**
     * Lista de oyentes para eventos de conexiones de clientes.
     */
    private final ArrayList<SocketConnectionListener> socketConnectionListeners = new ArrayList<>();

    /**
     * Lista de oyentes para eventos de desconexión de clientes.
     */
    private final ArrayList<SocketDisconnectListener> socketDisconnectListeners = new ArrayList<>();

    /**
     * Lista de instancias de socket.ClientSocketHandler que representan a los clientes conectados.
     */
    private final ArrayList<ClientSocketHandler> clientSockets = new ArrayList<>();

    /**
     * Bandera que indica si el servidor esta cerrado.
     */
    private boolean close;

    /**
     * Bandera que indica si el servidor esta escuchando nuevas conexiones.
     */
    private boolean listening;

    /**
     * Crea una instancia de socket.ServerSocketHandler utilizando un objeto ServerSocket existente.
     *
     * @param serverSocket El objeto ServerSocket que se utilizará para administrar conexiones entrantes.
     */
    public ServerSocketHandler(ServerSocket serverSocket) {
        if (serverSocket == null) {
            throw new NullPointerException("El ServerSocket es null");
        }
        this.serverSocket = serverSocket;
    }

    /**
     * Inicia la escucha de conexiones entrantes en un hilo separado.
     * Dejara de escuchar hasta acceptar el numero de clientes indicado.
     *
     * @param clientMax indica el numero maximo de clientes que aceptara.
     */
    public void starListeningConnections(int clientMax) {
        if (clientMax < 1) {
            throw new IllegalArgumentException("El numero de clientes debe ser mayor a 0");
        }
        if (listening) return;
        if (close) return;
        if (executor.isShutdown()) return;

        listening = true;

        executor.execute(() -> {
            int clientNum = 0;
            while (!executor.isShutdown() && !close && clientNum < clientMax) {
                acceptClient();
                clientNum++;
            }
            listening = false;
        });
    }

    /**
     * Cierra el ServerSocket, ya no podra resibir nuevos clientes.
     * El setvidor seguira resibiendo y respondiendo mensajes entrantes de los clientes restantes.
     *
     * @throws IOException Si ocurre un error al cerrar el ServerSocket.
     */
    public void closeServer() throws IOException {
        if (close) return;
        serverSocket.close();
        close = true;
        listening = false;
    }

    /**
     * Cierra la conexión con todos los sockets clientes y los borra de la lista,
     * Pero no cierra los recursos, el servidor podra seguir recibiendo nuevos clientes y mensajes.
     */
    public void clearSockets() {
        for (ClientSocketHandler clientSocketHandler : clientSockets) {
            if (clientSocketHandler == null) continue;
            clientSocketHandler.closeSocket();
        }
        clientSockets.clear();
    }

    /**
     * Cierra el servidor y todas las conexiónes, detiene los hilos de escucha y liberar los recuros asociado,
     * esto desactivar el servidor por completo.
     *
     * @throws IOException Si ocurre un error al cerrar el ServerSocket.
     */
    public void dropServer() throws IOException {
        closeServer();
        clearSockets();
        executor.shutdown();
    }

    /**
     * Crea instancias de socket.ClientSocketHandler para manejarlas.
     * Agrega los nuevos clientes a la lista de clientes y notifica cuando se establece una nueva conexión.
     */
    private void acceptClient() {
        try {
            Socket socket = serverSocket.accept();

            ClientSocketHandler clientSocketHandler = new ClientSocketHandler(socket);
            clientSocketHandler.addSocketMessageListener(socketMessageListener);
            clientSocketHandler.addSocketDisconnectListener(socketDisconnectListener);
            clientSocketHandler.setClientName("Servidor");

            clientSocketHandler.starCommunication();

            clientSockets.add(clientSocketHandler);

            notifySocketConnectionEvent(clientSocketHandler);
        } catch (SocketException ignored) {
            //Se espera que lance este error cuando el servidor se desconecte.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Envía un mensaje a todos los clientes conectados de manera asincrónica.
     * Si el objeto es null, la acción se anulara.
     *
     * @param object El objeto que se enviará a los clientes.
     */
    public synchronized void sendMessageToAllClients(Object object) {
        if (object == null) return;
        if (executor.isShutdown()) return;
        executor.execute(() -> {
            for (ClientSocketHandler clientSocketHandler : clientSockets) {
                if (clientSocketHandler == null || clientSocketHandler.isClose()) continue;
                clientSocketHandler.sendMessage(object);
            }
        });
    }

    /**
     * Envia un mensaje a un unico cliente.
     * Si alguno de los objetos es null, la acción se anulara.
     *
     * @param client El destinatario
     * @param object El El objeto que se enviará al cliente.
     */
    public synchronized void sendMessageToClient(ClientSocketHandler client, Object object) {
        if (object == null) return;
        if (client == null) return;
        if (executor.isShutdown()) return;
        executor.execute(() -> client.sendMessage(object));
    }

    /**
     * El oyente para eventos de mensajes del socket. Por defecto, reenvía los mensajes entrantes a través del método sendMessage.
     */
    private SocketMessageListener socketMessageListener = (clientSocketHandle, object) -> sendMessageToAllClients(object);

    /**
     * El oyente para eventos de desconexión del socket. Por defecto, notifica la desconexión a través del método notifyDisconnection.
     */
    private final SocketDisconnectListener socketDisconnectListener = this::notifyDisconnection;

    /**
     * Establece un nuevo oyente para eventos de mensajes del socket.
     *
     * @param socketMessageListener El nuevo oyente para eventos de mensajes del socket.
     */
    public void setSocketMessageListener(SocketMessageListener socketMessageListener) {
        if (socketMessageListener == null) return;
        this.socketMessageListener = socketMessageListener;
    }

    /**
     * Obtiene el oyente actual para eventos de mensajes del socket.
     *
     * @return El oyente actual para eventos de mensajes del socket.
     */
    public SocketMessageListener getSocketMessageListener() {
        return socketMessageListener;
    }

    /**
     * Notifica a los oyentes de eventos de desconexión del socket que un cliente se ha desconectado.
     *
     * @param clientSocketHandler El cliente que se ha desconectado.
     */
    private synchronized void notifyDisconnection(ClientSocketHandler clientSocketHandler) {
        if (clientSocketHandler == null) return;
        for (SocketDisconnectListener disconnectListener : socketDisconnectListeners) {
            if (disconnectListener == null) continue;
            disconnectListener.disconnected(clientSocketHandler);
        }
    }

    /**
     * Agrega un oyente para eventos de desconexión del socket.
     *
     * @param socketDisconnectListener El oyente que se agregará.
     */
    public void addSocketDisconnectListener(SocketDisconnectListener socketDisconnectListener) {
        if (socketDisconnectListener == null) return;
        socketDisconnectListeners.add(socketDisconnectListener);
    }

    /**
     * Elimina un oyente de eventos de desconexión del socket.
     *
     * @param socketDisconnectListener El oyente que se eliminará.
     */
    public void removeSocketDisconnectListener(SocketDisconnectListener socketDisconnectListener) {
        if (socketDisconnectListener == null) return;
        socketDisconnectListeners.remove(socketDisconnectListener);
    }

    /**
     * Notifica a los oyentes de eventos de conexión del socket que se ha establecido una nueva conexión.
     *
     * @param clientSocketHandler El cliente que se ha conectado.
     */
    private synchronized void notifySocketConnectionEvent(ClientSocketHandler clientSocketHandler) {
        if (clientSocketHandler == null) return;
        for (SocketConnectionListener socketConnectionListener : socketConnectionListeners) {
            if (socketConnectionListener == null) continue;
            socketConnectionListener.newConnection(clientSocketHandler);
        }
    }

    /**
     * Agrega un oyente para eventos de conexión del socket.
     *
     * @param socketConnectionListener El oyente que se agregará.
     */
    public void addSocketConnectionListeners(SocketConnectionListener socketConnectionListener) {
        if (socketConnectionListener == null) return;
        socketConnectionListeners.add(socketConnectionListener);
    }

    /**
     * Elimina un oyente de eventos de conexión del socket.
     *
     * @param socketConnectionListener El oyente que se eliminará.
     */
    public void removeSocketConnectionListeners(SocketConnectionListener socketConnectionListener) {
        if (socketConnectionListener == null) return;
        socketConnectionListeners.remove(socketConnectionListener);
    }

    /**
     * Verifica si el servidor esta escuchando nuevas conexiones.
     * @return true si es servidor esta escuchando.
     */
    public boolean isListening() {
        return listening;
    }

    /**
     * Verifica si el servidor se ha cerrado.
     * @return true si el servidor se ha cerrado.
     */
    public boolean isClose() {
        return close;
    }

    /**
     * Retorna el numero de clientes en la lista del servidor,
     * estos clientes no necesariamente estan conectados.
     *
     * @return el numero de clientes registrados.
     */
    public int getClientsNum() {
        return clientSockets.size();
    }
}
