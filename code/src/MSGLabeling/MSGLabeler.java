package MSGLabeling;

import com.google.common.hash.HashCode;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import util.DBUtil;
import util.PATHS;
import util.ReadFile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import static cl.uchile.dcc.blabel.modification.LeanLabeler.leanAndLabel;

public class MSGLabeler {
    /**
     * Leaning and label each MSG using the LeanLabeler, and update the labels into HubLabel.database.
     *
     * @author Xiaxia Wang
     */

    /**
     *
     * @param fileName: the file recording all local available datasets
     * @param start: the first order of dataset in the list to be processed
     * @param end: the last
     * @param table: 1: CKAN, 2: SOCRATA
     */
    private static void labelAllDatasets(String fileName, int start, int end, int table) {
        List<Integer> datasets = ReadFile.readInteger(fileName, " ").get(0);
        try {
            if (table == 1) {
                for (int i = start; i <= end; i++) {
                    System.out.print(i + "\t");
                    labelCKANResource(datasets.get(i));
                }
            }
            else if (table == 2) {
                for (int i = start; i <= end; i++) {
                    System.out.print(i + "\t");
                    labelSocrataResource(datasets.get(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Find each MSG and immediately get and update the hash.
     * @param resource
     */
    public static void labelCKANResource(int resource) {
        long startTime = System.currentTimeMillis();

        Map<Integer, String> textMap = new HashMap<>();
        Map<Integer, Integer> kindMap = new HashMap<>();
        String getTriple = "SELECT subject,predicate,object FROM triple WHERE file_id = " + resource;
        String getTerm = "SELECT id,kind,iri FROM rdf_term WHERE file_id = " + resource;

        String update = "UPDATE triple SET msg_code = ? WHERE file_id = ? AND `subject` = ? AND predicate = ? AND object = ?;";

        Set<List<Integer>> allTriple = new HashSet<>(); // retain all triples
        Map<Integer, Set<List<Integer>>> bnode2triple = new HashMap<>(); // each blank node -> set of triples containing it
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement getTermSt = connection.prepareStatement(getTerm);

            PreparedStatement updatePst = connection.prepareStatement(update);
            updatePst.setInt(2, resource);
            int submitBatchIndicator = 0;

            ResultSet resultSet1 = getTermSt.executeQuery();
            while (resultSet1.next()) {
                int id = resultSet1.getInt("id");
                int termType = resultSet1.getInt("kind");
                String text = resultSet1.getString("iri");

                kindMap.put(id, termType);
                textMap.put(id, text);

                if (termType == 1) {
                    bnode2triple.put(id, new HashSet<>()); // as a blank node
                }
            }
            PreparedStatement getTripleSt = connection.prepareStatement(getTriple);
            ResultSet resultSet = getTripleSt.executeQuery();
            while (resultSet.next()) {
                int subject = resultSet.getInt("subject");
                int predicate = resultSet.getInt("predicate");
                int object = resultSet.getInt("object");
                List<Integer> triple = new ArrayList<>(Arrays.asList(subject, predicate, object));
                allTriple.add(triple);
                if (kindMap.get(subject) == 1) {
                    bnode2triple.get(subject).add(triple);
                }
                if (kindMap.get(object) == 1) {
                    bnode2triple.get(object).add(triple);
                }
            }

            int msgCount = 0;
            while (!allTriple.isEmpty()) {
                List<Integer> init = allTriple.iterator().next(); // get an initial triple
                Set<List<Integer>> msg = new HashSet<>();
                msg.add(init);
                Set<Integer> bnode = new HashSet<>(); // record bnodes in this MSG
                if (kindMap.get(init.get(0)) == 1) {
                    bnode.add(init.get(0));
                }
                if (kindMap.get(init.get(2)) == 1) {
                    bnode.add(init.get(2));
                }
                Set<Integer> containedBnode = new HashSet<>();
                while (!bnode.isEmpty()) {
                    int node = bnode.iterator().next();
                    containedBnode.add(node);
                    Set<List<Integer>> bTriples = bnode2triple.get(node);
                    for (List<Integer> iter: bTriples) {
                        int sub = iter.get(0);
                        int obj = iter.get(2);
                        if (!containedBnode.contains(sub) && kindMap.get(sub) == 1) {
                            bnode.add(sub);
                        }
                        if (!containedBnode.contains(obj) && kindMap.get(obj) == 1) {
                            bnode.add(obj);
                        }
                    }
                    msg.addAll(bTriples);
                    bnode.remove(node);
                }
                allTriple.removeAll(msg);

                // to get hash
                Model model = ModelFactory.createDefaultModel();
                for (List<Integer> iter: msg) {
                    Resource subj = null;
                    switch (kindMap.get(iter.get(0))) {
                        case 0:
                            subj = model.createResource(textMap.get(iter.get(0)));
                            break;
                        case 1:
                            subj = model.createResource(AnonId.create(textMap.get(iter.get(0))));
                            break;
                    }

                    Property pred = model.createProperty(textMap.get(iter.get(1)));

                    switch (kindMap.get(iter.get(2))) {
                        case 0:
                            Resource obj = model.createResource(textMap.get(iter.get(2)));
                            model.add(subj, pred, obj);
                            break;
                        case 1:
                            Resource bobj = model.createResource(AnonId.create(textMap.get(iter.get(2))));
                            model.add(subj, pred, bobj);
                            break;
                        case 2:
                            Literal lobj = model.createLiteral(textMap.get(iter.get(2)));
                            model.add(subj, pred, lobj);
                            break;
                    }
                }
                StringWriter tripleWriter = new StringWriter();
                RDFDataMgr.write(tripleWriter, model, Lang.NTRIPLES);
//                    Set<String> triples = new HashSet<>(Arrays.asList(tripleWriter.toString().split("\n")));
                // for special resource: 17215(9166)
                Set<String> triples = new HashSet<>();
                String[] ts = tripleWriter.toString().split("\\.\n");
                for (String s: ts) {
                    triples.add(s + ".");
                }

                // Get the HubLabel.graph hash
                HashCode hash = leanAndLabel(triples);
                if (hash == null) {
                    System.out.println("WRONG HASH!");
                    for (String iter: triples) {
                        System.out.println(iter);
                    }
                    return;
                }

                // Update the HubLabel.database
                updatePst.setString(1, String.valueOf(hash));
                for (List<Integer> iter: msg) {
                    updatePst.setInt(3, iter.get(0));
                    updatePst.setInt(4, iter.get(1));
                    updatePst.setInt(5, iter.get(2));
                    updatePst.addBatch();
                    submitBatchIndicator++;
                    if (submitBatchIndicator == DBUtil.BATCH_SIZE) {
                        updatePst.executeBatch();
                        updatePst.clearBatch();
                        submitBatchIndicator = 0;
                    }
                }
                msgCount++;
//                if (msgCount % 10000 == 0) {
//                    System.out.println("Finish " + msgCount + " msgs. ");
//                }
            }
            updatePst.executeBatch();
            updatePst.close();
            connection.close();

            long time = System.currentTimeMillis() - startTime;
            System.out.println(resource + "\t" + msgCount + "\t" + time);

        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Find each MSG and immediately get and update the hash. Only difference to the above is the table
     * @param resource
     */
    public static void labelSocrataResource(int resource) {
        long startTime = System.currentTimeMillis();

        Map<Integer, String> textMap = new HashMap<>();
        Map<Integer, Integer> kindMap = new HashMap<>();
        String getTriple = "SELECT subject,predicate,object FROM triple_socrata WHERE file_id = " + resource;
        String getTerm = "SELECT id,kind,iri FROM rdf_term WHERE file_id = " + resource;

        String update = "UPDATE triple_socrata SET msg_code = ? WHERE file_id = ? AND `subject` = ? AND predicate = ? AND object = ?;";

        Set<List<Integer>> allTriple = new HashSet<>(); // retain all triples
        Map<Integer, Set<List<Integer>>> bnode2triple = new HashMap<>(); // each blank node -> set of triples containing it
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement getTermSt = connection.prepareStatement(getTerm);

            PreparedStatement updatePst = connection.prepareStatement(update);
            updatePst.setInt(2, resource);
            int submitBatchIndicator = 0;

            ResultSet resultSet1 = getTermSt.executeQuery();
            while (resultSet1.next()) {
                int id = resultSet1.getInt("id");
                int termType = resultSet1.getInt("kind");
                String text = resultSet1.getString("iri");

                kindMap.put(id, termType);
                textMap.put(id, text);

                if (termType == 1) {
                    bnode2triple.put(id, new HashSet<>()); // as a blank node
                }
            }
            PreparedStatement getTripleSt = connection.prepareStatement(getTriple);
            ResultSet resultSet = getTripleSt.executeQuery();
            while (resultSet.next()) {
                int subject = resultSet.getInt("subject");
                int predicate = resultSet.getInt("predicate");
                int object = resultSet.getInt("object");
                List<Integer> triple = new ArrayList<>(Arrays.asList(subject, predicate, object));
                allTriple.add(triple);
                if (kindMap.get(subject) == 1) {
                    bnode2triple.get(subject).add(triple);
                }
                if (kindMap.get(object) == 1) {
                    bnode2triple.get(object).add(triple);
                }
            }

            int msgCount = 0;
            while (!allTriple.isEmpty()) {
                List<Integer> init = allTriple.iterator().next(); // get an initial triple
                Set<List<Integer>> msg = new HashSet<>();
                msg.add(init);
                Set<Integer> bnode = new HashSet<>(); // record bnodes in this MSG
                if (kindMap.get(init.get(0)) == 1) {
                    bnode.add(init.get(0));
                }
                if (kindMap.get(init.get(2)) == 1) {
                    bnode.add(init.get(2));
                }
                Set<Integer> containedBnode = new HashSet<>();
                while (!bnode.isEmpty()) {
                    int node = bnode.iterator().next();
                    containedBnode.add(node);
                    Set<List<Integer>> bTriples = bnode2triple.get(node);
                    for (List<Integer> iter: bTriples) {
                        int sub = iter.get(0);
                        int obj = iter.get(2);
                        if (!containedBnode.contains(sub) && kindMap.get(sub) == 1) {
                            bnode.add(sub);
                        }
                        if (!containedBnode.contains(obj) && kindMap.get(obj) == 1) {
                            bnode.add(obj);
                        }
                    }
                    msg.addAll(bTriples);
                    bnode.remove(node);
                }
                allTriple.removeAll(msg);

                // to get hash
                Model model = ModelFactory.createDefaultModel();
                for (List<Integer> iter: msg) {
                    Resource subj = null;
                    switch (kindMap.get(iter.get(0))) {
                        case 0:
                            subj = model.createResource(textMap.get(iter.get(0)));
                            break;
                        case 1:
                            subj = model.createResource(AnonId.create(textMap.get(iter.get(0))));
                            break;
                    }

                    Property pred = model.createProperty(textMap.get(iter.get(1)));

                    switch (kindMap.get(iter.get(2))) {
                        case 0:
                            Resource obj = model.createResource(textMap.get(iter.get(2)));
                            model.add(subj, pred, obj);
                            break;
                        case 1:
                            Resource bobj = model.createResource(AnonId.create(textMap.get(iter.get(2))));
                            model.add(subj, pred, bobj);
                            break;
                        case 2:
                            Literal lobj = model.createLiteral(textMap.get(iter.get(2)));
                            model.add(subj, pred, lobj);
                            break;
                    }
                }
                StringWriter tripleWriter = new StringWriter();
                RDFDataMgr.write(tripleWriter, model, Lang.NTRIPLES);
//                    Set<String> triples = new HashSet<>(Arrays.asList(tripleWriter.toString().split("\n")));
                // for special resource: 17215(9166)
                Set<String> triples = new HashSet<>();
                String[] ts = tripleWriter.toString().split("\\.\n");
                for (String s: ts) {
                    triples.add(s + ".");
                }

                // Get the HubLabel.graph hash
                HashCode hash = leanAndLabel(triples);
                if (hash == null) {
                    System.out.println("WRONG HASH!");
                    for (String iter: triples) {
                        System.out.println(iter);
                    }
                    return;
                }

                // Update the HubLabel.database
                updatePst.setString(1, String.valueOf(hash));
                for (List<Integer> iter: msg) {
                    updatePst.setInt(3, iter.get(0));
                    updatePst.setInt(4, iter.get(1));
                    updatePst.setInt(5, iter.get(2));
                    updatePst.addBatch();
                    submitBatchIndicator++;
                    if (submitBatchIndicator == DBUtil.BATCH_SIZE) {
                        updatePst.executeBatch();
                        updatePst.clearBatch();
                        submitBatchIndicator = 0;
                    }
                }
                msgCount++;
//                if (msgCount % 10000 == 0) {
//                    System.out.println("Finish " + msgCount + " msgs. ");
//                }
            }
            updatePst.executeBatch();
            updatePst.close();
            connection.close();

            long time = System.currentTimeMillis() - startTime;
            System.out.println(resource + "\t" + msgCount + "\t" + time);

        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * For especially large resource.
     * Find each MSG and immediately get and update the hash.
     * @param resource
     * @return 1: continue, -1: finish
     */
    public static int labelLARGECKANResource(int resource, int batch) {
        StringBuilder batchRecorder = new StringBuilder();

        Map<Integer, String> textMap = new HashMap<>();
        Map<Integer, Integer> kindMap = new HashMap<>();
        String getTriple = "SELECT subject,predicate,object FROM triple_socrata WHERE file_id = " + resource + " AND msg_code IS NULL";
        String getTerm = "SELECT id,kind,iri FROM rdf_term WHERE file_id = " + resource;

        String update = "UPDATE triple_socrata SET msg_code = ? WHERE file_id = ? AND `subject` = ? AND predicate = ? AND object = ?;";

        Set<List<Integer>> allTriple = new HashSet<>(); // retain all triples
        Map<Integer, Set<List<Integer>>> bnode2triple = new HashMap<>(); // each blank node -> set of triples containing it
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement getTermSt = connection.prepareStatement(getTerm, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            getTermSt.setFetchSize(Integer.MIN_VALUE);

            PreparedStatement updatePst = connection.prepareStatement(update);
            updatePst.setInt(2, resource);
//            int submitBatchIndicator = 0;

            ResultSet resultSet1 = getTermSt.executeQuery();
            while (resultSet1.next()) {
                int id = resultSet1.getInt("id");
                int termType = resultSet1.getInt("kind");
                String text = resultSet1.getString("iri");

                kindMap.put(id, termType);
                textMap.put(id, text);

//                if (termType == 1) {
//                    bnode2triple.put(id, new HashSet<>()); // as a blank node
//                }
            }
            PreparedStatement getTripleSt = connection.prepareStatement(getTriple, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            getTripleSt.setFetchSize(Integer.MIN_VALUE);
            ResultSet resultSet = getTripleSt.executeQuery();
            while (resultSet.next()) {
                int subject = resultSet.getInt("subject");
                int predicate = resultSet.getInt("predicate");
                int object = resultSet.getInt("object");
                List<Integer> triple = new ArrayList<>(Arrays.asList(subject, predicate, object));
                allTriple.add(triple);
                if (kindMap.get(subject) == 1) {
                    Set<List<Integer>> stSet = bnode2triple.getOrDefault(subject, new HashSet<>());
                    stSet.add(triple);
                    bnode2triple.put(subject, stSet);
                }
                if (kindMap.get(object) == 1) {
                    Set<List<Integer>> otSet = bnode2triple.getOrDefault(object, new HashSet<>());
                    otSet.add(triple);
                    bnode2triple.put(object, otSet);
                }
            }

            int msgCount = 0;
            while (!allTriple.isEmpty()) {
                List<Integer> init = allTriple.iterator().next(); // get an initial triple
                Set<List<Integer>> msg = new HashSet<>();
                msg.add(init);
                Set<Integer> bnode = new HashSet<>(); // record bnodes in this MSG
                if (kindMap.get(init.get(0)) == 1) {
                    bnode.add(init.get(0));
                }
                if (kindMap.get(init.get(2)) == 1) {
                    bnode.add(init.get(2));
                }
                Set<Integer> containedBnode = new HashSet<>();
                while (!bnode.isEmpty()) {
                    int node = bnode.iterator().next();
                    containedBnode.add(node);
                    Set<List<Integer>> bTriples = bnode2triple.get(node);
                    for (List<Integer> iter: bTriples) {
                        int sub = iter.get(0);
                        int obj = iter.get(2);
                        if (!containedBnode.contains(sub) && kindMap.get(sub) == 1) {
                            bnode.add(sub);
                        }
                        if (!containedBnode.contains(obj) && kindMap.get(obj) == 1) {
                            bnode.add(obj);
                        }
                    }
                    msg.addAll(bTriples);
                    bnode.remove(node);
                }
                allTriple.removeAll(msg);

                // to get hash
                Model model = ModelFactory.createDefaultModel();
                for (List<Integer> iter: msg) {
                    Resource subj = null;
                    switch (kindMap.get(iter.get(0))) {
                        case 0:
                            subj = model.createResource(textMap.get(iter.get(0)));
                            break;
                        case 1:
                            subj = model.createResource(AnonId.create(textMap.get(iter.get(0))));
                            break;
                    }

                    Property pred = model.createProperty(textMap.get(iter.get(1)));

                    switch (kindMap.get(iter.get(2))) {
                        case 0:
                            Resource obj = model.createResource(textMap.get(iter.get(2)));
                            model.add(subj, pred, obj);
                            break;
                        case 1:
                            Resource bobj = model.createResource(AnonId.create(textMap.get(iter.get(2))));
                            model.add(subj, pred, bobj);
                            break;
                        case 2:
                            Literal lobj = model.createLiteral(textMap.get(iter.get(2)));
                            model.add(subj, pred, lobj);
                            break;
                    }
                }
                StringWriter tripleWriter = new StringWriter();
                RDFDataMgr.write(tripleWriter, model, Lang.NTRIPLES);
//                    Set<String> triples = new HashSet<>(Arrays.asList(tripleWriter.toString().split("\n")));
                // for special resource: 17215(9166)
                Set<String> triples = new HashSet<>();
                String[] ts = tripleWriter.toString().split("\\.\n");
                for (String s: ts) {
                    triples.add(s + ".");
                }

                // Get the HubLabel.graph hash
                HashCode hash = leanAndLabel(triples);
                if (hash == null) {
                    System.out.println("WRONG HASH!");
                    for (String iter: triples) {
                        System.out.println(iter);
                    }
                    return 0;
                }

                // Update the HubLabel.database
                updatePst.setString(1, String.valueOf(hash));
                for (List<Integer> iter: msg) {
                    updatePst.setInt(3, iter.get(0));
                    updatePst.setInt(4, iter.get(1));
                    updatePst.setInt(5, iter.get(2));
                    updatePst.addBatch();
//                    submitBatchIndicator++;
                    batchRecorder.append(iter.get(0)).append(" ").append(iter.get(1)).append(" ").append(iter.get(2)).append(" ").append(hash).append("\n");
                }
                msgCount++;
                if (msgCount % 100 == 0) {
                    updatePst.executeBatch();
                    updatePst.clearBatch();
//                        submitBatchIndicator = 0;
                    batchRecorder = new StringBuilder();
                }
                if (msgCount % 10000 == 0) {
                    System.out.println("Finish " + msgCount + " msgs. ");
                }
                if (msgCount == batch) {
                    break;
                }
            }
            updatePst.executeBatch();
            updatePst.close();
            connection.close();

            System.out.println(resource + "\t" + msgCount);

        }catch (Exception e) {
            e.printStackTrace();
            try {
                PrintWriter writer = new PrintWriter("batch.txt");
                writer.print(batchRecorder);
                writer.close();
                System.out.println("Fail batch has been saved. ");
            } catch (Exception ee) {
                ee.printStackTrace();
            }
            return 0;
        }

        if (allTriple.isEmpty()) {
            return -1;
        }
        return 1;

    }

//            PreparedStatement getTriplePst = connection.prepareStatement(getTriple, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
//            getTriplePst.setFetchSize(Integer.MIN_VALUE);

    /**
     * Count the amount of MSGs.
     *
     * @param dataset
     */
    public static void countMSGs(int dataset) {
        long startTime = System.currentTimeMillis();
        String getTriple = "SELECT subject,predicate,object FROM triple WHERE file_id = " + dataset;
        String getTerm = "SELECT id,kind,iri FROM rdf_term WHERE file_id = " + dataset;
        Set<List<Integer>> allTriple = new HashSet<>(); // retain all triples
        Map<Integer, String> textMap = new HashMap<>();
        Map<Integer, Integer> kindMap = new HashMap<>();
        Map<Integer, Set<List<Integer>>> bnode2triple = new HashMap<>(); // each blank node -> set of triples containing it
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement getTermSt = connection.prepareStatement(getTerm);
            ResultSet resultSet1 = getTermSt.executeQuery();
            while (resultSet1.next()) {
                int id = resultSet1.getInt("id");
                int termType = resultSet1.getInt("kind");
                String text = resultSet1.getString("iri");

                kindMap.put(id, termType);
                textMap.put(id, text);

                if (termType == 1) {
                    bnode2triple.put(id, new HashSet<>()); // as a blank node
                }
            }
            PreparedStatement getTripleSt = connection.prepareStatement(getTriple);
            ResultSet resultSet = getTripleSt.executeQuery();
            while (resultSet.next()) {
                int subject = resultSet.getInt("subject");
                int predicate = resultSet.getInt("predicate");
                int object = resultSet.getInt("object");
                List<Integer> triple = new ArrayList<>(Arrays.asList(subject, predicate, object));
                allTriple.add(triple);
                if (kindMap.get(subject) == 1) {
                    bnode2triple.get(subject).add(triple);
                }
                if (kindMap.get(object) == 1) {
                    bnode2triple.get(object).add(triple);
                }
            }
            connection.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Dataset: " + dataset);
        System.out.println("All triple size: " + allTriple.size());

        int msgCount = 0;
        while (!allTriple.isEmpty()) {
            List<Integer> init = allTriple.iterator().next(); // get an initial triple
            Set<List<Integer>> msg = new HashSet<>();
            msg.add(init);
            Set<Integer> bnode = new HashSet<>(); // record bnodes in this MSG
            if (kindMap.get(init.get(0)) == 1) {
                bnode.add(init.get(0));
            }
            if (kindMap.get(init.get(2)) == 1) {
                bnode.add(init.get(2));
            }
            Set<Integer> containedBnode = new HashSet<>();
            while (!bnode.isEmpty()) {
                int node = bnode.iterator().next();
                containedBnode.add(node);
                Set<List<Integer>> bTriples = bnode2triple.get(node);
                for (List<Integer> iter: bTriples) {
                    int sub = iter.get(0);
                    int obj = iter.get(2);
                    if (!containedBnode.contains(sub) && kindMap.get(sub) == 1) {
                        bnode.add(sub);
                    }
                    if (!containedBnode.contains(obj) && kindMap.get(obj) == 1) {
                        bnode.add(obj);
                    }
                }
                msg.addAll(bTriples);
                bnode.remove(node);
            }
            allTriple.removeAll(msg);

            msgCount++;
            if (msgCount % 10000 == 0) {
                System.out.println("Finish " + msgCount + " msgs. ");
            }
        }
        long time = System.currentTimeMillis() - startTime;
        System.out.println(dataset + "\t" + msgCount + "\t" + time);
    }

    public static void main(String[] args) {
        String resourceFile = PATHS.FileBase + "resources.txt";
        labelAllDatasets(resourceFile, Integer.parseInt(args[0]), Integer.parseInt(args[1]), 1);

        String resourceFile2 = PATHS.FileBase + "resources-soc.txt";
        labelAllDatasets(resourceFile2, Integer.parseInt(args[0]), Integer.parseInt(args[1]), 2);

    }
}
