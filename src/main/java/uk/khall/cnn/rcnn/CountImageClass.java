package uk.khall.cnn.rcnn;

import uk.khall.sql.SqlLiteBridge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CountImageClass {
    public static void main(String[] params) throws SQLException {
        try(Connection connection = SqlLiteBridge.createSqliteConnection("photoobjects.db")) {
            String queryString ="select \n" +
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
                //D:\Users\theke\java projects\BirdClassification\testimages\P1270461.JPG|person|0.99751353263855|523.902351379395|1472.6385269165|951.691291809082|3209.04553985596
                //Retrieve from DB
                int imageTotal = rs.getInt(1);
                String className = rs.getString(2);
                System.out.println("Class = " + className + " total = " + imageTotal);
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
