package cristiangs.handler;

import cristiangs.handler.interfaces.SocketDisconnectListener;
import cristiangs.handler.interfaces.SocketMessageListener;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Esta clase representa un controlador de socket del cliente.
 */
public class ClientSocketHandler {
    /**
     * El servicio de ejecución para administrar hilos.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    /**
     * Lista de oyentes para eventos de mensajes del socket.
     */
    private final ArrayList<SocketMessageListener> socketMessageListeners = new ArrayList<>();

    /**
     * Lista de oyentes para eventos de desconexión del socket.
     */
    private final ArrayList<SocketDisconnectListener> socketDisconnectListeners = new ArrayList<>();

    /**
     * El objeto Socket asociado a esta instancia.
     */
    private final Socket socket;

    /**
     * El nombre del cliente.
     */
    private String clientName;

    /**
     * Indica si el socket está cerrado.
     */
    private boolean close;

    /**
     * El flujo de entrada asociado al socket, debe cerrarse al finalizar.
     */
    private final InputStream inputStream;

    /**
     * El flujo de entrada de objetos asociado al socket, debe cerrarse al finalizar.
     */
    private ObjectInputStream objectInputStream;

    /**
     * El flujo de salida asociado al socket, debe cerrarse al finalizar.
     */
    private final OutputStream outputStream;

    /**
     * El flujo de salida de objetos asociado al socket, debe cerrarse al finalizar.
     */
    private ObjectOutputStream objectOutputStream;

    /**
     * Crea una instancia de socket.ClientSocketHandler utilizando un socket existente.
     *
     * @param socket El socket existente que se utilizará.
     * @throws NullPointerException Si el socket proporcionado es nulo.
     * @throws IOException si no le logra optener los flujos de datos.
     */
    public ClientSocketHandler(Socket socket) throws IOException {
        if (socket == null) {
            throw new NullPointerException("El Socket es null");
        }
        this.socket = socket;
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        setClientName("Cliente");
    }

    /**
     * Inicia la comunicación al lanzar un hilo para recibir mensajes.
     * Si los hilos ya han detenido, la acción se anulara.
     */
    public void starCommunication() {
        if (executor.isShutdown()) return;
        executor.execute(this::readMessage);
    }

    /**
     * Cierra todos los recursos y hilos asociados al socket.
     * Si el servidor ya ha sido cerrado, la acción se anulara.
     */
    public void closeSocket() {
        if (close) return;
        notifyDisconnect();
        try {
            if (objectInputStream != null) objectInputStream.close();
            if (inputStream != null) inputStream.close();

            if (objectOutputStream != null) objectOutputStream.close();
            if (outputStream != null) outputStream.close();

            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        close = true;
        executor.shutdown();
    }

    /**
     * Lee mensajes desde el flujo de entrada del socket y notifica cuando se recibe un mensaje nuevo.
     * Este método se ejecuta en un bucle hasta que se cierra el socket o el hilo se apaga.
     */
    private void readMessage() {
        while (!executor.isShutdown() && inputStream != null && !socket.isClosed() && socket.isConnected()) {
            try {
                if (objectInputStream == null) objectInputStream = new ObjectInputStream(inputStream);
                Object object = objectInputStream.readObject();
                notifyNewMessage(object);
            } catch (IOException e) {
                /*
                Se espera que lance este error cuando el socket se desconecte.
                Es probable que se agregue un sistema de notificaciones en un futuro.
                 */
                closeSocket();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Envía un objeto a través de la ejecución de un hilo en el executor, si no está cerrado.
     * Si el objeto es null o si los hilos se han detenido, la acción se anulara.
     *
     * @param object El objeto que se enviará a través del hilo del executor.
     */
    public void sendMessage(Object object) {
        if (object == null) return;
        if (executor.isShutdown()) return;
        executor.execute(() -> {
            if (outputStream != null && !socket.isClosed() && socket.isConnected()) {
                try {
                    if (objectOutputStream == null) objectOutputStream = new ObjectOutputStream(outputStream);
                    objectOutputStream.writeObject(object);
                } catch (IOException e) {
                    //Se espera que lance este error cuando el socket se desconecte durante el envio de datos,
                    closeSocket();
                }
            }
        });
    }

    /**
     * Notifica a los oyentes de mensajes sobre la recepción de un nuevo objeto.
     * Si el objeto es null, la acción se anulara.
     *
     * @param object El objeto que se ha recibido y se notificará a los oyentes.
     */
    private void notifyNewMessage(Object object) {
        if (object == null) return;
        for (SocketMessageListener socketMessageListener : socketMessageListeners) {
            if (socketMessageListener == null) continue;
            socketMessageListener.newMessage(this, object);
        }
    }

    /**
     * Notifica a los escuchadores que la conexión se ha desconectado.
     */
    private void notifyDisconnect() {
        for (SocketDisconnectListener socketDisconnectListener : socketDisconnectListeners) {
            if (socketDisconnectListener == null) continue;
            socketDisconnectListener.disconnected(this);
        }
    }

    /**
     * Agrega un oyente para eventos de desconexión del socket.
     * Si el objeto es null, la acción se anulara.
     *
     * @param socketDisconnectListener El oyente que se agregará.
     */
    public void addSocketDisconnectListener(SocketDisconnectListener socketDisconnectListener) {
        if (socketDisconnectListener == null) return;
        socketDisconnectListeners.add(socketDisconnectListener);
    }

    /**
     * Elimina un oyente de eventos de desconexión del socket.
     * Si el objeto es null, la acción se anulara.
     *
     * @param socketDisconnectListener El oyente que se eliminará.
     */
    public void removeSocketDisconnectListener(SocketDisconnectListener socketDisconnectListener) {
        if (socketDisconnectListener == null) return;
        socketDisconnectListeners.remove(socketDisconnectListener);
    }

    /**
     * Agrega un oyente para eventos de mensajes del socket.
     * Si el objeto es null, la acción se anulara.
     *
     * @param socketMessageListener El oyente que se agregará.
     */
    public void addSocketMessageListener(SocketMessageListener socketMessageListener) {
        if (socketMessageListener == null) return;
        socketMessageListeners.add(socketMessageListener);
    }

    /**
     * Elimina un oyente de eventos de mensajes del socket.
     * Si el objeto es null, la acción se anulara.
     *
     * @param socketMessageListener El oyente que se eliminará.
     */
    public void removeSocketMessageListener(SocketMessageListener socketMessageListener) {
        if (socketMessageListener == null) return;
        socketMessageListeners.remove(socketMessageListener);
    }

    /**
     * Obtiene el objeto Socket asociado a esta instancia.
     *
     * @return El objeto Socket asociado a esta instancia.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Obtiene el nombre del cliente.
     *
     * @return El nombre del cliente.
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Establece el nombre del cliente.
     *
     * @param clientName El nuevo nombre del cliente.
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * Verifica si el socket está cerrado, esto incluye los hilos de escucha.
     *
     * @return true si el socket está cerrado, false de lo contrario.
     */
    public boolean isClose() {
        return close;
    }
}
