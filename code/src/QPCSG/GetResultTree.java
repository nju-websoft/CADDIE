package QPCSG;

import beans.RDFTerm;
import HubLabel.DOGST.AnsTree;
import HubLabel.DOGST.CommonStruct;
import HubLabel.DOGST.DOGST;
import HubLabel.DOGST.TreeEdge;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import util.DBUtil;
import util.PATHS;
import util.ReadFile;

import java.io.FileReader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class GetResultTree {
    public static final int SNIPPET_MAX_SIZE = 80; //40
    public static final int MAX_COMPONENT_NUM = 1; //2

    public static Set<String> keywords2Triple(String queryString, int dataset) {
        List<String> keywords = new ArrayList<>();
        Set<String> resultSnippet = new HashSet<>();
        try {
            StandardAnalyzer analyzer = new StandardAnalyzer(new FileReader(PATHS.FileBase + "nltk-stopwords-3.6.2.txt"));
            TokenStream tokenStream = analyzer.tokenStream("content", queryString);
            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                keywords.add(attr.toString());
            }
            tokenStream.close();

            Map<Integer, Integer> edp2freq = new HashMap<>();
            int entitySum = 0;
            for (List<String> iter: ReadFile.readString(PATHS.IndexPath + "EDPIndex/" + dataset + ".txt", "\t")) {
                int count = Integer.parseInt(iter.get(1));
                edp2freq.put(Integer.parseInt(iter.get(0)), count);
                entitySum += count;
            }

            Map<Integer, Set<Integer>> comp2edp = new HashMap<>();
            Map<Integer, Set<Integer>> comp2keywords = new HashMap<>();
            String componentFolder = PATHS.IndexPath + "ComponentIndex/" + dataset + "/";

            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(componentFolder)));
            for (int i = 0; i < reader.maxDoc(); i++) {
                Document document = reader.document(i);
                int id = Integer.parseInt(document.get("id"));
                Set<Integer> edpSet = new HashSet<>();
                for (String s: document.get("edp").split(" ")) {
                    edpSet.add(Integer.parseInt(s));
                }
                comp2edp.put(id, edpSet);
                comp2keywords.put(id, new HashSet<>());
            }
            IndexSearcher searcher = new IndexSearcher(reader);
            int keywordHitCount = 0;
            for (int i = 0; i < keywords.size(); i++) {
                QueryParser parser = new QueryParser("text", analyzer);
                Query query = parser.parse(keywords.get(i));
                ScoreDoc[] scores = searcher.search(query, 1000000).scoreDocs;
                for (ScoreDoc score: scores) {
                    int id = Integer.parseInt(searcher.doc(score.doc).get("id"));
                    comp2keywords.get(id).add(i);
                }
                if (scores.length > 0) {
                    keywordHitCount++;
                }
            }

            Set<Integer> coveredEDP = new HashSet<>();
            Set<Integer> coveredKeywords = new HashSet<>();
            String baseFolder = PATHS.ProjectBase + "HubLabel/" + dataset + "/";
            String nodeIndexFolder = PATHS.IndexPath + "NodeLabel/" + dataset + "/";

            IndexSearcher tripleSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(nodeIndexFolder))));
            List<Integer> nodesId = ReadFile.readInteger(baseFolder + "subName.txt");

            long searchStart = System.currentTimeMillis();
            int time = 0;
            while (resultSnippet.size() < SNIPPET_MAX_SIZE && time < MAX_COMPONENT_NUM) {
                time++;
//                System.out.println(time);
                double score = 0;
                int comp = 0;
                for (Map.Entry<Integer, Set<Integer>> entry: comp2edp.entrySet()) {
                    int current = entry.getKey();
                    int freqCount = 0;
                    entry.getValue().removeAll(coveredEDP);
                    for (int edp: entry.getValue()) {
                        freqCount += edp2freq.get(edp);
                    }
                    Set<Integer> compKeywords = comp2keywords.get(current);
                    if (!compKeywords.isEmpty()) {
                        compKeywords.removeAll(coveredKeywords);
                    }
                    double currentScore = 0;
                    if (keywordHitCount != 0) {
                        currentScore = ((double) freqCount)/entitySum + ((double) compKeywords.size())/keywordHitCount;
                    } else {
                        currentScore = ((double) freqCount)/entitySum;
                    }

                    if (score < currentScore) {
                        score = currentScore;
                        comp = current;
                    }
                }
                if (comp == 0) {
                    break;
                }

                CommonStruct c1 = new CommonStruct();
                c1.Init2(baseFolder);
                DOGST k = new DOGST(c1); // keyKG+
                List<String> keys = new ArrayList<>();
                for (int edp: comp2edp.get(comp)) {
                    keys.add(String.valueOf(edp));
                }
                for (int key: comp2keywords.get(comp)) {
                    keys.add("\"" + keywords.get(key) + "\"");
                }

//                System.out.println("Current comp: " + comp);
//                System.out.println(keys);
//                System.out.println("Key size: " + keys.size());
//                System.out.println(score);

                AnsTree resultTree = k.search(c1, keys, 2);
                Set<Integer> nodeSet = new HashSet<>();
                for (TreeEdge edge: resultTree.edge) {
                    nodeSet.add(edge.u);
                    nodeSet.add(edge.v);
                }

                for (int node: nodeSet) {
                    Query query = IntPoint.newExactQuery("node", nodesId.get(node));
                    ScoreDoc[] docs = tripleSearcher.search(query, 1).scoreDocs;
                    Document document = tripleSearcher.doc(docs[0].doc);
                    resultSnippet.addAll(Arrays.asList(document.get("triple").split(",")));
                }

                coveredEDP.addAll(comp2edp.get(comp));
                coveredKeywords.addAll(comp2keywords.get(comp));
            }

