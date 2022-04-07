package os.hw1.util;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler {
    private final Socket socket;
    private final Scanner scanner;
    private final PrintStream printStream;
    private final RequestProcessor requestProcessor;

    public ClientHandler(Socket socket, RequestProcessor requestProcessor) {
        try {
            this.socket = socket;
            this.requestProcessor = requestProcessor;
            this.scanner = new Scanner(socket.getInputStream());
            this.printStream = new PrintStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void run() {
        try {
            String request = scanner.nextLine();
            String response = requestProcessor.handle(request);
            printStream.println(response);
            printStream.flush();
        } finally {
            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
