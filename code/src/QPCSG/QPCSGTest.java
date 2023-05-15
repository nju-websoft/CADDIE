package QPCSG;

import util.PATHS;
import util.ReadFile;

import java.io.PrintWriter;
import java.util.*;

public class QPCSGTest extends Thread{
    /**
     *
     * Queries are from the test-collection (ACORDAR, SIGIR 22)
     *
     */

    int queryId = 0;
    int dataset = 0;
    String queryString = "";
    private static double TAU = 0.8;
    public long lastTime = TIME_LIMIT;
    private static final long TIME_LIMIT = 1000000; //1000s

    public QPCSGTest(int queryId, int dataset, String queryString) {
        super();
        this.queryId = queryId;
        this.dataset = dataset;
        this.queryString = queryString;
    }

    public void run() {
        long start = System.currentTimeMillis();
        Set<String> result = GetResultTree.keywords2TripleTopEDP(queryString, dataset, TAU);
        lastTime = System.currentTimeMillis() - start;
        try {
            PrintWriter writer = new PrintWriter(PATHS.vldbBase + "QPCSGResult4/" + queryId + "-" + dataset + ".txt");
            for (String iter: result) {
                writer.println(iter);
            }
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void test(double tau) {
        TAU = tau;
        try {
            PrintWriter writer = new PrintWriter(PATHS.vldbBase + "QPCSG-all-1000s-" + (int)(TAU * 100) +".txt");

            for (List<String> iter: ReadFile.readString(PATHS.vldbBase + "pairs-all.txt", "\t")) {
                int query = Integer.parseInt(iter.get(0));
                int dataset = Integer.parseInt(iter.get(1));
                QPCSGTest thread = new QPCSGTest(query, dataset, iter.get(2));
                thread.start();
                thread.join(TIME_LIMIT);
                writer.println(iter.get(0) + "\t" + iter.get(1) + "\t" + thread.lastTime);
                writer.flush();
                thread.stop();
            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        test(0.8);
    }

}