//            System.out.println("Search time: " + (System.currentTimeMillis() - searchStart));

//            for (String triple: resultSnippet) {
//                System.out.println(triple);
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultSnippet;

    }

    // retain top components
    public static Set<String> keywords2Triple(String queryString, int dataset, double tau) {
        List<String> keywords = new ArrayList<>();
        Set<String> resultSnippet = new HashSet<>();
        try {
            StandardAnalyzer analyzer = new StandardAnalyzer(new FileReader(PATHS.FileBase + "nltk-stopwords-3.6.2.txt"));
            TokenStream tokenStream = analyzer.tokenStream("content", queryString);
            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                keywords.add(attr.toString());
            }
            tokenStream.close();

            Map<Integer, Integer> edp2freq = new HashMap<>();
            int entitySum = 0;
            for (List<String> iter: ReadFile.readString(PATHS.IndexPath + "EDPIndex/" + dataset + ".txt", "\t")) {
                int count = Integer.parseInt(iter.get(1));
                edp2freq.put(Integer.parseInt(iter.get(0)), count);
                entitySum += count;
            }

            Map<Integer, Set<Integer>> comp2edp = new HashMap<>();
            Map<Integer, Set<Integer>> comp2keywords = new HashMap<>();
            String componentFolder = PATHS.IndexPath + "ComponentIndex/" + dataset + "/";

            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(componentFolder)));
            for (int i = 0; i < reader.maxDoc(); i++) {
                Document document = reader.document(i);
                int id = Integer.parseInt(document.get("id"));
                Set<Integer> edpSet = new HashSet<>();
                for (String s: document.get("edp").split(" ")) {
                    edpSet.add(Integer.parseInt(s));
                }
                comp2edp.put(id, edpSet);
                comp2keywords.put(id, new HashSet<>());
            }
            IndexSearcher searcher = new IndexSearcher(reader);
            int keywordHitCount = 0;
            for (int i = 0; i < keywords.size(); i++) {
                QueryParser parser = new QueryParser("text", analyzer);
                Query query = parser.parse(keywords.get(i));
                ScoreDoc[] scores = searcher.search(query, 1000000).scoreDocs;
                for (ScoreDoc score: scores) {
                    int id = Integer.parseInt(searcher.doc(score.doc).get("id"));
                    comp2keywords.get(id).add(i);
                }
                if (scores.length > 0) {
                    keywordHitCount++;
                }
            }

            Set<Integer> coveredEDP = new HashSet<>();
            Set<Integer> coveredKeywords = new HashSet<>();
            String baseFolder = PATHS.ProjectBase + "HubLabel/" + dataset + "/";
            String nodeIndexFolder = PATHS.IndexPath + "NodeLabel/" + dataset + "/";

            IndexSearcher tripleSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(nodeIndexFolder))));
            List<Integer> nodesId = ReadFile.readInteger(baseFolder + "subName.txt");

            long searchStart = System.currentTimeMillis();
            int time = 0;
            int coveredEntityCount = 0;
            while (resultSnippet.size() < SNIPPET_MAX_SIZE && time < MAX_COMPONENT_NUM) {
                time++;
//                System.out.println(time);
                double score = 0;
                int comp = 0;
                for (Map.Entry<Integer, Set<Integer>> entry: comp2edp.entrySet()) {
                    int current = entry.getKey();
                    int freqCount = 0;
                    entry.getValue().removeAll(coveredEDP);
                    for (int edp: entry.getValue()) {
                        freqCount += edp2freq.get(edp);
                    }
                    Set<Integer> compKeywords = comp2keywords.get(current);
                    if (!compKeywords.isEmpty()) {
                        compKeywords.removeAll(coveredKeywords);
                    }
                    double currentScore = 0;
                    if (keywordHitCount != 0) {
                        currentScore = ((double) freqCount)/entitySum + ((double) compKeywords.size())/keywordHitCount;
                    } else {
                        currentScore = ((double) freqCount)/entitySum;
                    }

                    if (score < currentScore) {
                        score = currentScore;
                        comp = current;
                    }
                }
                if (comp == 0) {
                    break;
                }

                CommonStruct c1 = new CommonStruct();
                c1.Init2(baseFolder);
                DOGST k = new DOGST(c1); // keyKG+
                List<String> keys = new ArrayList<>();
                for (int edp: comp2edp.get(comp)) {
                    keys.add(String.valueOf(edp));

                    coveredEntityCount += edp2freq.get(edp);
                }
                for (int key: comp2keywords.get(comp)) {
                    keys.add("\"" + keywords.get(key) + "\"");
                }

//                System.out.println("Current comp: " + comp);
//                System.out.println(keys);
//                System.out.println("Key size: " + keys.size());
//                System.out.println(score);

                AnsTree resultTree = k.search(c1, keys, 2);
                Set<Integer> nodeSet = new HashSet<>();
                for (TreeEdge edge: resultTree.edge) {
                    nodeSet.add(edge.u);
                    nodeSet.add(edge.v);
                }

                for (int node: nodeSet) {
                    Query query = IntPoint.newExactQuery("node", nodesId.get(node));
                    ScoreDoc[] docs = tripleSearcher.search(query, 1).scoreDocs;
                    Document document = tripleSearcher.doc(docs[0].doc);
                    resultSnippet.addAll(Arrays.asList(document.get("triple").split(",")));
                }

                coveredEDP.addAll(comp2edp.get(comp));
                coveredKeywords.addAll(comp2keywords.get(comp));

                if (coveredEntityCount >= tau * entitySum) {
                    break;
                }
            }

