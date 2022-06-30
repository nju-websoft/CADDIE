package ReRanking;

import beans.Dataset;
import org.apache.commons.text.similarity.LevenshteinDistance;
import util.PATHS;
import util.ReadFile;

import java.io.PrintWriter;
import java.util.*;

import static util.readAllMetadata.getAllMetadata;
import static util.readAllMetadata.getContentSimilarityMatrix;

public class MMRTest {

    private static final double lambda = 0.5;
    private static final int N = 10;

    static Map<Integer, Dataset> metadataMap;
    static Map<Set<Integer>, Double> contentSim;

    public MMRTest() {
        metadataMap = getAllMetadata();
        contentSim = getContentSimilarityMatrix();
    }

    private static List<Integer> getMMRResult(Map<Integer, Double> originalList) {

        List<Integer> result = new ArrayList<>();

        while (result.size() < N && !originalList.isEmpty()) {
            double score = -Double.MAX_VALUE;
            int select = -1;
            for (Map.Entry<Integer, Double> entry: originalList.entrySet()) {
                int dataset = entry.getKey();
                double sim1 = entry.getValue();
                double sim2 = 0;
                for (int iter: result) {
                    double tempScore = getSim2Score(dataset, iter);
                    if (sim2 < tempScore) {
                        sim2 = tempScore;
                    }
                }
                double mmrScore = (lambda * sim1 - (1 - lambda) * sim2);
                if (score < mmrScore) {
                    score = mmrScore;
                    select = dataset;
                }
            }
            if (select == -1) {
                break;
            }
            result.add(select);
            originalList.remove(select);
//            System.out.print(score + "\t");
        }
//        System.out.println();
        return result;
    }

