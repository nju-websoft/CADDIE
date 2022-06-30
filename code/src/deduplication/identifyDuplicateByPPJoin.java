package deduplication;

import util.PATHS;
import util.ReadFile;

import java.io.PrintWriter;
import java.util.*;

public class identifyDuplicateByPPJoin {

    /**
     * Re-implement of the PPJoin+ algorithm to compute the similar dataset pairs.
     * @reference: Chuan Xiao, Wei Wang, Xuemin Lin, Jeffrey Xu Yu, Guoren Wang
     *  - Efficient similarity joins for near-duplicate detection. ACM Trans. Database Syst. 36(3): 15:1-15:41 (2011)
     */

    private static final double JACCARD_SIM_THRESHOLD = 0.95;

    private static void test(String file, String hashPath) {
        Set<String> allmsg = new HashSet<>();
        int count = 0;
        for (String iter: ReadFile.readString(file)) {
            String dataset = iter.split("\t")[0];
            allmsg.addAll(ReadFile.readString(hashPath + dataset + ".txt"));
            count++;
            if (count % 500 == 0) {
                System.out.println(count);
            }
        }
        System.out.println(allmsg.size());
    }

    private static void ppjoin(String sortedDatasetFile, String hashPath, String outputFile) throws Exception{
        List<Integer> datasets = new ArrayList<>();
        Map<Integer, Integer> dataset2size = new HashMap<>();
        for (List<Integer> iter: ReadFile.readInteger(sortedDatasetFile, "\t")) {
            datasets.add(iter.get(0));
            dataset2size.put(iter.get(0), iter.get(1));
        }
        // Initiate S, I
        List<List<Integer>> S = new ArrayList<>();
        Map<String, Set<List<Integer>>> I = new HashMap<>();

        PrintWriter writer = new PrintWriter(outputFile);

        int count = 0;
        // For each x in R do
        for (int dataset: datasets) {
            // Initialize A: empty map from record id to int
            Map<Integer, Integer> A = new HashMap<>();
            List<String> xData = ReadFile.readString(hashPath + dataset + ".txt");
            int xLength = xData.size();
            int p = xLength - (int) Math.ceil(JACCARD_SIM_THRESHOLD * xLength) + 1;

            for (int i = 0; i < p; i++) {
                String key = xData.get(i);
                Set<List<Integer>> yCandidate = I.getOrDefault(key, new HashSet<>());
                for (List<Integer> iter: yCandidate) {
                    int yId = iter.get(0);
                    int yLength = dataset2size.get(yId);
                    if (yLength < JACCARD_SIM_THRESHOLD * xLength) {
                        continue;
                    }
                    int alpha = (int) Math.ceil(JACCARD_SIM_THRESHOLD/(1 + JACCARD_SIM_THRESHOLD) * (xLength + yLength));
                    int ubound = 1 + Math.min(xLength - i - 1, yLength - iter.get(1) - 1);
                    int ay = A.getOrDefault(yId, 0);
                    if (ay + ubound >= alpha) {
                        A.put(yId, ay + 1);
                        //could be changed to ppjoin+
                    } else {
                        A.put(yId, 0);
                    }
                }
                yCandidate.add(new ArrayList<>(Arrays.asList(dataset, i)));
                I.put(key, yCandidate);
            }
            // verify
            for (Map.Entry<Integer, Integer> iter: A.entrySet()) {
                int yId = iter.getKey();
                int ay = iter.getValue();
                if (ay <= 0) {
                    continue;
                }
                Set<String> yData = new HashSet<>(ReadFile.readString(hashPath + yId + ".txt"));
                int yLength = yData.size();
                yData.retainAll(xData);
                int commomSize = yData.size();
                double percent = ((double) commomSize) / (yLength + xLength - commomSize);
                if (percent >= JACCARD_SIM_THRESHOLD) {
                    writer.println(dataset + "\t" + yId + "\t" + commomSize + "\t" + xLength + "\t" + yLength);
                    writer.flush();
                }
            }
            count++;
            if (count % 500 == 0) {
                System.out.println(count);
            }
        }
        writer.close();
    }

