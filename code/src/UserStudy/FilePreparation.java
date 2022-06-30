package UserStudy;

import beans.Dataset;
import util.PATHS;
import util.ReadFile;

import java.io.PrintWriter;
import java.util.*;

import static util.readAllMetadata.getAllMetadata;

public class FilePreparation {
    private static final int CANDIDATE_NUM = 20;
    private static final int PAIR_REPEAT = 4;

    private static List<Integer> getRankedList(int length) {
        List<Integer> rankedList = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            rankedList.add(i);
        }
        return rankedList;
    }

    private static void generateOrders(String outputPath) {

        // if contained in reverseOrder, then show metadata only first.
        Set<Integer> reverse = new TreeSet<>();
        List<Integer> rankedList = getRankedList(CANDIDATE_NUM);
        Random random = new Random();
        for (int i = 0; i < CANDIDATE_NUM/2; i++) {
            int index = random.nextInt(rankedList.size());
            reverse.add(rankedList.remove(index));
        }
//        System.out.println(reverse);
        List<Integer> reverseList = new ArrayList<>(reverse);
        List<Integer> normalList = new ArrayList<>();
        for (int i = 0; i < CANDIDATE_NUM; i++) {
            if (!reverse.contains(i)) {
                normalList.add(i);
            }
        }
        System.out.println(normalList);
        System.out.println(reverseList);

        int[] pairOrder = new int[50];
        int ind = 0;
        rankedList = getRankedList(CANDIDATE_NUM);
        for (int i = 0; i < CANDIDATE_NUM ; i++) {
            int index = random.nextInt(rankedList.size());
            pairOrder[ind] = rankedList.get(index);
            ind++;
            rankedList.remove(index);
        }
        rankedList = getRankedList(CANDIDATE_NUM);
        for (int i = 0; i < CANDIDATE_NUM ; i++) {
            int index = random.nextInt(rankedList.size());
            pairOrder[ind] = rankedList.get(index);
            ind++;
            rankedList.remove(index);
        }
        rankedList = getRankedList(CANDIDATE_NUM);
        for (int i = 0; i < 50 % CANDIDATE_NUM ; i++) {
            int index = random.nextInt(rankedList.size());
            pairOrder[ind] = rankedList.get(index);
            ind++;
            rankedList.remove(index);
        }
        // then repeat the pairOrder in the reverse order. e.g., 1,2,3,4 -> 1,2,3,4,4,3,2,1
        System.out.println(Arrays.toString(pairOrder));

        //each
        try {
            for (int i = 0; i < normalList.size(); i++) {
                PrintWriter rankedWriter = new PrintWriter(outputPath + normalList.get(i) + ".txt");
                PrintWriter reverseWriter = new PrintWriter(outputPath + reverseList.get(i) + ".txt");
                for (int j = 0; j < 10; j++) {
                    int index = i * 10 + j;
                    if (index >= pairOrder.length) {
                        index = 99 - index;
                    }
                    if (j < 5) {
                        rankedWriter.println(pairOrder[index] + "\t" + 0);
                        reverseWriter.println(pairOrder[index] + "\t" + 1);
                    } else {
                        rankedWriter.println(pairOrder[index] + "\t" + 1);
                        reverseWriter.println(pairOrder[index] + "\t" + 0);
                    }
                }
                rankedWriter.close();
                reverseWriter.close();
            }

            PrintWriter orderWriter = new PrintWriter(outputPath + "order.txt");
            orderWriter.println(normalList);
            orderWriter.println(reverseList);
            orderWriter.println(Arrays.toString(pairOrder));
            orderWriter.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getCases(String outputFile) {
        Map<Integer, Dataset> allMetadata = getAllMetadata();
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for (List<String> iter: ReadFile.readString(PATHS.vldbBase + "user-study-cases.txt", "\t")) {
                int queryId = Integer.parseInt(iter.get(1));
                int datasetId = Integer.parseInt(iter.get(2));
                String queryString = iter.get(3);
                String[] keywords = iter.get(4).split("\\s+");
                Dataset dataset = allMetadata.get(datasetId);
                String title = dataset.getTitle();
                title = title.replaceAll("\\s*\\n\\s*", "[n]");
                title = title.replaceAll("\\s*\\r\\s*", "[n]");
                title = title.replaceAll("\\s*\t\\s*", " ");
                title = highlightAllKeywords(title, keywords);
                String desc = dataset.getDescription();
                desc = desc.replaceAll("\\s*\\n\\s*", "[n]");
                desc = desc.replaceAll("\\s*\\r\\s*", "[n]");
                desc = desc.replaceAll("\\s*\t\\s*", " ");
                desc = highlightAllKeywords(desc, keywords);
                writer.println(queryId + "\t" + queryString + "\t" + iter.get(4) + "\t" + datasetId + "\t" + title + "\t" + desc);
            }
            writer.close();
            for (List<String> iter: ReadFile.readString(outputFile, "\t")) {
                if (iter.size() != 6) {
                    System.out.println(iter);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getCasesPre(String outputFile) {
        Map<Integer, Dataset> allMetadata = getAllMetadata();
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for (List<String> iter: ReadFile.readString(PATHS.vldbBase + "user-study-cases.txt", "\t")) {
                int queryId = Integer.parseInt(iter.get(1));
                int datasetId = Integer.parseInt(iter.get(2));
                String queryString = iter.get(3);
                String[] keywords = iter.get(4).split("\\s+");
                Dataset dataset = allMetadata.get(datasetId);
                String title = dataset.getTitle();
                title = title.replaceAll("\\s*\\n\\s*", "[n]");
                title = title.replaceAll("\\s*\\r\\s*", "[n]");
                title = title.replaceAll("\\s*\t\\s*", " ");
                title = highlightAllKeywords(title, keywords);
                String desc = dataset.getDescription();
                desc = desc.replaceAll("\\s*\\n\\s*", "[n]");
                desc = desc.replaceAll("\\s*\\r\\s*", "[n]");
                desc = desc.replaceAll("\\s*\t\\s*", " ");
                writer.println(queryString + "\t" + datasetId + "\t" + desc);
            }
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getCasesFinal(String outputFile) {
        Map<Integer, Dataset> allMetadata = getAllMetadata();
        List<List<String>> descMap = ReadFile.readString(PATHS.vldbBase + "case-info-pre.txt", "\t");
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            int i = 0;
            for (List<String> iter: ReadFile.readString(PATHS.vldbBase + "user-study-cases.txt", "\t")) {
                int queryId = Integer.parseInt(iter.get(1));
                int datasetId = Integer.parseInt(iter.get(2));
                String queryString = iter.get(3);
                String[] keywords = iter.get(4).split("\\s+");
                Dataset dataset = allMetadata.get(datasetId);
                String title = dataset.getTitle();
                title = title.replaceAll("\\s*\\n\\s*", "[n]");
                title = title.replaceAll("\\s*\\r\\s*", "[n]");
                title = title.replaceAll("\\s*\t\\s*", " ");
                title = highlightAllKeywords(title, keywords);
                String desc = descMap.get(i).get(2);
                i++;
                desc = desc.replaceAll("\\s*\\n\\s*", "[n]");
                desc = desc.replaceAll("\\s*\\r\\s*", "[n]");
                desc = desc.replaceAll("\\s*\t\\s*", " ");
                desc = highlightAllKeywords(desc, keywords);
                writer.println(queryId + "\t" + queryString + "\t" + iter.get(4) + "\t" + datasetId + "\t" + title + "\t" + desc);
            }
            writer.close();
            for (List<String> iter: ReadFile.readString(outputFile, "\t")) {
                if (iter.size() != 6) {
                    System.out.println(iter);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String highlightAllKeywords(String source, String[] keywords) {
        String lowerSource = source.toLowerCase();

        Map<Integer, Integer> spanMap = new TreeMap<>();
        for (String keyword: keywords) {
            int len = keyword.length();
            int begin = 0;
            int ind = lowerSource.indexOf(keyword, begin);
            while (ind >= 0 && begin < source.length()) {
                int currentEnd = spanMap.getOrDefault(ind, -1);
                if (currentEnd < ind + len) {
                    spanMap.put(ind, ind + len);
                }
                begin = ind + len;
                ind = lowerSource.indexOf(keyword, begin);
            }
        }
        String target = "";
        int begin = 0;
        for (Map.Entry<Integer, Integer> iter: spanMap.entrySet()) {
            int ind = iter.getKey();
            int end = iter.getValue();
            if (begin <= ind) {
                target += source.substring(begin, ind);
                target += "<span style='color:red'>" + source.substring(ind, end) + "</span>";
                begin = end;
            } else if (begin < end) {
                target += "<span style='color:red'>" + source.substring(begin, end) + "</span>";
                begin = end;
            }
        }
        if (begin < source.length()) {
            target += source.substring(begin);
        }
        return target;
    }

    public static void main(String[] args) {
//        generateOrders(PATHS.vldbBase + "UserStudy/");
//        getCases(PATHS.vldbBase + "case-info-original.txt");
//        getCasesPre(PATHS.vldbBase + "case-info-pre.txt");
        getCasesFinal(PATHS.vldbBase + "case-info.txt");

    }
}
