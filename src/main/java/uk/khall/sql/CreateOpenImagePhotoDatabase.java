package uk.khall.sql;


import uk.khall.imagenet.OpenImagesClasses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TreeMap;

public class CreateOpenImagePhotoDatabase {

    public static void main(String[] params){
        Connection connection;
        try {
            connection = SqlLiteBridge.createSqliteConnection("openimagephotoobjects.db");
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("DROP TABLE IF EXISTS images");
            stmt.close();
            stmt.executeUpdate("CREATE TABLE images(imageid INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " imagename TEXT NOT NULL , width INTEGER NOT NULL, height INTEGER NOT NULL ) ");
            stmt.close();
            stmt.executeUpdate("DROP TABLE IF EXISTS openimageclass");
            stmt.close();
            stmt.executeUpdate("CREATE TABLE openimageclass( id NUMBER NOT NULL , openimageclassname TEXT NOT NULL ) ");
            stmt.close();
            OpenImagesClasses openImagesClasses = new OpenImagesClasses();
            PreparedStatement pstmt = connection.prepareStatement("insert into openimageclass (id, openimageclassname) values (?, ?)");
            TreeMap <Float, String> openImageClassesTree = openImagesClasses.getOpenImageTreeMap();
            for (Float id : openImageClassesTree.keySet()) {
                pstmt.setFloat(1, id);
                pstmt.setString(2, openImageClassesTree.get(id));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            pstmt.close();
            stmt.executeUpdate("DROP TABLE IF EXISTS openimagephotoobjects");
            stmt.close();
            stmt.executeUpdate("CREATE TABLE openimagephotoobjects( id NUMBER NOT NULL, imageid INTEGER NOT NULL, x1 REAL, y1 REAL, x2 REAL, y2 REAL, score REAL ) ");
            stmt.close();
            //stmt.executeUpdate("DROP VIEW IF EXISTS ClassTotals");
            //stmt.close();
            stmt.executeUpdate("CREATE VIEW IF NOT EXISTS ClassTotals (total, className) \n" +
                    "AS \n" +
                    "select \n" +
                    "count(images.imagename) as total, \n" +
                    "openimageclass.openimageclassname \n" +
                    "from images\n" +
                    "join openimagephotoobjects on openimagephotoobjects.imageid = images.imageid \n" +
                    "join openimageclass on openimageclass.id = openimagephotoobjects.id \n" +
                    "group by openimageclass.openimageclassname");
            stmt.close();
            //stmt.executeUpdate("DROP VIEW IF EXISTS ClassFile");
            //stmt.close();
            stmt.executeUpdate("CREATE VIEW IF NOT EXISTS ClassFile (imageName, className) \n" +
                    "AS \n" +
                    "select \n" +
                    "images.imagename, \n" +
                    "openimageclass.openimageclassname \n" +
                    "from images\n" +
                    "join openimagephotoobjects on openimagephotoobjects.imageid = images.imageid \n" +
                    "join openimageclass on openimageclass.id = openimagephotoobjects.id"
                    );
            stmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {


        }
    }


}
