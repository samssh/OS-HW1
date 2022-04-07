package os.hw1.master;

import os.hw1.cache.Cache;
import os.hw1.util.SocketServerHandler;
import os.hw1.worker.Worker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Master {
    private final Object lock;
    private volatile boolean running;
    private final int port;
    private final int workerCount;
    private final int totalWeight;
    private final List<String> commonArgs;
    private final String[] programNames;
    private final int[] programWeights;
    private final SocketServerHandler socketServerHandler;
    private CacheState cacheState;
    private final Queue<WorkerState> workerStates;
    private final PriorityQueue<RequestData> requests;
    private final ExecutorService executorService;


    public Master(int port, int workerCount, int totalWeight, String[] commonArgs, String[] programNames, int[] programWeights) {
        this.lock = new Object();
        this.port = port;
        this.workerCount = workerCount;
        this.totalWeight = totalWeight;
        this.commonArgs = Arrays.asList(commonArgs);
        this.programNames = programNames;
        this.programWeights = programWeights;
        this.socketServerHandler = new SocketServerHandler(port, this::handleRequest);
        this.workerStates = new PriorityQueue<>(workerCount, Comparator.comparing(WorkerState::getLoad));
        this.requests = new PriorityQueue<>(Comparator.comparing(RequestData::getTime));
        this.executorService = Executors.newCachedThreadPool();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
    }

    private void shutdownHook() {
        if (cacheState.getProcess() != null) {
            cacheState.getProcess().destroy();
        }
        for (WorkerState workerState : workerStates) {
            if (workerState.getProcess() != null) {
                workerState.getProcess().destroy();
            }
        }
    }

    public void start() {
        synchronized (lock) {
            System.out.printf("master start %d %d\n", ProcessHandle.current().pid(), port);
            this.running = true;
            this.initCache();
            this.initWorkers();
            this.socketServerHandler.start();
            this.executorService.submit(this::processRequests);
        }
    }

    private void initCache() {
        int programCount = this.programNames.length;
        int cachePort = this.port - 1;
        Process cacheProcess = runProcess(Cache.class.getName(), cachePort, programCount);
        this.cacheState = new CacheState(cacheProcess, cachePort);
        new Scanner(cacheProcess.getInputStream()).nextLine();
        System.out.printf("cache start %d %d\n", cacheState.getPid(), cachePort);
    }

    private void initWorkers() {
        for (int i = 0; i < this.workerCount; i++) {
            int workerPort = this.port + i + 1;
            WorkerState workerState = new WorkerState(i, workerPort);
            this.startWorker(workerState);
            this.executorService.submit(() -> this.workerHealthCheck(workerState));
        }
    }

    static void transfer(OutputStream outputStream, InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream);
        PrintStream printStream = new PrintStream(outputStream);
        while (scanner.hasNextLine()) {
            printStream.println(scanner.nextLine());
            printStream.flush();
        }
    }

    private void startWorker(WorkerState workerState) {
        Process workerProcess = runProcess(Worker.class.getName(), workerState.getPort(), workerState.getNumber());
        new Scanner(workerProcess.getInputStream()).nextLine();
        workerState.setProcess(workerProcess);
        workerState.setLoad(0);
        synchronized (this.lock) {
            this.workerStates.add(workerState);
            this.lock.notifyAll();
        }
        System.out.printf("worker %d start %d %d\n", workerState.getNumber(), workerState.getPid(), workerState.getPort());
    }

    private String sendRequest(int port, String request) {
        try {
            long start = System.currentTimeMillis();
            System.err.println("master send request " + port + " " + request);
            Socket socket = new Socket(InetAddress.getLocalHost(), port);
            Scanner scanner = new Scanner(socket.getInputStream());
            PrintStream printStream = new PrintStream(socket.getOutputStream());
            printStream.println(request);
            String response = scanner.nextLine();
            socket.close();
            System.err.println("master get response " + port + " " + request + " res: " + response + " time: " + (System.currentTimeMillis() - start));
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeWorker(WorkerState workerState) {
        synchronized (this.lock) {
            this.workerStates.remove(workerState);
        }
    }

    private void workerHealthCheck(WorkerState workerState) {
        boolean running = true;
        while (running) {
            try {
                Process workerProcess = workerState.getProcess();
                if (workerProcess == null || !workerProcess.isAlive()) {
                    startWorker(workerState);
                }
                String response = sendRequest(workerState.getPort(), "keep-connection");
                if (Integer.parseInt(response) == workerState.getNumber() || !workerState.getProcess().isAlive()) {
                    removeWorker(workerState);
                    if (workerState.getProcess().isAlive()) {
                        try {
                            workerState.getProcess().waitFor();
                            System.out.printf("worker %d stop %d %d\n", workerState.getNumber(), workerState.getPid(), workerState.getPort());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    List<RequestData> requestDataList;
                    synchronized (workerState) {
                        while (true) {
                            int requestsLoad = workerState.getRequestDataList().stream().mapToInt(RequestData::getLastProgram)
                                    .map(operand -> this.programWeights[operand]).sum();
                            if (requestsLoad < workerState.getLoad()) {
                                try {
                                    workerState.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else break;
                        }
                        requestDataList = new LinkedList<>(workerState.getRequestDataList());
                    }
                    addRequest(requestDataList);
                }
            } catch (Exception exception) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            synchronized (this.lock) {
                running = this.running;
            }
        }
    }

    private Process runProcess(String className, int port, int num) {
        List<String> command = new LinkedList<>(this.commonArgs);
        command.add(className);
        command.add(String.valueOf(port));
        command.add(String.valueOf(num));
        try {
            Process process = new ProcessBuilder(command).start();
            new Thread(() -> transfer(System.err, process.getErrorStream())).start();
            return process;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void releaseWorker(WorkerState workerState, int weight) {
        this.workerStates.remove(workerState);
        workerState.setLoad(workerState.getLoad() - weight);
        this.workerStates.add(workerState);
    }

    private void addRequest(Collection<RequestData> requestData, WorkerState workerState, int weight) {
        synchronized (this.lock) {
            this.requests.addAll(requestData);
            this.lock.notifyAll();
            if (workerState != null)
                this.releaseWorker(workerState, weight);
        }
    }

    private void addRequest(Collection<RequestData> requestData) {
        addRequest(requestData, null, -1);
    }

    private String handleRequest(String request) {
        System.err.println("master receive req " + request);
        long start = System.currentTimeMillis();
        Scanner scanner = new Scanner(request);
        String programs = scanner.next();
        int input = scanner.nextInt();
        RequestData requestData = new RequestData(programs.split("\\|"), input);
        addRequest(Collections.singleton(requestData));
        synchronized (requestData) {
            while (requestData.getPrograms().size() != 0) {
                try {
                    requestData.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.err.println("master respond req " + request + " " + requestData.getInput() + " time: " + (System.currentTimeMillis() - start));
        return String.valueOf(requestData.getInput());
    }

    private void updateRequest(RequestData requestData, int output, WorkerState workerState, int weight) {
        requestData.removeLastProgram();
        requestData.setInput(output);
        if (requestData.getPrograms().isEmpty()) {
            synchronized (requestData) {
                requestData.notifyAll();
            }
            if (workerState != null) {
                synchronized (this.lock) {
                    this.releaseWorker(workerState, weight);
                    this.lock.notifyAll();
                }
            }
        } else {
            addRequest(Collections.singletonList(requestData), workerState, weight);
        }
    }

    private void updateRequest(RequestData requestData, int output) {
        updateRequest(requestData, output, null, -1);
    }

    private void getOutputFromCache(RequestData requestData) {
        String request = String.format("get %d %d", requestData.getLastProgram(), requestData.getInput());
        String response = sendRequest(cacheState.getPort(), request);
        if (response.matches("\\d+")) {
            int output = Integer.parseInt(response);
            updateRequest(requestData, output);
        } else {
            addRequest(Collections.singletonList(requestData));
        }
    }

    private void processRequestsByCache() {
        for (Iterator<RequestData> iterator = requests.iterator(); iterator.hasNext(); ) {
            RequestData requestData = iterator.next();
            String request = String.format("check %d %d", requestData.getLastProgram(), requestData.getInput());
            String response = sendRequest(cacheState.getPort(), request);
            if (response.equals("true")) {
                iterator.remove();
                this.executorService.submit(() -> getOutputFromCache(requestData));
            }
        }
    }

    private void getOutputFromWorker(RequestData requestData, WorkerState workerState) {
        int programNumber = requestData.getLastProgram();
        String commonArgString = String.join(" ", this.commonArgs);
        int input = requestData.getInput();
        String request = String.format("execute %d %s %s %d", this.commonArgs.size() + 1, commonArgString, this.programNames[programNumber], input);
        try {
            String response = sendRequest(workerState.getPort(), request);
            int output = Integer.parseInt(response);
            updateRequest(requestData, output, workerState, this.programWeights[programNumber]);
            String requestToCache = String.format("set %d %d %d", programNumber, input, output);
            sendRequest(cacheState.getPort(), requestToCache);
        } catch (Exception e) {
//            e.printStackTrace();
            synchronized (workerState) {
                workerState.getRequestDataList().add(requestData);
                workerState.notifyAll();
            }
            String requestToCache = String.format("delete %d %d", programNumber, input);
            sendRequest(cacheState.getPort(), requestToCache);
        }
    }

    private void processRequests() {
        try {
            synchronized (this.lock) {
                while (this.running) {
                    if (!this.requests.isEmpty()) {
                        processRequestsByCache();
                    }
                    if (!this.requests.isEmpty() && !this.workerStates.isEmpty()) {
                        System.err.println(requests);
                        RequestData requestData = this.requests.element();
                        System.err.println(requestData);
                        WorkerState workerState = this.workerStates.element();
                        int programWeight = this.programWeights[requestData.getLastProgram()];
                        if (this.totalWeight - workerState.getLoad() >= programWeight) {
                            this.requests.remove();
                            this.workerStates.remove();
                            String request = String.format("set %d %d null", requestData.getLastProgram(), requestData.getInput());
                            this.sendRequest(cacheState.getPort(), request);
                            workerState.setLoad(workerState.getLoad() + programWeight);
                            this.workerStates.add(workerState);
                            this.executorService.submit(() -> getOutputFromWorker(requestData, workerState));
                            continue;
                        }
                    }
                    try {
                        this.lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}