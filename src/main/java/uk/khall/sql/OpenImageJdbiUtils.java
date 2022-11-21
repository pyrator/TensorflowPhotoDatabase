package uk.khall.sql;

import org.jdbi.v3.core.Jdbi;
import uk.khall.ui.interact.PhotoObjectProperties;

import java.util.ArrayList;
import java.util.List;


public class OpenImageJdbiUtils {
    public static ArrayList<ClassTotals> getClassTotals() {

        Jdbi jdbi =  JdbiBridge.createSqliteJdbiConnection("openimagephotoobjects.db");
        List<ClassTotals> classTotals = jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM ClassTotals order by total")
                .mapToBean(ClassTotals.class)
                .list());
        return new ArrayList<>(classTotals);
    }
    public static ArrayList<PhotoObjectProperties> getPhotObjectProperties(String imageName) {
        Jdbi jdbi =  JdbiBridge.createSqliteJdbiConnection("openimagephotoobjects.db");
        String queryString ="select \n" +
                "images.imagename as imageName, \n" +
                "openimageclass.openimageclassname as className, \n" +
                "openimagephotoobjects.score as classScore, \n" +
                "(openimagephotoobjects.x1 * images.width) as pointX1, \n" +
                "(openimagephotoobjects.y1 * images.height) as pointY1, \n" +
                "(openimagephotoobjects.x2 * images.width) as pointX2, \n" +
                "(openimagephotoobjects.y2 * images.height) as pointY2 \n" +
                "from images\n" +
                "join openimagephotoobjects on openimagephotoobjects.imageid = images.imageid \n" +
                "join openimageclass on openimageclass.id = openimagephotoobjects.id \n" +
                "where images.imagename = ?";
        List<PhotoObjectProperties> imageObjects =  jdbi.withHandle(handle -> handle.createQuery(queryString)
                .bind(0, imageName)
                .mapToBean(PhotoObjectProperties.class)
                .list());
        return new ArrayList<>(imageObjects);
    }
    public static ArrayList<String> getFilesContainingClass(String className){
        Jdbi jdbi =  JdbiBridge.createSqliteJdbiConnection("openimagephotoobjects.db");
        String queryString = "select \n" +
                "images.imagename \n" +
                "from images\n" +
                "join openimagephotoobjects on openimagephotoobjects.imageid = images.imageid \n" +
                "join openimageclass on openimageclass.id = openimagephotoobjects.id \n" +
                "where openimageclass.openimageclassname = ?";
        List<String> files =  jdbi.withHandle(handle -> handle.createQuery(queryString)
                .bind(0, className)
                .mapTo(String.class)
                .list());
        return new ArrayList<>(files);
    }
    public static ArrayList<ClassFile> getClassesInFolder(String folderName){
        Jdbi jdbi =  JdbiBridge.createSqliteJdbiConnection("openimagephotoobjects.db");
        /*String queryString = "select \n" +
                "count(images.imagename) as total, \n" +
                "cococlass.cococlassname \n" +
                "from images\n" +
                "join photoobjects on photoobjects.imageid = images.imageid \n" +
                "join cococlass on cococlass.id = photoobjects.id \n" +
                "where rtrim(imagename, replace(images.imagename, '\\', '')) like ?" +
                "group by cococlass.cococlassname";*/


        String queryString = "select * from ClassFile where imagename like ?";
        List<ClassFile> files =  jdbi.withHandle(handle -> handle.createQuery(queryString)
                .bind(0, folderName+"%")
                .mapToBean(ClassFile.class)
                .list());
        return new ArrayList<>(files);
    }
    public static ArrayList<String> getFolders(){
        Jdbi jdbi =  JdbiBridge.createSqliteJdbiConnection("openimagephotoobjects.db");
        String queryString = "select distinct rtrim(imagename, replace(imagename, '\\', '')) from images";
        List<String> files =  jdbi.withHandle(handle -> handle.createQuery(queryString)
                .mapTo(String.class)
                .list());
        return new ArrayList<>(files);
    }
    public static ArrayList<String> getFilenamesInFolder(String folderName){
        Jdbi jdbi =  JdbiBridge.createSqliteJdbiConnection("openimagephotoobjects.db");
        String queryString = "select replace(imagename, rtrim(imagename, replace(imagename, '\\', '')), '') from images where imagename like ?";
        List<String> files =  jdbi.withHandle(handle -> handle.createQuery(queryString)
                .bind(0, folderName+"%")
                .mapTo(String.class)
                .list());
        return new ArrayList<>(files);
    }
    public static void main(String[] params){
        ArrayList<String> folders = getFolders();
        for (String folder : folders) {
            System.out.println("folder " + folder);
/*            ArrayList<ClassFile> classFilesList = getClassesInFolder(folder);
            for (ClassFile classFiles : classFilesList) {
                System.out.println(folder + " " + classFiles.getImageName() + " " + classFiles.getClassName());
            }*/
/*            ArrayList<String> files = getFilenamesInFolder(folder);
            for (String file : files ){
                System.out.println(folder + " : " + file);
            }*/
        }
        String folder = "D:\\Users\\theke\\Pictures\\Wallington with Eli April 2022\\";
        ArrayList<String> files = getFilenamesInFolder(folder);
        for (String file : files ){
            System.out.println(folder + " : " + file);
        }
        ArrayList<PhotoObjectProperties> properties = getPhotObjectProperties(folder + "P1040095.JPG");
        for (PhotoObjectProperties property : properties){
            System.out.println(property);
        }
    }
}
