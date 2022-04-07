package os.hw1.master;

import java.util.Scanner;

public class MasterMain {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int port = scanner.nextInt();
        int workerCount = scanner.nextInt();
        int totalWeight = scanner.nextInt();
        int commonArgsCount = scanner.nextInt();
        String[] commonArgs = new String[commonArgsCount];
        for (int i = 0; i < commonArgsCount; i++) {
            commonArgs[i] = scanner.next();
        }
        int programsCount = scanner.nextInt();
        String[] programNames = new String[programsCount];
        int[] programWeights = new int[programsCount];
        for (int i = 0; i < programsCount; i++) {
            programNames[i] = scanner.next();
            programWeights[i] = scanner.nextInt();
        }
        Master master = new Master(port, workerCount, totalWeight, commonArgs, programNames, programWeights);
        master.start();
    }
}
