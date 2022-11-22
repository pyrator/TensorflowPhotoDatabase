package uk.khall.ui;


import javax.swing.JFrame;

public class GUIThread implements Runnable {

    private String openType;
    private String filePath;
    private String filterName;
    private String filterDesc;
    private String selDir;
    private Integer selectValue;
    private String suggestedFileName;

    public String getFilePath() {
        return filePath;
    }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    public String getOpenType() {
        return openType;
    }
    public void setOpenType(String openType) {
        this.openType = openType;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getFilterDesc() {
        return filterDesc;
    }

    public void setFilterDesc(String filterDesc) {
        this.filterDesc = filterDesc;
    }

    public String getSelDir() {
        return selDir;
    }

    public void setSelDir(String selDir) {
        this.selDir = selDir;
    }

    public Integer getSelectValue() {
        return selectValue;
    }

    public void setSelectValue(Integer selectValue) {
        this.selectValue = selectValue;
    }

    public String getSuggestedFileName() {
        return suggestedFileName;
    }

    public void setSuggestedFileName(String suggestedFileName) {
        this.suggestedFileName = suggestedFileName;
    }

    public GUIThread(String openType, String selDir ){
        this.openType = openType;
        this.selDir=selDir;
    }

    public GUIThread(String openType, String filterName, String filterDesc, String selDir ){
        this.openType = openType;
        this.filterName=filterName;
        this.filterDesc = filterDesc;
        this.selDir=selDir;

}
    public GUIThread(String openType, String filterName, String filterDesc, String selDir, String suggestedFileName ){
        this.openType = openType;
        this.filterName=filterName;
        this.filterDesc = filterDesc;
        this.selDir=selDir;
        this.suggestedFileName = suggestedFileName;
    }
    public GUIThread(String openType ){
        this.openType = openType;
    }
    public void run(){
        if (getOpenType().equalsIgnoreCase("select")){
            //Create and set up the content pane.
            ComboBoxFrame newContentPane = new ComboBoxFrame(new JFrame(), true);
            newContentPane.setVisible(true);
            setSelectValue(newContentPane.getScaleValue());
            //System.out.println(newContentPane.getScaleValue());

        }   else if (getOpenType().equalsIgnoreCase("slide")){
            //Create and set up the content pane.
            SliderFrame newContentPane = new SliderFrame(new JFrame(), true);
            newContentPane.setVisible(true);
            setSelectValue(newContentPane.getScaleValue());
            //System.out.println(newContentPane.getScaleValue());
        } else {
            if (suggestedFileName!=null) {
                FileChooser dc = new FileChooser(new JFrame(), true,
                        getOpenType(), getFilterName(), getFilterDesc(), getSelDir(), getSuggestedFileName());
                dc.setVisible(true);
                setFilePath(dc.getFilePath());
            } else if (filterDesc!=null ) {
                FileChooser dc = new FileChooser(new JFrame(), true,
                        getOpenType(), getFilterName(), getFilterDesc(), getSelDir());
                dc.setVisible(true);
                setFilePath(dc.getFilePath());
            } else {
                FileChooser dc = new FileChooser(new JFrame(), true, getSelDir());
                dc.setVisible(true);
                setFilePath(dc.getFilePath());
            }
        }
    }
}
