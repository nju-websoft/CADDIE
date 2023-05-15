package QPCSG;

import beans.RDFTerm;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Multigraph;
import util.DBUtil;
import util.PATHS;
import util.ReadFile;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class DatasetIndexer {

    /**
     * index patterns and keywords for QPCSG
     * select top-k patterns and/or keywords: k is a super-parameter
     *
     */
    private static void indexDatasetPattern(int start, int end) throws Exception {
        int datasetCount = 0;
        for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
            int dataset = Integer.parseInt(iter.get(0));
            if (dataset < start || dataset > end) {
                continue;
            }
            String[] resource = iter.get(1).split(" ");
            if (resource.length == 1) {
                continue;
            }
            datasetCount++;
            Map<Integer, RDFTerm> id2term = new HashMap<>(); // id start from 1
            Set<List<Integer>> triples = new HashSet<>();
            Set<Integer> classSet = new HashSet<>();
            Map<Integer, Set<Integer>> id2EDP = new HashMap<>();
//            System.out.println("Dataset: " + dataset);
//            System.out.println("Resources: " + resource.length);
            int typeId = readDataset(resource, id2term, triples, classSet, id2EDP);
//            System.out.println("Triples: " + triples.size());
            //========Finishing reading terms========================

            //build pattern map
            int total = id2EDP.size();
            if (total == 0) {
                System.out.println("Error: no entity in dataset " + dataset);
                return;
            }
            Map<Set<Integer>, Integer>pattern2Count = new HashMap<>(); /**pattern -> count*/
            for (Map.Entry<Integer, Set<Integer>> entry: id2EDP.entrySet()){
                Set<Integer> pattern = entry.getValue();
                int count = pattern2Count.getOrDefault(pattern, 0);
                pattern2Count.put(pattern, count + 1);
            }
            List<Map.Entry<Set<Integer>, Integer>> pattern2countList = new ArrayList<>(pattern2Count.entrySet());
            Collections.sort(pattern2countList, new Comparator<Map.Entry<Set<Integer>, Integer>>() {
                @Override
                public int compare(Map.Entry<Set<Integer>, Integer> o1, Map.Entry<Set<Integer>, Integer> o2) {
                    int v1 = o1.getValue();
                    int v2 = o2.getValue();
                    return (Integer.compare(v2, v1));
//                    return o2.getValue() - o1.getValue();
                }
            });

            Map<Set<Integer>, Integer> edp2id = new HashMap<>(); // used in entity2edp index;

            // store EDP index
//            String edpIndexFolder = PATHS.IndexPath + "EDPIndex/" + dataset + "/";
//            File file = new File(edpIndexFolder);
//            if (!file.exists()) {
//                file.mkdirs();
//            }
//            IndexWriter indexWriter = new IndexWriter(FSDirectory.open(Paths.get(edpIndexFolder)), new IndexWriterConfig(new StandardAnalyzer()));
//            int edpId = 0;
//            for (Map.Entry<Set<Integer>, Integer> entry: pattern2countList) {
//                edpId++;
//                Document document = new Document();
//                document.add(new IntPoint("id", edpId));
//                document.add(new StoredField("id", edpId));
//
//                int count = entry.getValue();
//                document.add(new IntPoint("count", count));
//                document.add(new StoredField("count", count));
//
//                Set<Integer> pattern = entry.getKey();
//                edp2id.put(pattern, edpId);
//                int edpSize = pattern.size();
//                document.add(new IntPoint("size", edpSize));
//                document.add(new StoredField("size", edpSize));
//                StringBuilder inProp = new StringBuilder();
//                StringBuilder outProp = new StringBuilder();
//                StringBuilder classes = new StringBuilder();
//                for (int id: pattern) {
//                    if (classSet.contains(id)) {
//                        classes.append(id).append(" ");
//                    }
//                    else if (id > 0) {
//                        outProp.append(id).append(" ");
//                    }
//                    else {
//                        inProp.append(-id).append(" ");
//                    }
//                }
//                document.add(new TextField("classes", classes.toString().trim(), Field.Store.YES));
//                document.add(new TextField("inProperty", inProp.toString().trim(), Field.Store.YES));
//                document.add(new TextField("outProperty", outProp.toString().trim(), Field.Store.YES));
//                indexWriter.addDocument(document);
//            }
//            indexWriter.close();
            PrintWriter writer = new PrintWriter(PATHS.IndexPath + "EDPIndex/" + dataset + ".txt");
            int edpId = 0;
            for (Map.Entry<Set<Integer>, Integer> entry: pattern2countList) {
                edpId++;
                int count = entry.getValue();
                Set<Integer> pattern = entry.getKey();
                edp2id.put(pattern, edpId);
                int edpSize = pattern.size();
                StringBuilder inProp = new StringBuilder();
                StringBuilder outProp = new StringBuilder();
                StringBuilder classes = new StringBuilder();
                for (int id: pattern) {
                    if (classSet.contains(id)) {
                        classes.append(id).append(" ");
                    }
                    else if (id > 0) {
                        outProp.append(id).append(" ");
                    }
                    else {
                        inProp.append(-id).append(" ");
                    }
                }
                writer.println(edpId + "\t" + count + "\t" + edpSize + "\t" + classes.toString().trim() + "\t" + inProp.toString().trim() + "\t" + outProp.toString().trim());
            }
            writer.close();

            // store entity index
//            String entityIndexFolder = PATHS.IndexPath + "Entity2EDP/" + dataset + "/";
//            file = new File(entityIndexFolder);
//            if (!file.exists()) {
//                file.mkdirs();
//            }
//            indexWriter = new IndexWriter(FSDirectory.open(Paths.get(entityIndexFolder)), new IndexWriterConfig(new StandardAnalyzer()));
//            for (Map.Entry<Integer, Set<Integer>> entry: id2EDP.entrySet()) {
//                int entity = entry.getKey();
//                Set<Integer> edp = entry.getValue();
//                Document document = new Document();
//                document.add(new TextField("entity", String.valueOf(entity), Field.Store.YES));
//                document.add(new TextField("edpId", String.valueOf(edp2id.get(edp)), Field.Store.YES));
//                int count = pattern2Count.get(edp);
//                document.add(new IntPoint("count", count));
//                document.add(new StoredField("count", count));
//                indexWriter.addDocument(document);
//            }
//            indexWriter.close();
            PrintWriter writer1 = new PrintWriter(PATHS.IndexPath + "Entity2EDP/" + dataset + ".txt");
            for (Map.Entry<Integer, Set<Integer>> entry: id2EDP.entrySet()) {
                int entity = entry.getKey();
                Set<Integer> edp = entry.getValue();
                int count = pattern2Count.get(edp);
                writer1.println(entity + "\t" + edp2id.get(edp) + "\t" + count);
            }
            writer1.close();
            System.out.println("Finish pattern index of " + "(" + datasetCount + ")" + " dataset: " + dataset);
        }
    }

    private static int readDataset(String[] resourceSet, Map<Integer, RDFTerm> id2term, Set<List<Integer>> triples, Set<Integer> classSet, Map<Integer, Set<Integer>> id2EDP) {
        int typeId = 0;

        String getTerm = "SELECT iri,label,kind,id FROM rdf_term WHERE file_id = ? ORDER BY id";
        String getTriple1 = "SELECT msg_code,subject,predicate,object FROM triple WHERE file_id = ?";
        String getTriple2 = "SELECT msg_code,subject,predicate,object FROM triple_socrata WHERE file_id = ?";
        Map<RDFTerm, Integer> term2generalId = new HashMap<>();
        int count = 0;
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement getTermSt = connection.prepareStatement(getTerm, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            getTermSt.setFetchSize(Integer.MIN_VALUE);

            PreparedStatement getTripleSt1 = connection.prepareStatement(getTriple1, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            getTripleSt1.setFetchSize(Integer.MIN_VALUE);
            PreparedStatement getTripleSt2 = connection.prepareStatement(getTriple2, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            getTripleSt2.setFetchSize(Integer.MIN_VALUE);

            Set<String> coveredMSG = new HashSet<>();
            for (String res: resourceSet) {
                int resource = Integer.parseInt(res);
                getTermSt.setInt(1, resource);

                Map<Integer, Integer> id2distinct = new HashMap<>();

                ResultSet resultSet = getTermSt.executeQuery();
                while (resultSet.next()) {
//                    record++;
                    String iri = resultSet.getString("iri");
                    int kind = resultSet.getInt("kind");
                    RDFTerm term = new RDFTerm(iri, resultSet.getString("label"), kind);
                    int localId = resultSet.getInt("id");
                    int generalId = term2generalId.getOrDefault(term, -1);
                    if (generalId == -1) {
                        count++;
                        generalId = count;
                        term2generalId.put(term, generalId);
                        id2term.put(generalId, term);
                        if (iri.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && kind == 0) {
                            typeId = generalId;
                        }
                    }
                    id2distinct.put(localId, generalId);
                }
                resultSet.close();

                ResultSet resultSet1 = null;
                if (resource > 200000 || resource == 18625) {
                    getTripleSt2.setInt(1, resource);
                    resultSet1 = getTripleSt2.executeQuery();
                } else {
                    getTripleSt1.setInt(1, resource);
                    resultSet1 = getTripleSt1.executeQuery();
                }

                Set<String> currentMSG = new HashSet<>();
                while (resultSet1.next()) {
                    String msgHash = resultSet1.getString("msg_code");
                    if (!coveredMSG.contains(msgHash)) {
                        int sid = id2distinct.get(resultSet1.getInt("subject"));
                        int pid = id2distinct.get(resultSet1.getInt("predicate"));
                        int oid = id2distinct.get(resultSet1.getInt("object"));
                        triples.add(new ArrayList<>(Arrays.asList(sid, pid, oid)));
//                    tripleCount++;
                        Set<Integer> edp = id2EDP.getOrDefault(sid, new HashSet<>());
                        if (typeId != 0 && pid == typeId) {
                            edp.add(oid);
                            classSet.add(oid);
                        } else {
                            edp.add(pid);
                        }
                        id2EDP.put(sid, edp);
                        if ((typeId == 0 || pid != typeId) && id2term.get(oid).getType() != 1) {
                            edp = id2EDP.getOrDefault(oid, new HashSet<>());
                            edp.add(-pid);
                            id2EDP.put(oid, edp);
                        }
                    }
                    currentMSG.add(msgHash);
                }
                resultSet1.close();
                if (currentMSG.isEmpty()) {
                    System.out.println("WRONG: " + resource);
                    return -1;
                }
                coveredMSG.addAll(currentMSG);
//                System.out.println(resource + ": " + id2distinct.size() + " ---- " + tripleCount);
            }
            connection.close();
//            System.out.println("Term records: " + record);
        }catch (Exception e) {
            e.printStackTrace();
        }

        return typeId;
    }

    /** DEPRECATED
     */
    private static void indexDatasetLabelId(int start, int end) throws Exception {
        int datasetCount = 0;
        StandardAnalyzer analyzer = new StandardAnalyzer(new FileReader(PATHS.FileBase + "nltk-stopwords-3.6.2.txt"));

        for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
            int dataset = Integer.parseInt(iter.get(0));
            datasetCount++;
            if (dataset < start || dataset > end) {
                continue;
            }
            String[] resource = iter.get(1).split(" ");
            Map<Integer, RDFTerm> id2term = new HashMap<>(); // id start from 1
            readTerms(resource, id2term);
            //========Finishing reading terms========================

            // store label with id
            String labelIdFolder = PATHS.IndexPath + "LabelId/" + dataset + "/";
            File file = new File(labelIdFolder);
            if (!file.exists()) {
                file.mkdirs();
            }
            IndexWriter indexWriter = new IndexWriter(FSDirectory.open(Paths.get(labelIdFolder)), new IndexWriterConfig(analyzer));
            for (Map.Entry<Integer, RDFTerm> entry: id2term.entrySet()) {
                int id = entry.getKey();
                String label = entry.getValue().getLabel();
                Document document = new Document();
                document.add(new IntPoint("id", id));
                document.add(new StoredField("id", id));
                document.add(new TextField("label", label, Field.Store.NO));
                indexWriter.addDocument(document);
            }
            indexWriter.close();
            System.out.println("Finish label id index of " + "(" + datasetCount + ")" + " dataset: " + dataset);
        }

        analyzer.close();
    }

    /**
     * Index term with id for datasets with 1+ resources
     * @param start
     * @param end
     * @throws Exception
     */
    private static void indexTermLabelId(int start, int end) throws Exception {
        int datasetCount = 0;

        for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
            int dataset = Integer.parseInt(iter.get(0));
            String[] resource = iter.get(1).split(" ");
            if (dataset < start || dataset > end || resource.length == 1) {
                continue;
            }
            datasetCount++;

            Map<Integer, RDFTerm> id2term = new HashMap<>(); // id start from 1
            readTerms(resource, id2term);
            //========Finishing reading terms========================

            // store label with id
            String labelIdFolder = PATHS.IndexPath + "LabelId/" + dataset + "/";
            File file = new File(labelIdFolder);
            if (!file.exists()) {
                file.mkdirs();
            }
            IndexWriter indexWriter = new IndexWriter(FSDirectory.open(Paths.get(labelIdFolder)), new IndexWriterConfig(new StandardAnalyzer()));
            for (Map.Entry<Integer, RDFTerm> entry: id2term.entrySet()) {
                int id = entry.getKey();
                RDFTerm term = entry.getValue();
                String iri = term.getIri();
                String label = term.getLabel();
                String kind = String.valueOf(term.getType());
                Document document = new Document();
                document.add(new IntPoint("id", id));
                document.add(new StoredField("id", id));
                document.add(new StoredField("iri", iri));
                document.add(new StoredField("label", label));
                document.add(new StoredField("kind", kind));
                indexWriter.addDocument(document);
            }
            indexWriter.close();
            System.out.println("Finish label id index of " + "(" + datasetCount + ")" + " dataset: " + dataset);
        }

    }

    private static void readTerms(String[] resourceSet, Map<Integer, RDFTerm> id2term) {
        String getTerm = "SELECT iri,label,kind,id FROM rdf_term WHERE file_id = ? ORDER BY id";
        Map<RDFTerm, Integer> term2generalId = new HashMap<>();
        int count = 0;
//        int record = 0;
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement getTermSt = connection.prepareStatement(getTerm, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            getTermSt.setFetchSize(Integer.MIN_VALUE);

            for (String res: resourceSet) {
                int resource = Integer.parseInt(res);
                getTermSt.setInt(1, resource);

                ResultSet resultSet = getTermSt.executeQuery();
                while (resultSet.next()) {
//                    record++;
                    String iri = resultSet.getString("iri");
                    int kind = resultSet.getInt("kind");
                    RDFTerm term = new RDFTerm(iri, resultSet.getString("label"), kind);
                    int generalId = term2generalId.getOrDefault(term, -1);
                    if (generalId == -1) {
                        count++;
                        generalId = count;
                        term2generalId.put(term, generalId);
                        id2term.put(generalId, term);
                    }
                }
                resultSet.close();
            }
            getTermSt.close();

            connection.close();
//            System.out.println("Term records: " + record);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void indexDatasetNodeLabel(int start, int end) throws Exception {
        StandardAnalyzer analyzer = new StandardAnalyzer(new FileReader(PATHS.FileBase + "nltk-stopwords-3.6.2.txt"));

        int datasetCount = 0;
        for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
            int dataset = Integer.parseInt(iter.get(0));
            if (dataset < start || dataset > end) {
                continue;
            }
            String[] resource = iter.get(1).split(" ");
            if (resource.length == 1) {
                continue;
            }
            datasetCount++;
            Map<Integer, RDFTerm> id2term = new HashMap<>(); // id start from 1
            Set<List<Integer>> triples = new HashSet<>();
            Set<Integer> classSet = new HashSet<>();
//            System.out.println("Dataset: " + dataset);
//            System.out.println("Resources: " + resource.length);
            int typeId = readDataset(resource, id2term, triples, classSet);
//            System.out.println("Triples: " + triples.size());
            //========Finishing reading terms========================

            Map<Integer, Set<List<Integer>>> entity2triple = new HashMap<>();
            for (List<Integer> t: triples) {
                int sid = t.get(0);
                int pid = t.get(1);
                int oid = t.get(2);
                if (typeId == pid || id2term.get(oid).getType() == 1) {
                    Set<List<Integer>> tripleSet = entity2triple.getOrDefault(sid, new HashSet<>());
                    tripleSet.add(t);
                    entity2triple.put(sid, tripleSet);
                } else {
                    Set<List<Integer>> tripleSet = entity2triple.getOrDefault(sid, new HashSet<>());
                    tripleSet.add(t);
                    entity2triple.put(sid, tripleSet);

                    Set<List<Integer>> tripleSetO = entity2triple.getOrDefault(oid, new HashSet<>());
                    tripleSetO.add(t);
                    entity2triple.put(oid, tripleSetO);
                }
            }

            // store entity index
            String nodeLabelFolder = PATHS.IndexPath + "NodeLabel/" + dataset + "/";
            File file = new File(nodeLabelFolder);
            if (!file.exists()) {
                file.mkdirs();
            }
            if (ReadFile.readString(PATHS.IndexPath + "Entity2EDP/" + dataset + ".txt").size() != entity2triple.size()) {
                System.out.println("Entity List WRONG: " + dataset);
                return;
            }
            IndexWriter indexWriter = new IndexWriter(FSDirectory.open(Paths.get(nodeLabelFolder)), new IndexWriterConfig(analyzer));
            for (Map.Entry<Integer, Set<List<Integer>>> entry: entity2triple.entrySet()) {
                int entity = entry.getKey();
                Set<List<Integer>> triple = entry.getValue();

                Document document = new Document();
                document.add(new IntPoint("node", entity));
                document.add(new StoredField("node", String.valueOf(entity)));

                Set<String> wordSet = new HashSet<>();
                StringBuilder tripleStr = new StringBuilder();

                for (List<Integer> t: triple) {
                    wordSet.addAll(Arrays.asList(id2term.get(t.get(0)).getLabel().split("\\s+")));
                    wordSet.addAll(Arrays.asList(id2term.get(t.get(1)).getLabel().split("\\s+")));
                    wordSet.addAll(Arrays.asList(id2term.get(t.get(2)).getLabel().split("\\s+")));
                    tripleStr.append(t.get(0)).append(" ").append(t.get(1)).append(" ").append(t.get(2)).append(",");
                }
                StringBuilder textStr = new StringBuilder();
                for (String word: wordSet) {
                    textStr.append(word).append(" ");
                }
                document.add(new TextField("text", textStr.toString().trim(), Field.Store.NO));
                document.add(new StoredField("triple", tripleStr.substring(0, tripleStr.length() - 1)));
                indexWriter.addDocument(document);
            }
            indexWriter.close();
            System.out.println("Finish node label index of " + "(" + datasetCount + ")" + " dataset: " + dataset);
        }

        analyzer.close();
    }

    public static int readDataset(String[] resourceSet, Map<Integer, RDFTerm> id2term, Set<List<Integer>> triples, Set<Integer> classSet) {
        int typeId = 0;

        String getTerm = "SELECT iri,label,kind,id FROM rdf_term WHERE file_id = ? ORDER BY id";
        String getTriple1 = "SELECT msg_code,subject,predicate,object FROM triple WHERE file_id = ?";
        String getTriple2 = "SELECT msg_code,subject,predicate,object FROM triple_socrata WHERE file_id = ?";
        Map<RDFTerm, Integer> term2generalId = new HashMap<>();
        int count = 0;
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement getTermSt = connection.prepareStatement(getTerm, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            getTermSt.setFetchSize(Integer.MIN_VALUE);

            PreparedStatement getTripleSt1 = connection.prepareStatement(getTriple1, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            getTripleSt1.setFetchSize(Integer.MIN_VALUE);
            PreparedStatement getTripleSt2 = connection.prepareStatement(getTriple2, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            getTripleSt2.setFetchSize(Integer.MIN_VALUE);

            Set<String> coveredMSG = new HashSet<>();
            for (String res: resourceSet) {
                int resource = Integer.parseInt(res);
                getTermSt.setInt(1, resource);

                Map<Integer, Integer> id2distinct = new HashMap<>();

                ResultSet resultSet = getTermSt.executeQuery();
                while (resultSet.next()) {
//                    record++;
                    String iri = resultSet.getString("iri");
                    int kind = resultSet.getInt("kind");
                    RDFTerm term = new RDFTerm(iri, resultSet.getString("label"), kind);
                    int localId = resultSet.getInt("id");
                    int generalId = term2generalId.getOrDefault(term, -1);
                    if (generalId == -1) {
                        count++;
                        generalId = count;
                        term2generalId.put(term, generalId);
                        id2term.put(generalId, term);
                        if (iri.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && kind == 0) {
                            typeId = generalId;
                        }
                    }
                    id2distinct.put(localId, generalId);
                }
                resultSet.close();

                ResultSet resultSet1 = null;
                if (resource > 200000 || resource == 18625) {
                    getTripleSt2.setInt(1, resource);
                    resultSet1 = getTripleSt2.executeQuery();
                } else {
                    getTripleSt1.setInt(1, resource);
                    resultSet1 = getTripleSt1.executeQuery();
                }

                Set<String> currentMSG = new HashSet<>();
                while (resultSet1.next()) {
                    String msgHash = resultSet1.getString("msg_code");
                    if (!coveredMSG.contains(msgHash)) {
                        int sid = id2distinct.get(resultSet1.getInt("subject"));
                        int pid = id2distinct.get(resultSet1.getInt("predicate"));
                        int oid = id2distinct.get(resultSet1.getInt("object"));
                        triples.add(new ArrayList<>(Arrays.asList(sid, pid, oid)));
//                    tripleCount++;
                        if (pid == typeId) {
                            classSet.add(oid);
                        }
                    }
                    currentMSG.add(msgHash);
                }
                resultSet1.close();
                if (currentMSG.isEmpty()) {
                    System.out.println("WRONG: " + resource);
                    return -1;
                }
                coveredMSG.addAll(currentMSG);
            }
            connection.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return typeId;
    }

    private static void indexComponents(int start, int end) {
        int dataset = 0;
        try {
            StandardAnalyzer analyzer = new StandardAnalyzer(new FileReader(PATHS.FileBase + "nltk-stopwords-3.6.2.txt"));
            Map<String, Analyzer> perFieldAnalyzer = new HashMap<>();
            perFieldAnalyzer.put("text", analyzer);
            PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), perFieldAnalyzer);

            int datasetCount = 0;
            for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
                dataset = Integer.parseInt(iter.get(0));
                if (dataset < start || dataset > end) {
                    continue;
                }
                String[] resource = iter.get(1).split(" ");
//                if (resource.length == 1) {
//                    continue;
//                }
                datasetCount++;
                Map<Integer, RDFTerm> id2term = new HashMap<>(); // id start from 1
                Set<List<Integer>> triples = new HashSet<>();
                Set<Integer> classSet = new HashSet<>();
                int typeId = readDataset(resource, id2term, triples, classSet);
                //========Finishing reading terms========================

                Map<Integer, Set<List<Integer>>> entity2triple = new HashMap<>();
                Multigraph<Integer, DefaultEdge> ERgraph = new Multigraph<>(DefaultEdge.class);
                for (List<Integer> t: triples) {
                    int sid = t.get(0);
                    int pid = t.get(1);
                    int oid = t.get(2);
                    if (typeId == pid || id2term.get(oid).getType() == 1) {
                        ERgraph.addVertex(sid);

                        Set<List<Integer>> tripleSet = entity2triple.getOrDefault(sid, new HashSet<>());
                        tripleSet.add(t);
                        entity2triple.put(sid, tripleSet);
                    } else {
                        ERgraph.addVertex(sid);
                        ERgraph.addVertex(oid);
                        if (sid != oid) {
                            ERgraph.addEdge(sid, oid);
                        }

                        Set<List<Integer>> tripleSet = entity2triple.getOrDefault(sid, new HashSet<>());
                        tripleSet.add(t);
                        entity2triple.put(sid, tripleSet);

                        Set<List<Integer>> tripleSetO = entity2triple.getOrDefault(oid, new HashSet<>());
                        tripleSetO.add(t);
                        entity2triple.put(oid, tripleSetO);
                    }
                }

                Map<Integer, Integer> entity2edp = new HashMap<>();
                Map<Integer, Integer> edp2count = new HashMap<>();
//            int EntitySum = ReadFile.readString(PATHS.IndexPath + "Entity2EDP/" + dataset + ".txt").size();
                for (List<Integer> i: ReadFile.readInteger(PATHS.IndexPath + "Entity2EDP/" + dataset + ".txt", "\t")) {
                    entity2edp.put(i.get(0), i.get(1));
                    edp2count.put(i.get(1), i.get(2));
                }

                ConnectivityInspector<Integer, DefaultEdge> inspector = new ConnectivityInspector<>(ERgraph);
                List<Set<Integer>> components = inspector.connectedSets();
                Map<Set<Integer>, Set<Integer>> component2edp = new HashMap<>();
                Map<Set<Integer>, Set<List<Integer>>> component2triple = new HashMap<>();
                Map<Set<Integer>, Integer> component2score = new HashMap<>(); // for sort
                for (Set<Integer> comp: components) {
                    Set<Integer> edpSet = new TreeSet<>(); //instantiated edps in the component
                    Set<List<Integer>> tripleSet = new HashSet<>(); // involved triples in the component
                    for (int node: comp) {
                        edpSet.add(entity2edp.get(node));
                        tripleSet.addAll(entity2triple.get(node));
                    }
                    component2edp.put(comp, edpSet);
                    component2triple.put(comp, tripleSet);
                    int edpFreq = 0;
                    for (int edp: edpSet) {
                        edpFreq += edp2count.get(edp);
                    }
                    component2score.put(comp, edpFreq);
                }

                List<Map.Entry<Set<Integer>, Integer>> componentList = new ArrayList<>(component2score.entrySet());
                Collections.sort(componentList, new Comparator<Map.Entry<Set<Integer>, Integer>>() {
                    @Override
                    public int compare(Map.Entry<Set<Integer>, Integer> o1, Map.Entry<Set<Integer>, Integer> o2) {
                        if (o1.getValue() > o2.getValue()) {
                            return -1;
                        }
                        if (o1.getValue() < o2.getValue()) {
                            return 1;
                        }
                        return 0;
                    }
                });
                String componentFolder = PATHS.IndexPath + "ComponentIndex/" + dataset + "/";
                File file = new File(componentFolder);
                if (!file.exists()) {
                    file.mkdirs();
                }
                IndexWriter indexWriter = new IndexWriter(FSDirectory.open(Paths.get(componentFolder)), new IndexWriterConfig(analyzerWrapper));
                int compCount = 0;
                for (Map.Entry<Set<Integer>, Integer> entry: componentList) {
                    Set<Integer> comp = entry.getKey();

//                    StringBuilder tripleStr = new StringBuilder();
                    Set<String> wordSet = new HashSet<>();
                    for (List<Integer> triple: component2triple.get(comp)) {
//                        tripleStr.append(triple.get(0)).append(" ").append(triple.get(1)).append(" ").append(triple.get(2)).append(",");
                        wordSet.addAll(Arrays.asList(id2term.get(triple.get(0)).getLabel().split("\\s+")));
                        wordSet.addAll(Arrays.asList(id2term.get(triple.get(1)).getLabel().split("\\s+")));
                        wordSet.addAll(Arrays.asList(id2term.get(triple.get(2)).getLabel().split("\\s+")));
                    }
                    StringBuilder textStr = new StringBuilder();
                    for (String word: wordSet) {
                        textStr.append(word).append(" ");
                    }

                    StringBuilder edpStr = new StringBuilder();
                    for (int edp: component2edp.get(comp)) {
                        edpStr.append(edp).append(" ");
                    }

                    Document document = new Document();
                    compCount++;
                    document.add(new IntPoint("id", compCount));
                    document.add(new StoredField("id", compCount));
                    document.add(new TextField("text", textStr.toString().trim(), Field.Store.NO));
//                    document.add(new StoredField("triple", tripleStr.substring(0, tripleStr.length() - 1)));
                    document.add(new StoredField("edp", edpStr.toString().trim()));
                    indexWriter.addDocument(document);
                }
                indexWriter.close();
                System.out.println("Finish component index of " + "(" + datasetCount + ")" + " dataset: " + dataset);
            }
            analyzer.close();
        } catch (Exception e) {
            System.out.println("ERROR in dataset: " + dataset);
            e.printStackTrace();
        }

    }

    private static void indexComponentsLARGE() {
        int dataset = 0;
        try {
            StandardAnalyzer analyzer = new StandardAnalyzer(new FileReader(PATHS.FileBase + "nltk-stopwords-3.6.2.txt"));
            Map<String, Analyzer> perFieldAnalyzer = new HashMap<>();
            perFieldAnalyzer.put("text", analyzer);
            PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), perFieldAnalyzer);

            int datasetCount = 0;
            for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
                dataset = Integer.parseInt(iter.get(0));
                datasetCount++;
                if (dataset != 15243) {
                    continue;
                }
                String[] resource = iter.get(1).split(" ");
                Map<Integer, RDFTerm> id2term = new HashMap<>(); // id start from 1
                Set<List<Integer>> triples = new HashSet<>();
                Set<Integer> classSet = new HashSet<>();
                int typeId = readDataset(resource, id2term, triples, classSet);
                //========Finishing reading terms========================

                Map<Integer, Set<List<Integer>>> entity2triple = new HashMap<>();
                Multigraph<Integer, DefaultEdge> ERgraph = new Multigraph<>(DefaultEdge.class);
                for (List<Integer> t: triples) {
                    int sid = t.get(0);
                    int pid = t.get(1);
                    int oid = t.get(2);
                    if (typeId == pid || id2term.get(oid).getType() == 1) {
                        ERgraph.addVertex(sid);

                        Set<List<Integer>> tripleSet = entity2triple.getOrDefault(sid, new HashSet<>());
                        tripleSet.add(t);
                        entity2triple.put(sid, tripleSet);
                    } else {
                        ERgraph.addVertex(sid);
                        ERgraph.addVertex(oid);
                        if (sid != oid) {
                            ERgraph.addEdge(sid, oid);
                        }

                        Set<List<Integer>> tripleSet = entity2triple.getOrDefault(sid, new HashSet<>());
                        tripleSet.add(t);
                        entity2triple.put(sid, tripleSet);

                        Set<List<Integer>> tripleSetO = entity2triple.getOrDefault(oid, new HashSet<>());
                        tripleSetO.add(t);
                        entity2triple.put(oid, tripleSetO);
                    }
                }

                Map<Integer, Integer> entity2edp = new HashMap<>();
                Map<Integer, Integer> edp2count = new HashMap<>();
