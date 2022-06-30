package util;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;

public class IndexUtil {

    private static void showFields(String indexPath){/**查看Field名称*/
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
//            for (IndexableField iter: reader.document(1).getFields()){
//                System.out.println(iter);
//            }
//            System.out.println(reader.maxDoc());
            for (int i = 0; i < reader.maxDoc(); i++){
                Document document = reader.document(i);
                System.out.print(document.get("key") + " --> ");
                System.out.println(document.get("value"));
            }
            reader.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static int countDocuments(String indexPath) {
        int docAmount = 0;
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
            docAmount = reader.maxDoc();
            reader.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return docAmount;
    }

    public static String getFieldValue(String indexPath, String idName, int docId, String fieldName) {
        String result = "";
        try {
            IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(indexPath))));
            Query query = IntPoint.newExactQuery(idName, docId);
            ScoreDoc[] docs = searcher.search(query, 1).scoreDocs;
            Document document = searcher.doc(docs[0].doc);
            result = document.get(fieldName);
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args){
//        showFields("D:/Work/ISWC2021Index/KeyKGPNoKeyword/8-1/invertedTable/");
        try {
            IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get("D:/Work/www2021/9959/"))));
            QueryParser parser = new QueryParser("text", new StandardAnalyzer());
            Query query = parser.parse("the");
            TopDocs docs = searcher.search(query, 100);
            ScoreDoc[] scores = docs.scoreDocs;
            System.out.println(scores.length);
//            String indexPath = "D:/Work/www2021/9959/";
//            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
//            for (int i = 0; i < reader.maxDoc(); i++) {
//                Document document = reader.document(i);
//                System.out.println("document: " + i);
//                System.out.println("node: " + document.get("node"));
//                System.out.println("text: " + document.get("text"));
//                System.out.println("triple: " + document.get("triple"));
//                System.out.println();
//            }
//            reader.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