//            System.out.println("Search time: " + (System.currentTimeMillis() - searchStart));

//            for (String triple: resultSnippet) {
//                System.out.println(triple);
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultSnippet;

    }

    // retain top EDPs
    public static Set<String> keywords2TripleTopEDP(String queryString, int dataset, double tau) {
        List<String> keywords = new ArrayList<>();
        Set<String> resultSnippet = new HashSet<>();
        String keywordsUsed = "";
        try {
            StandardAnalyzer analyzer = new StandardAnalyzer(new FileReader(PATHS.FileBase + "nltk-stopwords-3.6.2.txt"));
            TokenStream tokenStream = analyzer.tokenStream("content", queryString);
            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                keywords.add(attr.toString());
                keywordsUsed += attr + " ";
            }
            tokenStream.close();
            System.out.print(dataset + "\t" + queryString + "\t" + keywordsUsed.trim() + "\t");

            Map<Integer, Integer> edp2freq = new HashMap<>();
            int entitySum = ReadFile.readString(PATHS.IndexPath + "Entity2EDP/" + dataset + ".txt").size();
            double coveredEntitySum = tau * entitySum;
            int entityCount = 0;
            for (List<String> iter: ReadFile.readString(PATHS.IndexPath + "EDPIndex/" + dataset + ".txt", "\t")) {
                int count = Integer.parseInt(iter.get(1));
                edp2freq.put(Integer.parseInt(iter.get(0)), count);
                entityCount += count;
                if (entityCount >= coveredEntitySum) {
                    break;
                }
            }
            Set<Integer> allEDP = edp2freq.keySet();

            Map<Integer, Set<Integer>> comp2edp = new HashMap<>();
            Map<Integer, Set<Integer>> comp2keywords = new HashMap<>();
            String componentFolder = PATHS.IndexPath + "ComponentIndex/" + dataset + "/";

            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(componentFolder)));
            for (int i = 0; i < reader.maxDoc(); i++) {
                Document document = reader.document(i);
                int id = Integer.parseInt(document.get("id"));
                Set<Integer> edpSet = new HashSet<>();
                for (String s: document.get("edp").split(" ")) {
                    edpSet.add(Integer.parseInt(s));
                }
                edpSet.retainAll(allEDP);
                comp2edp.put(id, edpSet);
                comp2keywords.put(id, new HashSet<>());
            }
            IndexSearcher searcher = new IndexSearcher(reader);
            int keywordHitCount = 0;
            for (int i = 0; i < keywords.size(); i++) {
                QueryParser parser = new QueryParser("text", analyzer);
                Query query = parser.parse(keywords.get(i));
                ScoreDoc[] scores = searcher.search(query, 1000000).scoreDocs;
                for (ScoreDoc score: scores) {
                    int id = Integer.parseInt(searcher.doc(score.doc).get("id"));
                    comp2keywords.get(id).add(i);
                }
                if (scores.length > 0) {
                    keywordHitCount++;
                }
            }

            Set<Integer> coveredEDP = new HashSet<>();
            Set<Integer> coveredKeywords = new TreeSet<>();
            String baseFolder = PATHS.ProjectBase + "HubLabel/" + dataset + "/";
            String nodeIndexFolder = PATHS.IndexPath + "NodeLabel/" + dataset + "/";

            IndexSearcher tripleSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(nodeIndexFolder))));
            List<Integer> nodesId = ReadFile.readInteger(baseFolder + "subName.txt");

            long searchStart = System.currentTimeMillis();
            int time = 0;
            while (resultSnippet.size() < SNIPPET_MAX_SIZE && time < MAX_COMPONENT_NUM) {
                time++;
//                System.out.println(time);
                double score = 0;
                int comp = 0;
                for (Map.Entry<Integer, Set<Integer>> entry: comp2edp.entrySet()) {
                    int current = entry.getKey();
                    int freqCount = 0;
                    entry.getValue().removeAll(coveredEDP);
                    for (int edp: entry.getValue()) {
                        freqCount += edp2freq.get(edp);
                    }
                    Set<Integer> compKeywords = comp2keywords.get(current);
                    if (!compKeywords.isEmpty()) {
                        compKeywords.removeAll(coveredKeywords);
                    }
                    double currentScore = 0;
                    if (keywordHitCount != 0) {
                        currentScore = ((double) freqCount)/entitySum + ((double) compKeywords.size())/keywordHitCount;
                    } else {
                        currentScore = ((double) freqCount)/entitySum;
                    }

                    if (score < currentScore) {
                        score = currentScore;
                        comp = current;
                    }
                }
                if (comp == 0) {
                    break;
                }

                CommonStruct c1 = new CommonStruct();
                c1.Init2(baseFolder);
                DOGST k = new DOGST(c1); // keyKG+
                List<String> keys = new ArrayList<>();
                for (int edp: comp2edp.get(comp)) {
                    keys.add(String.valueOf(edp));
                }
                for (int key: comp2keywords.get(comp)) {
                    keys.add("\"" + keywords.get(key) + "\"");
                }

//                System.out.println("Current comp: " + comp);
//                System.out.println(keys);
//                System.out.println("Key size: " + keys.size());
//                System.out.println(score);

                AnsTree resultTree = k.search(c1, keys, 2);
                Set<Integer> nodeSet = new HashSet<>();
                for (TreeEdge edge: resultTree.edge) {
                    nodeSet.add(edge.u);
                    nodeSet.add(edge.v);
                }

                for (int node: nodeSet) {
                    Query query = IntPoint.newExactQuery("node", nodesId.get(node));
                    ScoreDoc[] docs = tripleSearcher.search(query, 1).scoreDocs;
                    Document document = tripleSearcher.doc(docs[0].doc);
                    resultSnippet.addAll(Arrays.asList(document.get("triple").split(",")));
                }

                coveredEDP.addAll(comp2edp.get(comp));
                coveredKeywords.addAll(comp2keywords.get(comp));
            }

            String coverKeywordsResult = "";
            for (int iter: coveredKeywords) {
                coverKeywordsResult += keywords.get(iter) + " ";
            }
            System.out.println(coverKeywordsResult.trim());

