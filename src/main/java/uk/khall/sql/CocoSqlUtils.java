package uk.khall.sql;

import org.jdbi.v3.core.Jdbi;
import uk.khall.ui.interact.PhotoObjectProperties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class CocoSqlUtils {
    public static ArrayList<PhotoObjectProperties> getPhotoObjectProperties(String imageName) {
        ArrayList<PhotoObjectProperties> arrayList = new ArrayList<>();
        try(Connection connection = SqlLiteBridge.createSqliteConnection("photoobjects.db")) {

            String queryString = "select \n" +
                    "images.imagename, \n" +
                    "cococlass.cococlassname, \n" +
                    "photoobjects.score, \n" +
                    "(photoobjects.x1 * images.width) as pointx1, \n" +
                    "(photoobjects.y1 * images.height) as pointy1, \n" +
                    "(photoobjects.x2 * images.width) as pointx2, \n" +
                    "(photoobjects.y2 * images.height) as pointy2 \n" +
                    "from images\n" +
                    "join photoobjects on photoobjects.imageid = images.imageid \n" +
                    "join cococlass on cococlass.id = photoobjects.id \n" +
                    "where images.imagename = ?";
            PreparedStatement pstmt;
            pstmt = connection.prepareStatement(queryString);
            pstmt.setString(1, imageName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                    PhotoObjectProperties properties = new PhotoObjectProperties();
                    //Retrieve from DB
                    properties.setImageName(rs.getString(1));
                    properties.setClassName(rs.getString(2));
                    properties.setClassScore(rs.getFloat(3));
                    properties.setPointX1((int) rs.getFloat(4));
                    properties.setPointY1((int) rs.getFloat(5));
                    properties.setPointX2((int) rs.getFloat(6));
                    properties.setPointY2((int) rs.getFloat(7));
                    arrayList.add(properties);
                }

            pstmt.close();
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return arrayList;
    }

    public static TreeMap<String, Integer> getClassTotals() {
        TreeMap<String, Integer> classTotals = new TreeMap<String, Integer>();
        try (Connection connection = SqlLiteBridge.createSqliteConnection("photoobjects.db")) {
            String queryString = "select \n" +
                    "count(images.imagename) as total, \n" +
                    "cococlass.cococlassname \n" +
                    "from images\n" +
                    "join photoobjects on photoobjects.imageid = images.imageid \n" +
                    "join cococlass on cococlass.id = photoobjects.id \n" +
                    "group by cococlass.cococlassname";
            PreparedStatement pstmt;
            pstmt = connection.prepareStatement(queryString);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //Retrieve from DB
                int imageTotal = rs.getInt(1);
                String className = rs.getString(2);
                classTotals.put(className, imageTotal);
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classTotals;
    }
    public static ArrayList<String> getFilesContainingClass(String className){
        ArrayList<String> files = new ArrayList<>();
        try (Connection connection = SqlLiteBridge.createSqliteConnection("photoobjects.db")) {
            String queryString = "select \n" +
                    "images.imagename \n" +
                    "from images\n" +
                    "join photoobjects on photoobjects.imageid = images.imageid \n" +
                    "join cococlass on cococlass.id = photoobjects.id \n" +
                    "where cococlass.cococlassname = ?";
            PreparedStatement pstmt;

            pstmt = connection.prepareStatement(queryString);
            pstmt.setString(1, className);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //Retrieve from DB
                String imageName = rs.getString(1);
                files.add(imageName);
            }
            pstmt.close();

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return files;
    }
    public static TreeMap<String, Integer> getClassedInFolder(String folderName) {
        TreeMap<String, Integer> classTotals = new TreeMap<String, Integer>();
        try (Connection connection = SqlLiteBridge.createSqliteConnection("photoobjects.db")) {
            String queryString = "select \n" +
                    "count(images.imagename) as total, \n" +
                    "cococlass.cococlassname \n" +
                    "from images\n" +
                    "join photoobjects on photoobjects.imageid = images.imageid \n" +
                    "join cococlass on cococlass.id = photoobjects.id \n" +
                    "where rtrim(imagename, replace(images.imagename, '\\', '')) like ?" +
                    "group by cococlass.cococlassname";
            PreparedStatement pstmt;
            pstmt = connection.prepareStatement(queryString);
            pstmt.setString(1, folderName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //Retrieve from DB
                int imageTotal = rs.getInt(1);
                String className = rs.getString(2);
                //System.out.printf(""+ className + ": " +imageTotal + "\n");
                classTotals.put(className, imageTotal);
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classTotals;
    }
    public static ArrayList<String> getFolders(){
        ArrayList<String> files = new ArrayList<>();
        try (Connection connection = SqlLiteBridge.createSqliteConnection("photoobjects.db")) {
            String queryString = "select distinct rtrim(imagename, replace(imagename, '\\', '')) as folderName from images";
            PreparedStatement pstmt;

            pstmt = connection.prepareStatement(queryString);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //Retrieve from DB
                String folderName = rs.getString(1);
                files.add(folderName);
            }
            pstmt.close();

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return files;
    }

    public static void main (String[] params){
        ArrayList<String> folders = getFolders();
        for (String folder : folders){
            System.out.println(folder);
        }
        String folderName = "D:\\Users\\theke\\Pictures\\Lindisfarne and Low Hauxley Nov 2022\\";
        TreeMap<String, Integer> classTotals =
                CocoSqlUtils.getClassedInFolder( folderName);
        for (String key : classTotals.keySet()){
            System.out.printf(""+ key + ": " +classTotals.get(key)+ "\n");
        }
    }
}
