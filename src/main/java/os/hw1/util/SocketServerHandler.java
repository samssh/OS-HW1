package os.hw1.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServerHandler {
    private final ServerSocket serverSocket;
    private final Thread thread;
    private final RequestProcessor requestProcessor;
    private final ExecutorService executorService;

    public SocketServerHandler(int port, RequestProcessor requestProcessor) {
        try {
            this.executorService = Executors.newCachedThreadPool();
            this.serverSocket = new ServerSocket(port);
            this.thread = new Thread(this::listen);
            this.requestProcessor = requestProcessor;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        this.thread.start();
    }

    public void stop() {
        try {
            executorService.shutdown();
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void listen() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this.requestProcessor);
                executorService.execute(clientHandler::run);
            } catch (IOException e) {
                break;
            }
        }
    }
}