    private static void runMMRk(String outputFile, int k) {
        Map<Integer, Map<Integer, Double>> originalLists = new TreeMap<>();
        double currMax = 0;
        for(List<String> line: ReadFile.readString(PATHS.vldbBase + "MMRTest/BM25F_top100.txt", "\t")) {
            if(Integer.parseInt(line.get(3)) > k) {
                continue;
            }
            int query = Integer.parseInt(line.get(0));
            if (!originalLists.containsKey(query)) {
                currMax = Double.parseDouble(line.get(4));
            }
            Map<Integer, Double> list = originalLists.getOrDefault(query, new HashMap<>());
            list.put(Integer.parseInt(line.get(2)), Double.parseDouble(line.get(4))/currMax);
            originalLists.put(query, list);
        }
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for (Map.Entry<Integer, Map<Integer, Double>> iter: originalLists.entrySet()) {
                List<Integer> result = getMMRResult(iter.getValue());
                String s = "";
                for(int i: result) {
                    s += i + " ";
                }
                writer.println(iter.getKey() + "\t" + s.trim());
                writer.flush();
            }
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double getSim2Score(int dataset1, int dataset2) {
        LevenshteinDistance distance = new LevenshteinDistance();
        double metaSim = 0;
        Dataset d0 = metadataMap.get(dataset1);
        Dataset d1 = metadataMap.get(dataset2);

        /// title
        String title0 = d0.getTitle().trim();
        String title1 = d1.getTitle().trim();
        if (title0.equals(title1)) {
            metaSim += 0.3;
        } else {
            metaSim += 0.3 * (1 - ((double) distance.apply(title0, title1))/((double) Math.max(title0.length(), title1.length())));
        }
        // desc
        String desc0 = d0.getDescription().trim();
        String desc1 = d1.getDescription().trim();
        if (desc0.equals(desc1)) {
            metaSim += 0.3;
        } else {
            metaSim += 0.3 * (1 - ((double) distance.apply(desc0, desc1))/((double) Math.max(desc0.length(), desc1.length())));
        }
        // author
        String author0 = d0.getAuthor().trim();
        String author1 = d1.getAuthor().trim();
        if (author0.equals(author1)) {
            metaSim += 0.2;
        }
        // url
        String url0 = d0.getUrl().trim();
        String url1 = d1.getUrl().trim();
        if (url0.equals(url1)) {
            metaSim += 0.1;
        }

        // license
        String license0 = d0.getLicense().trim();
        String license1 = d1.getLicense().trim();
        if (license0.equals(license1)) {
            metaSim += 0.1;
        }

        double conSim = contentSim.getOrDefault(new HashSet<>(Arrays.asList(dataset1, dataset2)), 0.0);
        double result = 1; // if metaSim == 1 || conSim == 1
        if (metaSim != 1 && conSim != 1) {
            result = 1 - (2 * (1 - conSim) * (1 - metaSim))/ (1 - conSim + 1 - metaSim);
        }
//        System.out.println(result);
        return result;
    }

    private static void getResultSim(String inputFile, String outputFile, int top0) {
        Map<Integer, List<Integer>> originalLists = new TreeMap<>();
        for(List<String> line: ReadFile.readString(inputFile, "\t")) {
            int query = Integer.parseInt(line.get(0));
            List<Integer> list = originalLists.getOrDefault(query, new ArrayList<>());
            list.add(Integer.parseInt(line.get(2)));
            originalLists.put(query, list);
        }
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for (Map.Entry<Integer, List<Integer>> iter: originalLists.entrySet()) {
                List<Integer> list = iter.getValue();
                int count = 0;
                double sim = 0.0;
                int top = Math.min(top0, list.size());
                for (int i = 0; i < top; i++) {
                    for(int j = i+1; j < top; j++) {
                        sim += getSim2Score(list.get(i), list.get(j));
                        count++;
                    }
                }
                writer.println(iter.getKey() + "\t" + sim/count);
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double getResultSimCount(String inputFile, int top0, double limit) {
        Map<Integer, List<Integer>> originalLists = new TreeMap<>();
        for(List<String> line: ReadFile.readString(inputFile, "\t")) {
            int query = Integer.parseInt(line.get(0));
            List<Integer> list = originalLists.getOrDefault(query, new ArrayList<>());
            list.add(Integer.parseInt(line.get(2)));
            originalLists.put(query, list);
        }
        int record = 0;
        int count = 0;
        for (Map.Entry<Integer, List<Integer>> iter: originalLists.entrySet()) {
            List<Integer> list = iter.getValue();
//            int top = Math.min(top0, list.size());
            if(top0 > list.size()) {
                continue;
            }
            record++;
            for (int i = 0; i < top0; i++) {
                for(int j = i+1; j < top0; j++) {
                    double sim = getSim2Score(list.get(i), list.get(j));
                    if(sim >= limit) {
                        count++;
                    }
                }
            }
        }
        return ((double) count)/record;
    }

    private static void getSim() {
        // note to change getSim2ScoreX !!
        getResultSim(PATHS.vldbBase + "MMRTest/BM25F_top100.txt", PATHS.vldbBase + "MMRTest/sim-bm25-5-3.txt", 5);
        getResultSim(PATHS.vldbBase + "MMRTest/BM25F [d]_top100.txt", PATHS.vldbBase + "MMRTest/sim-bm25d-5-3.txt", 5);
        getResultSim(PATHS.vldbBase + "MMRTest/BM25F [m]_top100.txt", PATHS.vldbBase + "MMRTest/sim-bm25m-5-3.txt", 5);
        getResultSim(PATHS.vldbBase + "MMRTest/mmr30-3.txt", PATHS.vldbBase + "MMRTest/sim-mmr30-5-3.txt", 5);
    }

    private static void getSimCount(int top0, double limit) {
        System.out.println("bm25: " + getResultSimCount(PATHS.vldbBase + "MMRTest/BM25F_top100.txt", top0, limit));
        System.out.println("bm25m: " + getResultSimCount(PATHS.vldbBase + "MMRTest/BM25F [m]_top100.txt", top0, limit));
        System.out.println("bm25d: " + getResultSimCount(PATHS.vldbBase + "MMRTest/BM25F [d]_top100.txt", top0, limit));
        System.out.println("mmr30: " + getResultSimCount(PATHS.vldbBase + "MMRTest/mmr30-3.txt", top0, limit));
    }

    public static void main(String[] args) {
        MMRTest test = new MMRTest();
        runMMRk(PATHS.vldbBase + "MMRTest/mmr30-3.txt", 30);

        // the following: for evaluation
//        getSim();

//        System.out.println("top 5(0.5): ");
//        getSimCount(5, 0.5);
//        System.out.println("top 10(0.5): ");
//        getSimCount(10, 0.5);

    }
}
