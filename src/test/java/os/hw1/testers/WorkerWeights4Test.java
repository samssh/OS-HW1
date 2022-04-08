package os.hw1.testers;

import org.junit.Test;
import os.hw1.ProcessRunner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static os.hw1.Config.WAIT_P1;
import static os.hw1.Config.WAIT_P2;

public class WorkerWeights4Test extends BaseTester {
    @Override
    public void setUpProcess() throws Exception {
        runningProcess = new ProcessRunner().setWorkerCount(3).setW(3).runProcess();
    }

    @Test(timeout = 10000)
    public void workerWeights4Test() throws InterruptedException, ExecutionException {
        Future<Response> responseFuture1 = executorService.submit(() -> sendRequest(30, 1, 1, 1, 1, 1));
        Future<Response> responseFuture2 = executorService.submit(() -> sendRequest(20, 1, 1, 1, 1, 1));
        Future<Response> responseFuture3 = executorService.submit(() -> sendRequest(10, 1, 1, 1, 1, 1));
        Thread.sleep(500);
        Response result1 = responseFuture1.get();
        assertTime(result1.time, 5 * WAIT_P1);
        assertEquals(result1.output, 25);
        Response result2 = responseFuture2.get();
        assertTime(result2.time, 5 * WAIT_P1);
        assertEquals(result2.output, 15);
        Response result3 = responseFuture3.get();
        assertTime(result3.time, 5 * WAIT_P1);
        assertEquals(result3.output, 5);
    }
}