//            int EntitySum = ReadFile.readString(PATHS.IndexPath + "Entity2EDP/" + dataset + ".txt").size();
                for (List<Integer> i: ReadFile.readInteger(PATHS.IndexPath + "Entity2EDP/" + dataset + ".txt", "\t")) {
                    entity2edp.put(i.get(0), i.get(1));
                    edp2count.put(i.get(1), i.get(2));
                }

                ConnectivityInspector<Integer, DefaultEdge> inspector = new ConnectivityInspector<>(ERgraph);
                List<Set<Integer>> components = inspector.connectedSets();
                Map<Set<Integer>, Set<Integer>> component2edp = new HashMap<>();
                Map<Set<Integer>, Set<List<Integer>>> component2triple = new HashMap<>();
                Map<Set<Integer>, Integer> component2score = new HashMap<>(); // for sort
                for (Set<Integer> comp: components) {
                    Set<Integer> edpSet = new TreeSet<>(); //instantiated edps in the component
                    Set<List<Integer>> tripleSet = new HashSet<>(); // involved triples in the component
                    for (int node: comp) {
                        edpSet.add(entity2edp.get(node));
                        tripleSet.addAll(entity2triple.get(node));
                    }
                    component2edp.put(comp, edpSet);
                    component2triple.put(comp, tripleSet);
                    int edpFreq = 0;
                    for (int edp: edpSet) {
                        edpFreq += edp2count.get(edp);
                    }
                    component2score.put(comp, edpFreq);
                }

                List<Map.Entry<Set<Integer>, Integer>> componentList = new ArrayList<>(component2score.entrySet());
                Collections.sort(componentList, new Comparator<Map.Entry<Set<Integer>, Integer>>() {
                    @Override
                    public int compare(Map.Entry<Set<Integer>, Integer> o1, Map.Entry<Set<Integer>, Integer> o2) {
                        if (o1.getValue() > o2.getValue()) {
                            return -1;
                        }
                        if (o1.getValue() < o2.getValue()) {
                            return 1;
                        }
                        return 0;
                    }
                });
                String componentFolder = PATHS.IndexPath + "ComponentIndex/" + dataset + "/";
                File file = new File(componentFolder);
                if (!file.exists()) {
                    file.mkdirs();
                }
                IndexWriter indexWriter = new IndexWriter(FSDirectory.open(Paths.get(componentFolder)), new IndexWriterConfig(analyzerWrapper));
                int compCount = 0;
                for (Map.Entry<Set<Integer>, Integer> entry: componentList) {
                    Set<Integer> comp = entry.getKey();

                    StringBuilder tripleStr = new StringBuilder();
                    Set<String> wordSet = new HashSet<>();
                    for (List<Integer> triple: component2triple.get(comp)) {
                        tripleStr.append(triple.get(0)).append(" ").append(triple.get(1)).append(" ").append(triple.get(2)).append(",");
                        wordSet.addAll(Arrays.asList(id2term.get(triple.get(0)).getLabel().split("\\s+")));
                        wordSet.addAll(Arrays.asList(id2term.get(triple.get(1)).getLabel().split("\\s+")));
                        wordSet.addAll(Arrays.asList(id2term.get(triple.get(2)).getLabel().split("\\s+")));
                    }
                    StringBuilder textStr = new StringBuilder();
                    for (String word: wordSet) {
                        textStr.append(word).append(" ");
                    }

                    StringBuilder edpStr = new StringBuilder();
                    for (int edp: component2edp.get(comp)) {
                        edpStr.append(edp).append(" ");
                    }

                    Document document = new Document();
                    compCount++;
                    document.add(new IntPoint("id", compCount));
                    document.add(new StoredField("id", compCount));
                    document.add(new TextField("text", textStr.toString().trim(), Field.Store.NO));
//                    document.add(new StoredField("triple", tripleStr.substring(0, tripleStr.length() - 1)));
                    document.add(new StoredField("edp", edpStr.toString().trim()));
                    indexWriter.addDocument(document);
                }
                indexWriter.close();
                System.out.println("Finish component index of " + "(" + datasetCount + ")" + " dataset: " + dataset);
            }
            analyzer.close();
        } catch (Exception e) {
            System.out.println("ERROR in dataset: " + dataset);
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        try {
//            indexDatasetPattern(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
//            indexDatasetNodeLabel(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
//            indexTermLabelId(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            indexComponents(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
//            indexComponentsLARGE();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
