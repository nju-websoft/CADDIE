package QPCSG;

import util.PATHS;
import util.ReadFile;

import java.io.PrintWriter;
import java.util.*;

public class prepareAllQDPairs {

    private static void getRestPair(String outputFile) {
        try {
            Map<Integer, Set<Integer>> existing = new HashMap<>();
            for (List<Integer> iter: ReadFile.readInteger(PATHS.vldbBase + "QPCSG-1000s-80.txt", "\t")) {
                int query = iter.get(0);
                int dataset = iter.get(1);
                Set<Integer> allds = existing.getOrDefault(query, new HashSet<>());
                allds.add(dataset);
                existing.put(query, allds);
            }
            for (List<Integer> iter: ReadFile.readInteger(PATHS.vldbBase + "QPCSG-30s-80.txt", "\t")) {
                if (iter.get(2) == 30000) {
                    continue;
                }
                int query = iter.get(0);
                int dataset = iter.get(1);
                Set<Integer> allds = existing.getOrDefault(query, new HashSet<>());
                allds.add(dataset);
                existing.put(query, allds);
            }
            Map<Integer, Set<Integer>> rest = new TreeMap<>();
            for (List<Integer> iter: ReadFile.readInteger(PATHS.vldbBase + "qrels.txt", "\t")) {
                int query = iter.get(0);
                int dataset = iter.get(2);
                Set<Integer> allds = existing.getOrDefault(query, new HashSet<>());
                if (!allds.contains(dataset)) {
                    Set<Integer> restds = rest.getOrDefault(query, new TreeSet<>());
                    restds.add(dataset);
                    rest.put(query, restds);
                }
            }
            Map<Integer, String> id2query = new HashMap<>();
            for (List<String> iter: ReadFile.readString(PATHS.vldbBase + "all_queries.txt", "\t")) {
                id2query.put(Integer.parseInt(iter.get(0)), iter.get(1));
            }

            int count = 0;
            PrintWriter writer = new PrintWriter(outputFile);
            for (Map.Entry<Integer, Set<Integer>> iter: rest.entrySet()) {
                int query = iter.getKey();
                for (int i: iter.getValue()) {
                    writer.println(query + "\t" + i + "\t" + id2query.get(query));
                    count++;
                }
            }
            writer.close();
            System.out.println(count);
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void getAllPair(String outputFile) {
        try {
            Map<Integer, Set<Integer>> all = new TreeMap<>();
            for (List<Integer> iter: ReadFile.readInteger(PATHS.vldbBase + "qrels.txt", "\t")) {
                int query = iter.get(0);
                int dataset = iter.get(2);
                Set<Integer> restds = all.getOrDefault(query, new TreeSet<>());
                restds.add(dataset);
                all.put(query, restds);
            }
            Map<Integer, String> id2query = new HashMap<>();
            for (List<String> iter: ReadFile.readString(PATHS.vldbBase + "all_queries.txt", "\t")) {
                id2query.put(Integer.parseInt(iter.get(0)), iter.get(1));
            }

            int count = 0;
            PrintWriter writer = new PrintWriter(outputFile);
            for (Map.Entry<Integer, Set<Integer>> iter: all.entrySet()) {
                int query = iter.getKey();
                for (int i: iter.getValue()) {
                    writer.println(query + "\t" + i + "\t" + id2query.get(query));
                    count++;
                }
            }
            writer.close();
            System.out.println(count);
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        getAllPair(PATHS.vldbBase + "pairs-all.txt");
    }
}
