package os.hw1.master;

import java.util.LinkedList;
import java.util.List;

public class WorkerState {
    private final int number;
    private final int port;
    private Process process;
    private int load;
    private final List<RequestData> requestDataList;

    public WorkerState(int number, int port) {
        this.number = number;
        this.port = port;
        this.load = 0;
        this.requestDataList = new LinkedList<>();
    }

    public long getPid() {
        return process.pid();
    }

    public int getPort() {
        return port;
    }

    public int getNumber() {
        return number;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public int getLoad() {
        return load;
    }

    public void setLoad(int load) {
        this.load = load;
    }

    public List<RequestData> getRequestDataList() {
        return requestDataList;
    }

    @Override
    public String toString() {
        return "WorkerState{" +
                "number=" + number +
                ", port=" + port +
                ", pid=" + getPid() +
                ", load=" + load +
                ", requestDataList=" + requestDataList +
                '}';
    }
}
