package os.hw1.master;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class RequestData {
    private final long time;
    private final List<Integer> programs;
    private int input;

    public RequestData(String[] programs, int input) {
        this.time = System.nanoTime();
        this.programs = Arrays.stream(programs).map(Integer::valueOf)
                .map(i -> i - 1).collect(Collectors.toCollection(LinkedList::new));
        this.input = input;
    }

    public long getTime() {
        return time;
    }

    public List<Integer> getPrograms() {
        return programs;
    }

    public int getInput() {
        return input;
    }

    public void setInput(int input) {
        this.input = input;
    }

    public int getLastProgram() {
        return programs.get(programs.size() - 1);
    }

    public void removeLastProgram() {
        programs.remove(programs.size() - 1);
    }

    @Override
    public String toString() {
        return "RequestData{" +
                "time=" + time +
                ", programs=" + programs +
                ", input=" + input +
                '}';
    }
}
