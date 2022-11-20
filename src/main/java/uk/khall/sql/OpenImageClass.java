package uk.khall.sql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "openimageclass")
public class OpenImageClass {
    @Id
    private Long imageNetKey;
    @Column(name = "openimageclassname")
    private String imageNetClassName;

    public Long getImageNetKey() {
        return imageNetKey;
    }

    public void setImageNetKey(Long imageNetKey) {
        this.imageNetKey = imageNetKey;
    }

    public String getImageNetClassName() {
        return imageNetClassName;
    }

    public void setImageNetClassName(String imageNetClassName) {
        this.imageNetClassName = imageNetClassName;
    }
}
