package os.hw1.programs;

import java.util.Scanner;

import static os.hw1.Config.WAIT_P2;


public class Program2 {
    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < WAIT_P2 - 100){
            Thread.sleep(100);
        }
        System.out.println((scanner.nextInt() / 2) % 5);
    }
}