    private static void ppjoinWithSim(String sortedDatasetFile, String hashPath, String outputPath, double SIM) throws Exception{
        List<Integer> datasets = new ArrayList<>();
        Map<Integer, Integer> dataset2size = new HashMap<>();
        for (List<Integer> iter: ReadFile.readInteger(sortedDatasetFile, "\t")) {
            datasets.add(iter.get(0));
            dataset2size.put(iter.get(0), iter.get(1));
        }
        // Initiate S, I
        List<List<Integer>> S = new ArrayList<>();
        Map<String, Set<List<Integer>>> I = new HashMap<>();

        PrintWriter writer = new PrintWriter(outputPath + "ppjoin-" + (int)(SIM * 100) + ".txt");

        int count = 0;
        // For each x in R do
        for (int dataset: datasets) {
            // Initialize A: empty map from record id to int
            Map<Integer, Integer> A = new HashMap<>();
            List<String> xData = ReadFile.readString(hashPath + dataset + ".txt");
            int xLength = xData.size();
            int p = xLength - (int) Math.ceil(SIM * xLength) + 1;

            for (int i = 0; i < p; i++) {
                String key = xData.get(i);
                Set<List<Integer>> yCandidate = I.getOrDefault(key, new HashSet<>());
                for (List<Integer> iter: yCandidate) {
                    int yId = iter.get(0);
                    int yLength = dataset2size.get(yId);
                    if (yLength < SIM * xLength) {
                        continue;
                    }
                    int alpha = (int) Math.ceil(SIM/(1 + SIM) * (xLength + yLength));
                    int ubound = 1 + Math.min(xLength - i - 1, yLength - iter.get(1) - 1);
                    int ay = A.getOrDefault(yId, 0);
                    if (ay + ubound >= alpha) {
                        A.put(yId, ay + 1);
                        //could be changed to ppjoin+
                    } else {
                        A.put(yId, 0);
                    }
                }
                yCandidate.add(new ArrayList<>(Arrays.asList(dataset, i)));
                I.put(key, yCandidate);
            }
            // verify
            for (Map.Entry<Integer, Integer> iter: A.entrySet()) {
                int yId = iter.getKey();
                int ay = iter.getValue();
                if (ay <= 0) {
                    continue;
                }
                Set<String> yData = new HashSet<>(ReadFile.readString(hashPath + yId + ".txt"));
                int yLength = yData.size();
                yData.retainAll(xData);
                int commomSize = yData.size();
                double percent = ((double) commomSize) / (yLength + xLength - commomSize);
                if (percent >= SIM) {
                    writer.println(dataset + "\t" + yId + "\t" + commomSize + "\t" + xLength + "\t" + yLength);
                    writer.flush();
                }
            }
            count++;
            if (count % 500 == 0) {
                System.out.println(count);
            }
        }
        writer.close();
    }

