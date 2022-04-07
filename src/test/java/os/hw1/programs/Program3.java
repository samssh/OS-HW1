package os.hw1.programs;

import java.util.Scanner;

import static os.hw1.Config.WAIT_P3;


public class Program3 {
    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < WAIT_P3 - 200){
            Thread.sleep(100);
        }
        System.out.println(scanner.nextInt());
    }
}
