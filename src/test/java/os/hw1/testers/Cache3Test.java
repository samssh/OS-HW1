package os.hw1.testers;

import org.junit.Test;
import os.hw1.ProcessRunner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static os.hw1.Config.*;

public class Cache3Test extends BaseTester {
    @Override
    public void setUpProcess() throws Exception {
        runningProcess = new ProcessRunner().setW(3).runProcess();
    }

    @Test(timeout = 10000)
    public void cache3Test() throws InterruptedException, ExecutionException {
        Future<Response> r1 = executorService.submit(() -> sendRequest(50, 3, 3, 3));
        Response result1 = r1.get();
        assertTime(result1.time, WAIT_P3);
        assertEquals(result1.output, 50);
    }
}
