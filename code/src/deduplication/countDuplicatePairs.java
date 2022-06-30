package deduplication;

import beans.Dataset;
import org.apache.commons.text.similarity.LevenshteinDistance;
import util.PATHS;
import util.ReadFile;

import java.io.PrintWriter;
import java.util.*;

import static util.readAllMetadata.getAllMetadata;
import static util.readAllMetadata.getContentSimilarityMatrix;


public class countDuplicatePairs {
    private static final double lambda = 0.5;
    private static final int N = 10;

    static Map<Integer, Dataset> metadataMap;
    static Map<Set<Integer>, Double> contentSim;

    public countDuplicatePairs() {
        metadataMap = getAllMetadata();
        contentSim = getContentSimilarityMatrix();
    }

    private static void getSameTitle(String outputFile) {
        List<Dataset> metadata = new ArrayList<>(getAllMetadata().values());
        System.out.println(metadata.size());
        Collections.sort(metadata, new Comparator<Dataset>() {
            @Override
            public int compare(Dataset o1, Dataset o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });
        Map<String, Set<Integer>> sameTitle = new HashMap<>();
        for(int i = 1; i < metadata.size(); i++) {
            String last = metadata.get(i-1).getTitle().trim();
            String title = metadata.get(i).getTitle().trim();
            if(title.equals(last)) {
                Set<Integer> curr = sameTitle.getOrDefault(title, new TreeSet<>());
                curr.add(i-1);
                curr.add(i);
                sameTitle.put(title, curr);
            }
        }
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for(Set<Integer> set: sameTitle.values()) {
                List<Integer> ids = new ArrayList<>(set);
                for(int i = 0; i < ids.size(); i++) {
                    for(int j = i+1; j < ids.size(); j++) {
                        Dataset di = metadata.get(ids.get(i));
                        Dataset dj = metadata.get(ids.get(j));
                        String ti = di.getTitle().trim();
                        String tj = dj.getTitle().trim();
                        String desi = di.getDescription().trim();
                        String desj = dj.getDescription().trim();
                        if(ti.equals(tj) && desi.equals(desj)) {
                            writer.println(di.getId() + "\t" + dj.getId() + "\t" + 1);
                        } else if((ti.trim().contains(tj.trim()) || tj.trim().contains(ti.trim())) && (desi.trim().contains(desj.trim()) || desj.trim().contains(desi.trim()))) {
//                            writer.println(di.getId() + "\t" + dj.getId() + "\t" + 0);
                        }
                    }
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getAllPairsWithSameTitle(String outputFile) {
        List<Dataset> metadata = new ArrayList<>(getAllMetadata().values());
        System.out.println(metadata.size());
        Collections.sort(metadata, new Comparator<Dataset>() {
            @Override
            public int compare(Dataset o1, Dataset o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });
        Map<String, Set<Integer>> sameTitle = new HashMap<>();
        for(int i = 1; i < metadata.size(); i++) {
            String last = metadata.get(i-1).getTitle().trim();
            String title = metadata.get(i).getTitle().trim();
            if(title.equals(last)) {
                Set<Integer> curr = sameTitle.getOrDefault(title, new TreeSet<>());
                curr.add(i-1);
                curr.add(i);
                sameTitle.put(title, curr);
            }
        }
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for(Set<Integer> set: sameTitle.values()) {
                List<Integer> ids = new ArrayList<>(set);
                for(int i = 0; i < ids.size(); i++) {
                    for(int j = i+1; j < ids.size(); j++) {
                        int d1 = metadata.get(ids.get(i)).getId();
                        int d2 = metadata.get(ids.get(j)).getId();
                        if(d1 < d2) {
                            writer.println(d1 + "\t" + d2);
                        } else if (d1 > d2) {
                            writer.println(d2 + "\t" + d1);
                        }
                    }
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double getSim2Score(int dataset1, int dataset2, List<Double> sims) {
        LevenshteinDistance distance = new LevenshteinDistance();
        double metaSim = 0;
        Dataset d0 = metadataMap.get(dataset1);
        Dataset d1 = metadataMap.get(dataset2);

        // title
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
        sims.add(metaSim);
        sims.add(conSim);
        return result;
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

    private static void getAllDuplicatePairs(String outputFile) {
        Set<List<Integer>> candidates = new HashSet<>(ReadFile.readInteger(PATHS.vldbBase + "same-title-pair.txt", "\t"));
        for (List<Integer> iter: ReadFile.readInteger(PATHS.FileBase + "overlap-nonzero.txt", "\t")) {
            List<Integer> pair = new ArrayList<>();
            pair.add(iter.get(0));
            pair.add(iter.get(1));
            candidates.add(pair);
        }
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for (List<Integer> pair: candidates) {
                List<Double> sims = new ArrayList<>();
                double sim = getSim2Score(pair.get(0), pair.get(1), sims);
                if (sim > 0.9) {
                    writer.println(pair.get(0) + "\t" + pair.get(1) + "\t" + sim + "\t" + sims.get(0) + "\t" + sims.get(1));
                    writer.flush();
                }
            }
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void countData1Meta0(double limit) {
        int simData = 0;
        int disMeta = 0;
        for (List<Integer> iter: ReadFile.readInteger(PATHS.FileBase + "overlap-nonzero.txt", "\t")) {
            double score = ((double) iter.get(2))/((double) iter.get(3) + iter.get(4) - iter.get(2));
            if (score >= limit) {
//            if (score >= 1.0) {
                simData++;
                double metasim = getSim2Score(iter.get(0), iter.get(1));
                if (metasim < limit) {
//                if (metasim < 1.0) {
                    disMeta++;
                }
            }
        }
        System.out.println(simData);
        System.out.println(disMeta);
    }

    private static double getMetaSim3Pruned(int dataset1, int dataset2) {
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

        return metaSim;
    }

    private static double getConSim(int dataset1, int dataset2) {
        double conSim = contentSim.getOrDefault(new HashSet<>(Arrays.asList(dataset1, dataset2)), 0.0);
        return conSim;
    }

    private static void countMetaOverLimit(double limit, String outputFile) {
        List<Dataset> metadata = new ArrayList<>(getAllMetadata().values());
        System.out.println(metadata.size());
        Collections.sort(metadata, new Comparator<Dataset>() {
            @Override
            public int compare(Dataset o1, Dataset o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });
//        int count = 0;
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            for (int i = 0; i < metadata.size(); i++) {
                for (int j = i+1; j < metadata.size(); j++) {
                    double metaSim = getMetaSim3Pruned(metadata.get(i).getId(), metadata.get(j).getId());
                    if (metaSim >= limit) {
                        int d1 = metadata.get(i).getId();
                        int d2 = metadata.get(j).getId();
                        if (d1 < d2) {
                            writer.println(d1 + "\t" + d2);
                        } else {
                            writer.println(d2 + "\t" + d1);
                        }
                        writer.flush();
                    } else {
                        break;
                    }
                }
//                count++;
//                if (count % 1000 == 0) {
//                    System.out.println(count);
//                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void countData0Meta1(double limit) {
        Set<Set<Integer>> dataOver90 = new HashSet<>();
        for (List<Integer> iter: ReadFile.readInteger(PATHS.FileBase + "overlap-nonzero.txt", "\t")) {
            double score = ((double) iter.get(2))/((double) iter.get(3) + iter.get(4) - iter.get(2));
            if (score >= limit) {
                Set<Integer> pair = new HashSet<>();
                pair.add(iter.get(0));
                pair.add(iter.get(1));
                dataOver90.add(pair);
            }
        }
        double sim = 0;
        int count = 0;
        int all = 0;
        for (List<Integer> iter: ReadFile.readInteger(PATHS.vldbBase + "meta-sim-over90.txt", "\t")) {
            Set<Integer> pair = new HashSet<>(iter);
            sim += getConSim(iter.get(0), iter.get(1));
            if (!dataOver90.contains(pair)) {
                count++;
            }
            all++;
        }
        System.out.println(count);
        System.out.println(all);
        System.out.println(sim/all);
    }

    public static void main(String[] args) {

        countDuplicatePairs test = new countDuplicatePairs();

        getAllDuplicatePairs(PATHS.vldbBase + "all-duplicate-pairs.txt");

        countData1Meta0(0.9);
//        countMetaOverLimit(0.9, PATHS.vldbBase + "meta-sim-over90.txt");
        countData0Meta1(0.9);
    }
}
