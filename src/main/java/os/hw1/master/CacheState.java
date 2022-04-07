package os.hw1.master;

public class CacheState {
    private final Process process;
    private final int port;

    public CacheState(Process process, int port) {
        this.process = process;
        this.port = port;
    }

    public long getPid() {
        return process.pid();
    }

    public int getPort() {
        return port;
    }

    public Process getProcess() {
        return process;
    }
}
