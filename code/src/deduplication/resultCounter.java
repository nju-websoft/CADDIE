package deduplication;

import util.PATHS;
import util.ReadFile;

import java.io.PrintWriter;
import java.util.*;

public class resultCounter {

    private static void getIdenticalCount(String ppjoinFile, String outputFile) {
        List<List<Integer>> records = ReadFile.readInteger(ppjoinFile, "\t");
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for (List<Integer> iter: records) {
                if (iter.get(2).equals(iter.get(3)) && iter.get(3).equals(iter.get(4))) {
                    writer.println(iter.get(0) + "\t" + iter.get(1) + "\t" + iter.get(2) + "\t" + iter.get(3) + "\t" + iter.get(4));
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void getPPJoinCount(String folder) {
        for (int i = 50; i <= 100; i += 5) {
            int size = ReadFile.readString(folder + "ppjoin-" + i + ".txt").size();
            System.out.println(i + "\t" + size);
        }
    }

    private static void getSubSetCount(String subsetFile, String sizeFile, String outputFile) {
        Map<Integer, Integer> dataset2size = new HashMap<>();
        for (List<Integer> iter: ReadFile.readInteger(sizeFile, "\t")) {
            dataset2size.put(iter.get(0), iter.get(1));
        }
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for (List<Integer> iter: ReadFile.readInteger(subsetFile, "\t")) {
                int size0 = dataset2size.get(iter.get(0));
                int size1 = dataset2size.get(iter.get(1));
                if (size0 != size1) {
                    writer.println(iter.get(0) + "\t" + iter.get(1) + "\t" + size0 + "\t" + size1);
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getIdenticalCluster(String identicalFile, String outputFile) {
        Map<Integer, Integer> dataset2size = new HashMap<>();
        Map<Integer, Integer> dataset2SetId = new HashMap<>();
        List<Set<Integer>> clusterList = new ArrayList<>();
        for (List<Integer> iter: ReadFile.readInteger(identicalFile, "\t")) {
            int dataset0 = iter.get(0);
            int dataset1 = iter.get(1);
            dataset2size.put(dataset0, iter.get(3));
            dataset2size.put(dataset1, iter.get(4));
            if (dataset2SetId.containsKey(dataset0)) {
                clusterList.get(dataset2SetId.get(dataset0)).add(dataset1);
                dataset2SetId.put(dataset1, dataset2SetId.get(dataset0));
            } else if (dataset2SetId.containsKey(dataset1)) {
                clusterList.get(dataset2SetId.get(dataset1)).add(dataset0);
                dataset2SetId.put(dataset0, dataset2SetId.get(dataset1));
            } else {
                Set<Integer> cluster = new TreeSet<>();
                cluster.add(dataset0);
                cluster.add(dataset1);
                dataset2SetId.put(dataset0, clusterList.size());
                dataset2SetId.put(dataset1, clusterList.size());
                clusterList.add(cluster);
            }
        }
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for (Set<Integer> iter: clusterList) {
                StringBuilder line = new StringBuilder();
                int size = 0;
                for (int i: iter) {
                    line.append(i).append("\t");
                    size = dataset2size.get(i);
                }
                writer.println(line.toString().trim() + ":" + size);
            }
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        getSubSetCount(PATHS.FileBase + "subset-or-duplicate-pair.txt", PATHS.FileBase + "dataset-sorted-by-size.txt", PATHS.FileBase + "subset-pair.txt");

//        getIdenticalCount(PATHS.FileBase + "ppjoin-95.txt", PATHS.FileBase + "ppjoin-100.txt");
//        getIdenticalCluster(PATHS.FileBase + "ppjoin-100.txt", PATHS.FileBase + "identical-cluster.txt");

        getPPJoinCount(PATHS.FileBase);

    }
}
