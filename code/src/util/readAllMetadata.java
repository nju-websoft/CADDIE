package util;

import beans.Dataset;
import beans.Resource;
import util.DBUtil;
import util.PATHS;
import util.ReadFile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class readAllMetadata {

    public static Map<Integer, Dataset> getAllMetadata() {
        Map<Integer, Dataset> result = new HashMap<>();

        Map<Integer, Set<Integer>> dataset2resource = new HashMap<>();
        for (List<String> iter: ReadFile.readString(PATHS.FileBase + "dataset-resource-all.txt", "\t")) {
            int datasetId = Integer.parseInt(iter.get(0));
            Set<Integer> res = new HashSet<>();
            for (String s: iter.get(1).split(" ")) {
                res.add(Integer.parseInt(s));
            }
            dataset2resource.put(datasetId, res);
            Dataset dataset = new Dataset();
            dataset.setIsComplete(iter.get(1).length() == iter.get(2).length());
            result.put(datasetId, dataset);
        }

        String select = "SELECT * FROM dataset_summary";
        try {
            Connection connection = new DBUtil().conn;
            PreparedStatement preparedStatement = connection.prepareStatement(select);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("dataset_id");
                if (!dataset2resource.containsKey(id)) {
                    continue;
                }
                Set<Integer> recordRes = dataset2resource.get(id);
                int resourceId = resultSet.getInt("file_id");
                if (!recordRes.contains(resourceId)) {
                    continue;
                }
                Dataset dataset = result.get(id);
                if (dataset.getTitle() == null) {
                    dataset.setId(id);
                    dataset.setTitle(resultSet.getString("title"));
                    dataset.setDescription(resultSet.getString("description"));
                    dataset.setAuthor(resultSet.getString("author"));
                    dataset.setUrl(resultSet.getString("url"));
                    dataset.setLicense(resultSet.getString("license"));
                    dataset.setVersion(resultSet.getString("version"));
                    dataset.setPortal(resultSet.getString("db_name"));
                    dataset.setSource(resultSet.getString("source"));
//                    String title = resultSet.getString("title");
//                    String description = resultSet.getString("description");
//                    String author = resultSet.getString("author");
//                    String url = resultSet.getString("url");
//                    String license = resultSet.getString("license");
//                    String version = resultSet.getString("version");
//                    String portal = resultSet.getString("db_name");
//                    String source = resultSet.getString("source");
//                    dataset = new Dataset(id, title, description, author, url, license, version, portal, source);
                }

                String name = resultSet.getString("name");
                String download = resultSet.getString("download");
                String created = resultSet.getString("created");
                String updated = resultSet.getString("updated");
                String notes = resultSet.getString("notes");
                String size = resultSet.getString("size");
                String str_tags = resultSet.getString("tags");
                List<String> tags = new ArrayList<>();
                if (str_tags!=null && str_tags.length() > 0) {
                    tags = new ArrayList<>(Arrays.asList(str_tags.split(";")));
                }
                String version = resultSet.getString("version");
                Resource dr = new Resource(resourceId, name, download, created, updated, notes, size, tags, version);
                dataset.addResource(dr);

                result.put(id, dataset);
            }
            connection.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Map<Set<Integer>, Double> getContentSimilarityMatrix() {
        Map<Set<Integer>, Double> result = new HashMap<>();
        for (List<Integer> iter: ReadFile.readInteger(PATHS.FileBase + "overlap-nonzero.txt", "\t")) {
            double score = ((double) iter.get(2)) / (iter.get(3) + iter.get(4) - iter.get(2));
            Set<Integer> key = new HashSet<>(Arrays.asList(iter.get(0), iter.get(1)));
            result.put(key, score);
        }
        return result;
    }

}
