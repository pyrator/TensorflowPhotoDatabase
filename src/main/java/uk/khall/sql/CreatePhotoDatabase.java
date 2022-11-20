package uk.khall.sql;

import uk.khall.coco.CocoClasses;

import java.sql.*;
import java.util.TreeMap;

public class CreatePhotoDatabase {

    public static void main(String[] params){
        Connection connection;
        try {
            connection = SqlLiteBridge.createSqliteConnection("photoobjects.db");
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("DROP TABLE IF EXISTS images");
            stmt.close();
            stmt.executeUpdate("CREATE TABLE images(imageid INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " imagename TEXT NOT NULL , width INTEGER NOT NULL, height INTEGER NOT NULL ) ");
            stmt.close();
            stmt.executeUpdate("DROP TABLE IF EXISTS cococlass");
            stmt.close();
            stmt.executeUpdate("CREATE TABLE cococlass( id NUMBER NOT NULL , cococlassname TEXT NOT NULL ) ");
            stmt.close();
            CocoClasses cocoClasses = new CocoClasses();
            PreparedStatement pstmt = connection.prepareStatement("insert into cococlass (id, cococlassname) values (?, ?)");
            TreeMap <Float, String> cocoClasseTree = cocoClasses.getCocoTreeMap();
            for (Float id : cocoClasseTree.keySet()) {
                pstmt.setFloat(1, id);
                pstmt.setString(2, cocoClasseTree.get(id));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            pstmt.close();
            stmt.executeUpdate("DROP TABLE IF EXISTS photoobjects");
            stmt.close();
            stmt.executeUpdate("CREATE TABLE photoobjects( id NUMBER NOT NULL, imageid INTEGER NOT NULL, x1 REAL, y1 REAL, x2 REAL, y2 REAL, score REAL ) ");
            stmt.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {


        }
    }



}