    private static void ppjoinplus(String sortedDatasetFile, String hashPath, String outputFile) throws Exception{
        List<Integer> datasets = new ArrayList<>();
        Map<Integer, Integer> dataset2size = new HashMap<>();
        for (List<Integer> iter: ReadFile.readInteger(sortedDatasetFile, "\t")) {
            datasets.add(iter.get(0));
            dataset2size.put(iter.get(0), iter.get(1));
        }
        // Initiate S, I
//        List<List<Integer>> S = new ArrayList<>();
        Map<String, Set<List<Integer>>> I = new HashMap<>();

        PrintWriter writer = new PrintWriter(outputFile);

        int count = 0;
        // For each x in R do
        for (int dataset: datasets) {
            // Initialize A: empty map from record id to int
            Map<Integer, Integer> A = new HashMap<>();
            List<String> xData = ReadFile.readString(hashPath + dataset + ".txt");
            int xLength = xData.size();
            int p = xLength - (int) Math.ceil(JACCARD_SIM_THRESHOLD * xLength) + 1;

            for (int i = 0; i < p; i++) {
                String w = xData.get(i); // w <- x[i]
                Set<List<Integer>> yCandidate = I.getOrDefault(w, new HashSet<>());
                for (List<Integer> iter: yCandidate) {
                    int yId = iter.get(0);
                    int j = iter.get(1);
                    int yLength = dataset2size.get(yId);
                    if (yLength < JACCARD_SIM_THRESHOLD * xLength) { // size filtering on |y|
                        continue;
                    }
                    int alpha = (int) Math.ceil(JACCARD_SIM_THRESHOLD/(1 + JACCARD_SIM_THRESHOLD) * (xLength + yLength));
                    int ubound = 1 + Math.min(xLength - i - 1, yLength - j - 1);
                    int ay = A.getOrDefault(yId, 0);
                    if (ay + ubound >= alpha) {
                        A.put(yId, ay + 1);
                        if (ay == 0) { // line 12: change to ppjoin+
                            int hmax = xLength + yLength - 2 * (int) Math.ceil(JACCARD_SIM_THRESHOLD/(1 + JACCARD_SIM_THRESHOLD) * (xLength + yLength)) - i - j;

                            // H = SuffixFilter
                            List<String> yData = ReadFile.readString(hashPath + yId + ".txt");
                            int h = suffixFilter(xData.subList(i + 1, xLength), yData.subList(j + 1, yLength), hmax, 1);
                            //end: SuffixFilter

                            if (h <= hmax) {
                                A.put(yId, ay + 1);
                            } else {
                                A.put(yId, Integer.MIN_VALUE);
                            }
                        }
                    } else {
                        A.put(yId, 0);
                    }
                }
                yCandidate.add(new ArrayList<>(Arrays.asList(dataset, i))); // add (x, i) to Iw
                I.put(w, yCandidate);
            }
            // verify
            for (Map.Entry<Integer, Integer> yIter: A.entrySet()) {
                int yId = yIter.getKey();
                int ay = yIter.getValue();
                if (ay <= 0) {
                    continue;
                }
                List<String> yData = ReadFile.readString(hashPath + yId + ".txt");
                int yLength = yData.size();
                int py = yLength - (int) Math.ceil(JACCARD_SIM_THRESHOLD * yLength) + 1;
                String wx = xData.get(p - 1);
                String wy = yData.get(py - 1);

                int alpha = (int) Math.ceil(JACCARD_SIM_THRESHOLD/(1 + JACCARD_SIM_THRESHOLD) * (xLength + yLength));

                int O = ay;
                if (wx.compareTo(wy) < 0) {
                    int ubound = ay + xLength - p;
                    if (ubound >= alpha) {
                        Set<String> commonSuffix = new HashSet<>(xData.subList(p, xLength));
                        commonSuffix.retainAll(yData.subList(ay, yLength));
                        O += commonSuffix.size();
                    }
                } else {
                    int ubound = ay + yLength - py;
                    if (ubound >= alpha) {
                        Set<String> commonSuffix = new HashSet<>(xData.subList(ay, xLength));
                        commonSuffix.retainAll(yData.subList(py, yLength));
                        O += commonSuffix.size();
                    }
                }

                if (O >= alpha) {
                    Set<String> commomSet = new HashSet<>(xData);
                    commomSet.retainAll(yData);
                    writer.println(dataset + "\t" + yId + "\t" + commomSet.size() + "\t" + xLength + "\t" + yLength);
                    writer.flush();
                }
            }
            count++;
            if (count % 500 == 0) {
                System.out.println(count);
            }
        }
        writer.close();
    }

    private static final int MAXDEPTH = 10;

