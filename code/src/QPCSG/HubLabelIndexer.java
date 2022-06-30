package QPCSG;

import HubLabel.PLL.WeightedPLL;
import beans.RDFTerm;
import HubLabel.graph.WeightedGraph;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import util.DBUtil;
import util.PATHS;
import util.ReadFile;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class HubLabelIndexer extends Thread {

    public static int readDataset(String[] resourceSet, Map<Integer, RDFTerm> id2term, Set<List<Integer>> triples, Set<Integer> classSet) {
        int typeId = 0;

        String getTerm = "SELECT iri,label,kind,id FROM rdf_term WHERE file_id = ? ORDER BY id";
        String getTriple1 = "SELECT msg_code,subject,predicate,object FROM triple WHERE file_id = ?";
        String getTriple2 = "SELECT msg_code,subject,predicate,object FROM triple_socrata WHERE file_id = ?";
        Map<RDFTerm, Integer> term2generalId = new HashMap<>();
        int count = 0;
        try {
            StandardAnalyzer analyzer = new StandardAnalyzer(new FileReader(PATHS.FileBase + "nltk-stopwords-3.6.2.txt"));

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

                    StringBuilder label = new StringBuilder();
                    String orgLabel = resultSet.getString("label");
                    if (orgLabel != null && !orgLabel.equals("")) {
                        TokenStream tokenStream = analyzer.tokenStream("content", orgLabel);
                        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
                        tokenStream.reset();
                        while (tokenStream.incrementToken()) {
                            label.append(attr.toString()).append(" ");
                        }
                        tokenStream.close();
                    }

                    RDFTerm term = new RDFTerm(iri, label.toString().trim(), kind);
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
            analyzer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return typeId;
    }

    private static void prepareFiles(int start, int end) {
        int dataset = 0;
        try {
            int datasetCount = 0;
            for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
                dataset = Integer.parseInt(iter.get(0));
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
                int typeId = readDataset(resource, id2term, triples, classSet);
                //========Finishing reading terms========================

                Map<Integer, Integer> entity2edp = new HashMap<>();
                Map<Integer, Set<Integer>> edp2entity = new HashMap<>();
                for (List<Integer> i: ReadFile.readInteger(PATHS.IndexPath + "Entity2EDP/" + dataset + ".txt", "\t")) {
                    int entity = i.get(0);
                    int edp = i.get(1);
                    entity2edp.put(entity, edp);
                    Set<Integer> entities = edp2entity.getOrDefault(edp, new HashSet<>());
                    entities.add(entity);
                    edp2entity.put(edp, entities);
                }

                String baseFolder = PATHS.ProjectBase + "HubLabel/" + dataset + "/";
                File baseFile = new File(baseFolder);
                if (!baseFile.exists()) {
                    baseFile.mkdirs();
                }

                // HubLabel.graph
                String graphFile = baseFolder + "/HubLabel.graph.txt";
                File file = new File(graphFile);
                if (!file.exists()) {
                    file.createNewFile();
                }
                PrintWriter graphWriter = new PrintWriter(graphFile);
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
                        if (sid != oid) {
                            graphWriter.println(sid + " " + pid + " " + oid); // note here !
                        }

                        Set<List<Integer>> tripleSet = entity2triple.getOrDefault(sid, new HashSet<>());
                        tripleSet.add(t);
                        entity2triple.put(sid, tripleSet);

                        Set<List<Integer>> tripleSetO = entity2triple.getOrDefault(oid, new HashSet<>());
                        tripleSetO.add(t);
                        entity2triple.put(oid, tripleSetO);
                    }
                }
                graphWriter.close();

                if (entity2edp.size() != entity2triple.size()) {
                    System.out.println("Entity maps have different size, wrong!");
                    return;
                }

                //subName
                Map<Integer, Integer> node2Id = new HashMap<>();
                String subNameFile = baseFolder + "/subName.txt";
                file = new File(subNameFile);
                if (!file.exists()) {
                    file.createNewFile();
                }
                PrintWriter subNameWriter = new PrintWriter(subNameFile);
                int count = 0;
                for (int entity: entity2edp.keySet()) {
                    subNameWriter.println(entity);
                    node2Id.put(entity, count);
                    count++;
                }
                subNameWriter.close();

                // keyMap and invertedTable
                Map<String, Set<Integer>> word2node = new HashMap<>();
                for (Map.Entry<Integer, Set<List<Integer>>> entry: entity2triple.entrySet()) {
                    int entity = entry.getKey();
                    Set<String> wordSet = new HashSet<>();
                    for (List<Integer> t: entry.getValue()) {
                        wordSet.addAll(Arrays.asList(id2term.get(t.get(0)).getLabel().split("\\s+")));
                        wordSet.addAll(Arrays.asList(id2term.get(t.get(1)).getLabel().split("\\s+")));
                        wordSet.addAll(Arrays.asList(id2term.get(t.get(2)).getLabel().split("\\s+")));
                    }
                    for (String word: wordSet) {
                        Set<Integer> nodes = word2node.getOrDefault(word, new HashSet<>());
                        nodes.add(entity);
                        word2node.put(word, nodes);
                    }
                    word2node.remove("");
                }
                String keyMapFile = baseFolder + "/keyMap.txt";
                file = new File(keyMapFile);
                if (!file.exists()) {
                    file.createNewFile();
                }
                PrintWriter keyMapWriter = new PrintWriter(keyMapFile);

                String invertedTableFile = baseFolder + "/invertedTable.txt";
                file = new File(invertedTableFile);
                if (!file.exists()) {
                    file.createNewFile();
                }
                PrintWriter invTableWriter = new PrintWriter(invertedTableFile);

                for (Map.Entry<Integer, Set<Integer>> entry: edp2entity.entrySet()) {
                    int edp = entry.getKey();
                    keyMapWriter.println(edp);
                    StringBuilder values = new StringBuilder();
                    for (int entity: entry.getValue()) {
                        values.append(node2Id.get(entity)).append(" ");
                    }
                    invTableWriter.println(edp + "::=" + values.toString().trim());
                }
                for (Map.Entry<String, Set<Integer>> entry: word2node.entrySet()) {
                    String word = entry.getKey();
                    keyMapWriter.println("\"" + word + "\"");
                    StringBuilder values = new StringBuilder();
                    for (int entity: entry.getValue()) {
                        values.append(node2Id.get(entity)).append(" ");
                    }
                    invTableWriter.println("\"" + word + "\"" + "::=" + values.toString().trim());
                }
                keyMapWriter.close();
                invTableWriter.close();

                System.out.println("Finish HubLabel files of " + "(" + datasetCount + ")" + " dataset: " + dataset);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Current dataset: " + dataset);
        }

    }

    private static void getHubLabels(int start, int end)  {
        try {
            PrintWriter writer = new PrintWriter(PATHS.ProjectBase + "Hub-label-time-2.txt");
            for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
                int dataset = Integer.parseInt(iter.get(0));
                if (dataset < start || dataset > end) {
                    continue;
                }
                String[] resource = iter.get(1).split(" ");
                if (resource.length == 1) {
                    continue;
                }
                String baseFolder = PATHS.ProjectBase + "HubLabel/" + dataset + "/";
                long startTime = System.currentTimeMillis();
                WeightedGraph ww = new WeightedGraph();
                ww.graphIndexRead3(baseFolder);
                WeightedPLL w2 = new WeightedPLL();
                w2.pllIndexDeal2(ww, baseFolder);

                writer.println(dataset + "\t" + (System.currentTimeMillis() - startTime));
                writer.flush();
            }
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        prepareFiles(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        getHubLabels(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }
}
