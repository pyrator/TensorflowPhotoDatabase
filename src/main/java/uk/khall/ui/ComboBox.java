package uk.khall.ui;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ComboBox extends JPanel implements ActionListener {
    private Integer scaleValue= 256;
    private Boolean isSet = false;
    public ComboBox() {
        super(new BorderLayout());

        Integer[] scaleValues = {128, 256, 512, 1024};
        JComboBox scaleValuesList = new JComboBox(scaleValues);
        scaleValuesList.setSelectedIndex(1);
        scaleValuesList.addActionListener(this);
        //Lay out the demo.
        add(scaleValuesList, BorderLayout.PAGE_START);
    }

    /**
     * Listens to the combo box.
     */
    public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox) e.getSource();
        scaleValue = (Integer) cb.getSelectedItem();
        isSet=true;
    }

    public Integer getScaleValue() {
        return scaleValue;
    }

    public void setScaleValue(Integer scaleValue) {
        this.scaleValue = scaleValue;
    }

    public Boolean getSet() {
        return isSet;
    }

    public void setSet(Boolean set) {
        isSet = set;
    }

}
