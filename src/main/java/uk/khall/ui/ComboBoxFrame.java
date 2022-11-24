package uk.khall.ui;

import javax.swing.JButton;
import javax.swing.JDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;

public class ComboBoxFrame extends JDialog {
    private Integer scaleValue = 256;
    private ComboBox comboBox;
    private JButton okButton;
    public ComboBoxFrame(Frame parent, boolean modal){
        super(parent, modal);
        GridBagConstraints gridBagConstraints;
        comboBox = new ComboBox();
        comboBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                comboBoxReleased(evt);
            }
        });
        comboBox.setOpaque(true); //content panes must be opaque
        getContentPane().setLayout(new GridBagLayout());
        setTitle("Get Scale");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 4.0;
        gridBagConstraints.weighty = 4.0;
        getContentPane().add(comboBox, gridBagConstraints);
        okButton = new JButton();
        okButton.setText("OK");
        okButton.setToolTipText("Set Scale Value");
        okButton.setEnabled(true);
        okButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                setOkButton(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        getContentPane().add(okButton, gridBagConstraints);
        pack();
    }

    public Integer getScaleValue() {
        return scaleValue;
    }

    public void setScaleValue(Integer scaleValue) {
        this.scaleValue = scaleValue;
    }

    private void comboBoxReleased(MouseEvent mouseEvent){
        setScaleValue(comboBox.getScaleValue());
    }
    private void setOkButton(MouseEvent mouseEvent){
        setScaleValue(comboBox.getScaleValue());
        setVisible(false);
        dispose();
    }
}
