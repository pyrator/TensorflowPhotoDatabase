package uk.khall.sql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cococlass")
public class CocoClass {
    @Id
    private Long cocoKey;
    @Column(name = "cococlassname")
    private String cocoClassName;

    public Long getCocoKey() {
        return cocoKey;
    }

    public void setCocoKey(Long cocoKey) {
        this.cocoKey = cocoKey;
    }

    public String getCocoClassName() {
        return cocoClassName;
    }

    public void setCocoClassName(String cocoClassName) {
        this.cocoClassName = cocoClassName;
    }
}
