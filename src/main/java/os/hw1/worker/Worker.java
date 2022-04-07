package os.hw1.worker;

import os.hw1.util.SocketServerHandler;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

public class Worker {
    private final Object lock;
    private volatile boolean running;
    private final SocketServerHandler socketServerHandler;
    private final int workerNumber;

    public Worker(int port, int workerNumber) {
        this.workerNumber = workerNumber;
        this.lock = new Object();
        this.socketServerHandler = new SocketServerHandler(port, this::handleRequest);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
        System.out.println();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("please specify port and worker number");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        int workerNumber = Integer.parseInt(args[1]);
        Worker worker = new Worker(port, workerNumber);
        worker.start();
    }

    public void start() {
        synchronized (this.lock) {
            this.running = true;
        }
        socketServerHandler.start();
    }

    private String handleRequest(String request) {
        Scanner scanner = new Scanner(request);
        String command = scanner.next();
        if (command.equals("keep-connection")) {
            return keepConnection();
        }
        if (command.equals("execute")) {
            int commandNum = scanner.nextInt();
            String[] commands = new String[commandNum];
            for (int i = 0; i < commandNum; i++) {
                commands[i] = scanner.next();
            }
            int input = scanner.nextInt();
            return execute(commands, input);
        }
        return "";
    }

    private String keepConnection() {
        synchronized (this.lock) {
            while (running) {
                try {
                    this.lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return String.valueOf(workerNumber);
    }

    private String execute(String[] command, int input) {
        try {
            Process process = new ProcessBuilder(command).start();
            System.out.printf("job with started with PID: %d\n", process.pid());
            PrintStream printStream = new PrintStream(process.getOutputStream());
            printStream.println(input);
            printStream.flush();
            Scanner scanner = new Scanner(process.getInputStream());
            int output = scanner.nextInt();
            process.waitFor();
            return String.valueOf(output);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void shutdownHook() {
        synchronized (this.lock) {
            this.running = false;
            this.lock.notifyAll();
        }
        socketServerHandler.stop();
    }
}
