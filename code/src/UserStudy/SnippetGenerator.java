package UserStudy;

import beans.RDFTerm;
import net.sf.json.JSONObject;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import util.DBUtil;
import util.PATHS;
import util.ReadFile;

import java.io.PrintWriter;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class SnippetGenerator {
    List<Integer> t;
    int curr = -1;

    private static List<Integer> test() {
        return new ArrayList<>();
    }

    public SnippetGenerator() {
        t = test();
        curr = 0;
    }


    public static void main(String[] args) {
        generateTextFile(PATHS.vldbBase + "UserStudy/text/");
//        generateJsonFile(PATHS.vldbBase + "UserStudy/json/");
    }

    private static void generateTextFile(String resultFolder) {
        try {
            String snippetFolder = PATHS.vldbBase + "UserStudy/QPCSGResult/";
            int i = 0;
            for (List<String> iter: ReadFile.readString(PATHS.vldbBase + "case-info.txt", "\t")) {
                int query = Integer.parseInt(iter.get(0));
                int dataset = Integer.parseInt(iter.get(3));
                List<String> triples = ReadFile.readString(snippetFolder + i + "-" + query + "-" + dataset + ".txt");
                String[] keywords = iter.get(2).split("\\s+");

                PrintWriter writer = new PrintWriter(resultFolder + i + ".txt");
                writer.print(snippet2NTString(dataset, triples, keywords)); // note to highlight
                writer.close();
                i++;
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

    private static String snippet2NTString(int dataset, List<String> triples, String[] keywords) {
        Map<Integer, RDFTerm> id2term = new HashMap<>();
        String[] resources = null;
        for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
            if (Integer.parseInt(iter.get(0)) == dataset) {
                resources = iter.get(1).split(" ");
                break;
            }
        }
        readTerms(dataset, resources, id2term);

        String result = "";
        for (String iter: triples){
            String[] item = iter.split(" ");
            int sid = Integer.parseInt(item[0]);
            int pid = Integer.parseInt(item[1]);
            int oid = Integer.parseInt(item[2]);
            String subject = id2term.get(sid).getIri();
            subject = subject.replace("\n", " ");
            subject = subject.replace("\t", " ");
            subject = subject.replace("<", "&lt");
            subject = subject.replace(">", "&gt");
            subject = subject.replaceAll("\\s+", " ");
            subject = highlightAllKeywords(subject, keywords);
            subject = subject.trim();

            String predicate = id2term.get(pid).getIri();
            predicate = predicate.replace("\n", " ");
            predicate = predicate.replace("\t", " ");
            predicate = predicate.replace("<", "&lt");
            predicate = predicate.replace(">", "&gt");
            predicate = predicate.replaceAll("\\s+", " ");
            predicate = highlightAllKeywords(predicate, keywords);
            predicate = predicate.trim();

            String object = id2term.get(oid).getIri();
            object = object.replace("\n", " ");
            object = object.replace("\t", " ");
            object = object.replace("<", "&lt");
            object = object.replace(">", "&gt");
            object = object.replaceAll("\\s+", " ");
            object = highlightAllKeywords(object, keywords);
            object = object.trim();

            if (id2term.get(sid).getType() == 1){
                result += subject + "&nbsp;&nbsp;&nbsp;&nbsp;";
            }
            else {
                result += "&lt" + subject + "&gt&nbsp;&nbsp;&nbsp;&nbsp;";
            }
            result += "&lt" + predicate + "&gt&nbsp;&nbsp;&nbsp;&nbsp;";

            if (id2term.get(oid).getType() == 2){
                result += "\"" + object + "\" .<br />";
            }
            else{
                result += "&lt" + object + "&gt .<br />";
            }
        }
        return result;
    }

    private static void generateJsonFile(String resultFolder) {
        try {
            String snippetFolder = PATHS.vldbBase + "UserStudy/QPCSGResult/";
            int i = 0;
            for (List<String> iter: ReadFile.readString(PATHS.vldbBase + "case-info.txt", "\t")) {
                int query = Integer.parseInt(iter.get(0));
                int dataset = Integer.parseInt(iter.get(3));
                List<String> triples = ReadFile.readString(snippetFolder + i + "-" + query + "-" + dataset + ".txt");
                String[] keywords = iter.get(2).split("\\s+");

                PrintWriter writer = new PrintWriter(resultFolder + i + ".txt");
                writer.print(snippet2JSON(dataset, triples, keywords)); // note to highlight
                writer.close();
                i++;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean containAnyKeywords(String source, String[] keywords) {
        for (String keyword : keywords) {
            if (source.toLowerCase().contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String snippet2JSON(int dataset, List<String> triples, String[] keywords) {
        Map<Integer, RDFTerm> id2term = new HashMap<>();
        String[] resources = null;
        for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
            if (Integer.parseInt(iter.get(0)) == dataset) {
                resources = iter.get(1).split(" ");
                break;
            }
        }
        readTerms(dataset, resources, id2term);

        String nodes = "";
        String edges = "";
        Set<Integer> ids = new HashSet<>();
        Set<Integer> propertySet = new HashSet<>();
        for (String iter: triples){
            String[] item = iter.split(" ");
            int sid = Integer.parseInt(item[0]);
            int pid = Integer.parseInt(item[1]);
            int oid = Integer.parseInt(item[2]);
            ids.add(sid);
            ids.add(oid);
            propertySet.add(pid);
            String predicate = id2term.get(pid).getLabel();
            predicate = predicate.replace("\n", " ");
            predicate = predicate.replace("\t", " ");
            predicate = predicate.replace("<", "&lt");
            predicate = predicate.replace(">", "&gt");
            predicate = predicate.replaceAll("\\s+", " ");
            predicate = predicate.trim();
            if (containAnyKeywords(predicate, keywords)) {
                edges += "{from: \"" + sid + "\", to: \"" + oid + "\", arrows:\"to\", label: \"" + predicate + "\", font: {color: \"red\"}},\n";
            } else {
                edges += "{from: \"" + sid + "\", to: \"" + oid + "\", arrows:\"to\", label: \"" + predicate + "\"},\n";
            }
        }
        for (int iter: ids) {
            if (propertySet.contains(iter)) continue;
            String label = id2term.get(iter).getLabel();
            label = label.replace("\n", " ");
            label = label.replace("\t", " ");
            label = label.replace("<", "&lt");
            label = label.replace(">", "&gt");
            label = label.replaceAll("\\s+", " ");
            label = label.trim();
            if (containAnyKeywords(label, keywords)) {
                nodes += "{id: \""+ iter + "\", label: \"" + label + "\", font: {color: \"red\"}},\n";
            } else {
                nodes += "{id: \""+ iter + "\", label: \"" + label + "\"},\n";
            }
        }

        JSONObject resultObj = new JSONObject();
        resultObj.put("nodes", nodes);
        if (edges.equals("")){
            resultObj.put("edges", "\"{}\"");
        }
        else {
            resultObj.put("edges", edges);
        }
        return resultObj.toString();
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

}
