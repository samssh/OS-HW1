package os.hw1.cache;

import os.hw1.util.SocketServerHandler;

import java.util.*;

public class Cache {
    private final Object[] locks;
    private final SocketServerHandler socketServerHandler;
    private final List<Map<Integer, Integer>> data;

    public Cache(int port, int programCount) {
        this.locks = new Object[programCount];
        this.data = new ArrayList<>(programCount);
        for (int i = 0; i < programCount; i++) {
            locks[i] = new Object();
            data.add(new HashMap<>());
        }
        this.socketServerHandler = new SocketServerHandler(port, this::handleRequest);
        System.out.println();
    }

    public void start() {
        this.socketServerHandler.start();
    }

    private String handleRequest(String request) {
        Scanner scanner = new Scanner(request);
        String command = scanner.next();
        int programNum = scanner.nextInt();
        int input = scanner.nextInt();
        if (command.equals("check")) {
            return check(programNum, input);
        }
        if (command.equals("get")) {
            return get(programNum, input);
        }
        if (command.equals("set")) {
            String outputString = scanner.next();
            Integer output;
            if (outputString.equals("null")) output = null;
            else output = Integer.valueOf(outputString);
            return set(programNum, input, output);
        }
        if (command.equals("delete")) {
            return delete(programNum, input);
        }
        return "";
    }

    private String delete(int programNum, int input) {
        Map<Integer, Integer> dataMap = this.data.get(programNum);
        synchronized (locks[programNum]) {
            dataMap.remove(input);
            locks[programNum].notifyAll();
        }
        return "removed";
    }

    private String check(int programNum, int input) {
        Map<Integer, Integer> dataMap = this.data.get(programNum);
        synchronized (locks[programNum]) {
            if (dataMap.containsKey(input)) {
                return "true";
            } else {
                return "false";
            }
        }
    }

    private String get(int programNum, int input) {
        Map<Integer, Integer> dataMap = this.data.get(programNum);
        synchronized (locks[programNum]) {
            if (!dataMap.containsKey(input))
                return "not-exist";
            Integer result = dataMap.get(input);
            while (result == null) {
                try {
                    locks[programNum].wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!dataMap.containsKey(input))
                    return "not-exist";
                result = dataMap.get(input);
            }
            return String.valueOf(result);
        }
    }

    private String set(int programNum, int input, Integer output) {
        Map<Integer, Integer> dataMap = this.data.get(programNum);
        synchronized (locks[programNum]) {
            dataMap.put(input, output);
            if (output != null) {
                locks[programNum].notifyAll();
            }
            return "done";
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("please specify port and program count");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        int programCount = Integer.parseInt(args[1]);
        Cache cache = new Cache(port, programCount);
        cache.start();
    }
}
