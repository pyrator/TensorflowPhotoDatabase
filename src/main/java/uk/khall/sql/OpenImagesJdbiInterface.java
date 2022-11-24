package uk.khall.sql;


import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import uk.khall.beans.ImageBean;
import uk.khall.beans.ObjectBean;

public interface OpenImagesJdbiInterface {
    @SqlUpdate ("insert into images (imagename, width, height) values (:imageName, :width, :height)")
    @GetGeneratedKeys("imageid")
    Long insertImage(@BindBean ImageBean imageBean);

    @SqlUpdate ("insert into openimagephotoobjects (id, imageid, x1, y1, x2, y2, score) values (:classId, :imageId, :x1, :y1, :x2, :y2, :score)")
    void insertObject(@BindBean ObjectBean objectBean);

}