//            System.out.println("Search time: " + (System.currentTimeMillis() - searchStart));

//            for (String triple: resultSnippet) {
//                System.out.println(triple);
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultSnippet;

    }

    private static void readTerms(int dataset, String[] resourceSet, Map<Integer, RDFTerm> id2term) {
        if (resourceSet.length == 1) {
            String getTerm = "SELECT iri,label,kind,id FROM rdf_term WHERE file_id = " + resourceSet[0] + " ORDER BY id";
            try {
                Connection connection = new DBUtil().conn;
                PreparedStatement getTermSt = connection.prepareStatement(getTerm, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                getTermSt.setFetchSize(Integer.MIN_VALUE);

                ResultSet resultSet = getTermSt.executeQuery();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String iri = resultSet.getString("iri");
                    int kind = resultSet.getInt("kind");
                    RDFTerm term = new RDFTerm(iri, resultSet.getString("label"), kind);
                    id2term.put(id, term);
                }
                connection.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            String labelIdIndex = PATHS.IndexPath + "LabelId/" + dataset + "/";
            try {
                IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(labelIdIndex)));
                for (int i = 0; i < reader.maxDoc(); i++) {
                    Document document = reader.document(i);
                    int id = Integer.parseInt(document.get("id"));
                    String label = document.get("label");
                    String iri = document.get("iri");
                    int kind = Integer.parseInt(document.get("kind"));
                    RDFTerm term = new RDFTerm(iri, label, kind);
                    id2term.put(id, term);
                }
                reader.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static void getSnippet(String queryString, int dataset) {
        Set<String> result = keywords2Triple(queryString, dataset);
        Map<Integer, RDFTerm> id2term = new HashMap<>();
        String[] resources = null;
        for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
            if (Integer.parseInt(iter.get(0)) == dataset) {
                resources = iter.get(1).split(" ");
                break;
            }
        }
        readTerms(dataset, resources, id2term);
        System.out.println("Term size: " + id2term.size());

        for (String triple: result) {
            int sid = Integer.parseInt(triple.split(" ")[0]);
            int pid = Integer.parseInt(triple.split(" ")[1]);
            int oid = Integer.parseInt(triple.split(" ")[2]);
            System.out.println(id2term.get(sid).getIri() + "\t" + id2term.get(pid).getIri() + "\t" + id2term.get(oid).getIri());
        }
    }

    private static void getDefaultSnippet(int dataset) {
        try {
            int entity = 0;
            for (List<Integer> iter: ReadFile.readInteger(PATHS.IndexPath + "Entity2EDP/" + dataset + ".txt", "\t")) {
                if (iter.get(1) == 1) {
                    entity = iter.get(0);
                    break;
                }
            }
            String nodeIndexFolder = PATHS.IndexPath + "NodeLabel/" + dataset + "/";
            IndexSearcher tripleSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(nodeIndexFolder))));
            Query query = IntPoint.newExactQuery("node", entity);
            ScoreDoc[] docs = tripleSearcher.search(query, 1).scoreDocs;
            Document document = tripleSearcher.doc(docs[0].doc);
            Set<String> result = new HashSet<>(Arrays.asList(document.get("triple").split(",")));

            Map<Integer, RDFTerm> id2term = new HashMap<>();
            String[] resources = null;
            for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
                if (Integer.parseInt(iter.get(0)) == dataset) {
                    resources = iter.get(1).split(" ");
                    break;
                }
            }
            readTerms(dataset, resources, id2term);

            for (String triple: result) {
                int sid = Integer.parseInt(triple.split(" ")[0]);
                int pid = Integer.parseInt(triple.split(" ")[1]);
                int oid = Integer.parseInt(triple.split(" ")[2]);
                System.out.println(id2term.get(sid).getIri() + "\t" + id2term.get(pid).getIri() + "\t" + id2term.get(oid).getIri());
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

}
