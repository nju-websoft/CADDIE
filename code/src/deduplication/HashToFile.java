package deduplication;

import util.PATHS;
import util.DBUtil;
import util.ReadFile;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class HashToFile {

    private static void datasetToFile(String datasetFile, String folder) {
        List<List<String>> record = ReadFile.readString(datasetFile, "\t");
        try {
            for (List<String> iter: record) {
                int dataset = Integer.parseInt(iter.get(0));
                Set<String> allHash = new HashSet<>();
                for (String res: iter.get(1).split(" ")) {
                    int resource = Integer.parseInt(res);
                    allHash.addAll(getHashForDataset(resource));
                }
                if (allHash.isEmpty()) {
                    System.out.println("Empty dataset: " + dataset);
                    return;
                }
                List<String> hashList = new ArrayList<>(allHash);
                Collections.sort(hashList);
                PrintWriter writer = new PrintWriter(folder + dataset + ".txt");
                for (String s: hashList) {
                    writer.println(s);
                }
                writer.close();
                System.out.println("Finish: " + dataset);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Set<String> getHashForDataset(int resource) {
        Set<String> result = new HashSet<>();
        String select = "SELECT msg_code FROM triple WHERE file_id = " + resource;
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement pst = connection.prepareStatement(select, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            pst.setFetchSize(Integer.MIN_VALUE);
//            PreparedStatement pst = connection.prepareStatement(select);
            ResultSet resultSet = pst.executeQuery();
            while (resultSet.next()) {
                result.add(resultSet.getString("msg_code"));
            }
            connection.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args) {
        datasetToFile(PATHS.FileBase + "dataset-resource-all.txt", PATHS.HashPath);
    }
}
