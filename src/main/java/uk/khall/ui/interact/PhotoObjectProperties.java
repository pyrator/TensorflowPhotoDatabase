package uk.khall.ui.interact;

public class PhotoObjectProperties {

    String imageName;
    String className;
    Float classScore;
    Integer pointX1;
    Integer pointY1;
    Integer pointX2;
    Integer pointY2;

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Float getClassScore() {
        return classScore;
    }

    public void setClassScore(Float classScore) {
        this.classScore = classScore;
    }

    public Integer getPointX1() {
        return pointX1;
    }

    public void setPointX1(Integer pointX1) {
        this.pointX1 = pointX1;
    }

    public Integer getPointY1() {
        return pointY1;
    }

    public void setPointY1(Integer pointY1) {
        this.pointY1 = pointY1;
    }

    public Integer getPointX2() {
        return pointX2;
    }

    public void setPointX2(Integer pointX2) {
        this.pointX2 = pointX2;
    }

    public Integer getPointY2() {
        return pointY2;
    }

    public void setPointY2(Integer pointY2) {
        this.pointY2 = pointY2;
    }

    public String toString(){
        return imageName + ":" + className+":"+classScore;
    }
}
