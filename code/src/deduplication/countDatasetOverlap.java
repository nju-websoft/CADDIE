package deduplication;

import util.PATHS;
import util.ReadFile;

import java.io.PrintWriter;
import java.util.*;

public class countDatasetOverlap {

    private static void sortBySize(String datasets, String hashPath, String resultFile) {
        Map<Integer, Integer> dataset2size = new HashMap<>();

        for (String iter: ReadFile.readString(datasets)) {
            String dataset = iter.split("\t")[0];
            List<String> content = ReadFile.readString(hashPath + dataset + ".txt");
            dataset2size.put(Integer.parseInt(dataset), content.size());
        }
        List<Map.Entry<Integer, Integer>> tobeSortList = new ArrayList<>(dataset2size.entrySet());
        Collections.sort(tobeSortList, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o1.getValue() - o2.getValue();
            }
        });

        try {
            PrintWriter writer = new PrintWriter(resultFile);
            for (Map.Entry<Integer, Integer> iter: tobeSortList) {
                writer.println(iter.getKey() + "\t" + iter.getValue());
            }
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void recordOverlap(String datasetFile, String hashPath, String resultFile, int start, int end) {
        List<Integer> datasets = new ArrayList<>();
        for (String iter: ReadFile.readString(datasetFile)) {
            datasets.add(Integer.parseInt(iter.split("\t")[0]));
        }

        try {
            PrintWriter writer = new PrintWriter(resultFile);
            for (int i = 0; i < datasets.size() - 1; i++) {
                int ds1 = datasets.get(i);
                if (ds1 < start || ds1 > end) {
                    continue;
                }
                Set<String> content1 = new HashSet<>(ReadFile.readString(hashPath + ds1 + ".txt"));
                int size1 = content1.size();

                for (int j = i+1; j < datasets.size(); j++) {
                    int ds2 = datasets.get(j);
                    Set<String> content2 = new HashSet<>(ReadFile.readString(hashPath + ds2 + ".txt"));
                    int size2 = content2.size();

                    content2.retainAll(content1);
                    int commonSize = content2.size();

                    if (commonSize > size1/2 || commonSize > size2/2) {
                        writer.println(ds1 + "\t" + ds2 + "\t" + commonSize + "\t" + size1 + "\t" + size2);
                        writer.flush();
                    }
                }
                System.out.println("Finish: " + ds1);
            }
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        sortBySize(PATHS.FileBase + "dataset-resource.txt", PATHS.HashPath, PATHS.FileBase + "dataset-sorted-by-size.txt");
//        recordOverlap(PATHS.FileBase + "dataset-resource.txt", PATHS.HashPath, PATHS.FileBase + "dataset-overlap-over50-4.txt", 40000, 90000);
    }

}
