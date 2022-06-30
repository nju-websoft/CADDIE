package MSGLabeling;

import util.DBUtil;
import util.PATHS;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class preparation {
    /**
     *
     * @author Xiaxia Wang
     */

    /**
     * To select all distinct local-id of local-stored datasets.
     */
    private static void getAllDatasets(String fileName) {
        String select = "SELECT DISTINCT file_id FROM triple ORDER BY file_id ASC";
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement pst = connection.prepareStatement(select);
            ResultSet resultSet = pst.executeQuery();
            StringBuilder dataset = new StringBuilder();
            int count = 0;
            while (resultSet.next()) {
                dataset.append(resultSet.getInt("file_id")).append(" ");
                count++;
            }
            PrintWriter writer = new PrintWriter(fileName);
            writer.print(dataset.toString().trim());
            writer.close();
            connection.close();
            System.out.println("Total: " + count);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * To select all distinct local-id of local-stored datasets.
     */
    private static void getSocrataDatasets(String fileName) {
        String select = "SELECT DISTINCT file_id FROM triple_socrata ORDER BY file_id ASC";
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement pst = connection.prepareStatement(select);
            ResultSet resultSet = pst.executeQuery();
            StringBuilder dataset = new StringBuilder();
            int count = 0;
            while (resultSet.next()) {
                dataset.append(resultSet.getInt("file_id")).append(" ");
                count++;
            }
            PrintWriter writer = new PrintWriter(fileName);
            writer.print(dataset.toString().trim());
            writer.close();
            connection.close();
            System.out.println("Total: " + count);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * To get the map of dataset -> resources
     * @param fileName
     */
    private static void getDatasetResourceMap(String fileName) {
        String select1 = "SELECT DISTINCT dataset_id, file_id FROM dataset_summary";
        String select = "SELECT DISTINCT file_id FROM triple_socrata";
        Set<Integer> resources = new HashSet<>();
        Map<Integer, Set<Integer>> dataset2resource = new TreeMap<>();
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement pst = connection.prepareStatement(select);
            ResultSet resultSet = pst.executeQuery();
            while (resultSet.next()) {
                resources.add(resultSet.getInt("file_id"));
            }
            PreparedStatement pst1 = connection.prepareStatement(select1);
            ResultSet resultSet1 = pst1.executeQuery();
            while (resultSet1.next()) {
                int dataset = resultSet1.getInt("dataset_id");
                int resource = resultSet1.getInt("file_id");
                if (resources.contains(resource)) {
                    Set<Integer> existRes = dataset2resource.getOrDefault(dataset, new TreeSet<>());
                    existRes.add(resource);
                    dataset2resource.put(dataset, existRes);
                }
            }
            connection.close();

            int resourceCount = 0;
            PrintWriter writer = new PrintWriter(fileName);
            for (Map.Entry<Integer, Set<Integer>> iter: dataset2resource.entrySet()) {
                StringBuilder res = new StringBuilder();
                for (int i: iter.getValue()) {
                    res.append(i).append(" ");
                }
                writer.println(iter.getKey() + "\t" + res.toString().trim());
                resourceCount += iter.getValue().size();
            }
            writer.close();
            System.out.println("Resource count: " + resourceCount);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        getAllDatasets(PATHS.FileBase + "resources.txt"); // Total: 14117
//        getDatasetResourceMap(PATHS.FileBase + "DatasetToResource.txt");
//        getSocrataDatasets(PATHS.FileBase + "resources-soc.txt"); // Total: 19054
        getDatasetResourceMap(PATHS.FileBase + "dataset-resource-all.txt");
    }
}
