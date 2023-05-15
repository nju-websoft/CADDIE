package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {

    /** NOTE: You should change the following settings to your local HubLabel.database, i.e., url, name, user, password
     * We use MySQL Community Server --version 8.0.15 */

    public static String name = "com.mysql.cj.jdbc.Driver";
    public static String url = "jdbc:mysql://114.212.190.189:3306/dataset_analysis_2021apr?" +
        "useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&autoReconnect=true";
    public static String user = "user";
    public static String password = "passwd";

    public Connection conn = null;

    public static final int BATCH_SIZE = 5000;
  
    public DBUtil() {
        try {
            Class.forName(name);
            conn = DriverManager.getConnection(url, user, password);
//            System.out.println("~~~~Succeed in connecting the HubLabel.database~~~~");
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            this.conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