    private static int suffixFilter(List<String> xData, List<String> yData, int hmax, int d) {
        int xLength = xData.size();
        int yLength = yData.size();
        if (d > MAXDEPTH || xLength == 0 || yLength == 0) {
            return Math.abs(xLength - yLength);
        }
        int mid = (int) Math.ceil(((double) yLength)/2);
//        System.out.println(xData);
//        System.out.println(yData);
        String w = yData.get(mid - 1);
        int o = (hmax - Math.abs(xLength - yLength))/2;
        int ol, or;
        if (xLength < yLength) {
            ol = 1;
            or = 0;
        } else {
            ol = 0;
            or = 1;
        }
        // partition
        List<String> yl = new ArrayList<>();
        List<String> yr = new ArrayList<>();
        int f;
        int diff;
        int l = mid;
        int r = mid;
        if (yData.get(l - 1).compareTo(w) > 0 || yData.get(r - 1).compareTo(w) < 0) {
            f = 0;
            diff = 1;
        } else {
            int p = binarySearch(yData, l - 1, r - 1, w); // p >= 1
            yl = yData.subList(0, p - 1);
            if (yData.get(p - 1).equals(w)) {
                yr = yData.subList(p, yLength);
                diff = 0;
            } else {
                yr = yData.subList(p - 1, yLength);
                diff = 1;
            }
            f = 1;
        }
        //end
        //partition
        List<String> xl = new ArrayList<>();
        List<String> xr = new ArrayList<>();
        l = mid - o - Math.abs(xLength - yLength) * ol;
        r = mid + o + Math.abs(xLength - yLength) * or;
        if (l <= 0 || r <= 0) {
//            System.out.println(xLength + "\t" + yLength + "\t" + hmax);
            return Math.abs(xLength - yLength);
        }
        if (xData.get(l - 1).compareTo(w) > 0 || xData.get(r - 1).compareTo(w) < 0) {
            f = 0;
            diff = 1;
        } else {
            int p = binarySearch(xData, l - 1, r - 1, w);
            xl = xData.subList(0, p - 1);
            if (xData.get(p - 1).equals(w)) {
                xr = xData.subList(p, xLength);
                diff = 0;
            } else {
                xr = xData.subList(p - 1, xLength);
                diff = 1;
            }
            f = 1;
        }
        //end
        if (f == 0) {
            return hmax + 1;
        }
        int h = Math.abs(xl.size() - yl.size()) + Math.abs(xr.size() - yr.size()) + diff;
        if (h > hmax) {
            return h;
        } else {
            int hl = suffixFilter(xl, yl, hmax - Math.abs(xr.size() - yr.size())-diff, d+1);
            h = hl + Math.abs(xr.size() - yr.size()) + diff;
            if (h <= hmax) {
                int hr = suffixFilter(xr, yr, hmax - hl - diff, d+1);
                return hl + hr + diff;
            } else {
                return h;
            }
        }
    }

    private static int binarySearch(List<String> s, int left, int right, String w) {
        while (left <= right) {
            int mid = (left + right) / 2;
            if (s.get(mid).compareTo(w) >= 0) { //a[index] >= value
                if (mid == 0 || s.get(mid - 1).compareTo(w) < 0) { // index == 0 || a[index - 1] < value
                    return mid + 1;
                } else {
                    right = mid - 1;
                }
            } else {
                left = mid + 1;
            }
        }
        return right + 1;
    }

    public static void main(String[] args) {
        try {
//            ppjoin(PATHS.FileBase + "dataset-sorted-by-size.txt", PATHS.HashPath, PATHS.FileBase + "ppjoin-95.txt");
//            ppjoinplus(PATHS.FileBase + "dataset-sorted-by-size.txt", PATHS.HashPath, PATHS.FileBase + "ppjoinplus-95.txt");

            long start = System.currentTimeMillis();
            ppjoinWithSim(PATHS.FileBase + "dataset-sorted-by-size.txt", PATHS.HashPath, PATHS.FileBase, 0.05);
            long end = System.currentTimeMillis();
            System.out.println("Time(0.05): " + (end - start));

        }catch (Exception e) {
            e.printStackTrace();
        }

    }

}
