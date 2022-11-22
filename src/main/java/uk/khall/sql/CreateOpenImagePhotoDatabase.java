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
            stmt.executeUpdate("""
                    CREATE TABLE images(
                    imageid INTEGER PRIMARY KEY AUTOINCREMENT, 
                    imagename TEXT NOT NULL , 
                    width INTEGER NOT NULL, 
                    height INTEGER NOT NULL ) 
                    """);
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
            stmt.executeUpdate("CREATE TABLE openimagephotoobjects" +
                    "( " +
                    "id NUMBER NOT NULL, " +
                    "imageid INTEGER NOT NULL, " +
                    "x1 REAL, " +
                    "y1 REAL, " +
                    "x2 REAL, " +
                    "y2 REAL, " +
                    "score REAL, " +
                    "FOREIGN KEY (imageid) " +
                    "REFERENCES images (imageid) " +
                    "ON DELETE CASCADE );");
            stmt.close();
            stmt.executeUpdate("""
                    CREATE VIEW IF NOT EXISTS ClassTotals (total, className) 
                    AS 
                    select 
                    count(images.imagename) as total, 
                    openimageclass.openimageclassname 
                    from images 
                    join openimagephotoobjects on openimagephotoobjects.imageid = images.imageid 
                    join openimageclass on openimageclass.id = openimagephotoobjects.id 
                    group by openimageclass.openimageclassname
                    """);
            stmt.close();
            stmt.executeUpdate(
                    """
                    CREATE VIEW IF NOT EXISTS ClassFile (imageName, className) 
                    AS 
                    select 
                    images.imagename, 
                    openimageclass.openimageclassname 
                    from images 
                    join openimagephotoobjects on openimagephotoobjects.imageid = images.imageid 
                    join openimageclass on openimageclass.id = openimagephotoobjects.id
                    """
                    );
            stmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {


        }
    }

    public static void main2(String[] params) {
        Connection connection;
        try {
            connection = SqlLiteBridge.createSqliteConnection("openimagephotoobjects.db");
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("""
                    PRAGMA foreign_keys=off;
                    
                    BEGIN TRANSACTION;
                    
                    ALTER TABLE openimagephotoobjects RENAME TO _openimagephotoobjects_old;
                    
                    CREATE TABLE openimagephotoobjects
                    ( 
                    id NUMBER NOT NULL, 
                    imageid INTEGER NOT NULL, 
                    x1 REAL, 
                    y1 REAL, 
                    x2 REAL, 
                    y2 REAL, 
                    score REAL, 
                    FOREIGN KEY (imageid) REFERENCES images (imageid) ON DELETE CASCADE 
                    ); 
                    
                    INSERT INTO openimagephotoobjects SELECT * FROM _openimagephotoobjects_old;
                    
                    COMMIT;
                    
                    PRAGMA foreign_keys=on;
                    """);
            stmt.close();
        }catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {


        }
    }
}
