package uk.khall.ui;


import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;

public class SliderFrame extends JDialog implements
        ChangeListener {
    private Integer scaleValue = 50;
    private JSlider jSlider;
    private JButton okButton;
    static final int scaleMIN = 0;
    static final int scaleMAX = 100;
    static final int scaleINIT = 50;    //initial frames per second
    public SliderFrame(Frame parent, boolean modal){
        super(parent, modal);
        GridBagConstraints gridBagConstraints;
        jSlider = new JSlider(JSlider.HORIZONTAL,
                scaleMIN, scaleMAX, scaleINIT);
        jSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                scaleValReleased(evt);
            }
        });
        jSlider.setOpaque(true); //content panes must be opaque
        jSlider.addChangeListener(this);

        //Turn on labels at major tick marks.

        jSlider.setMajorTickSpacing(10);
        jSlider.setMinorTickSpacing(1);
        jSlider.setPaintTicks(true);
        jSlider.setPaintLabels(true);
        getContentPane().setLayout(new GridBagLayout());
        setTitle("Blend %");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 4.0;
        gridBagConstraints.weighty = 4.0;
        getContentPane().add(jSlider, gridBagConstraints);
        okButton = new JButton();
        okButton.setText("OK");
        okButton.setToolTipText("Set %age Value");
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

    private void scaleValReleased(MouseEvent mouseEvent){
        setScaleValue(jSlider.getValue());
    }
    private void setOkButton(MouseEvent mouseEvent){
        setScaleValue(jSlider.getValue());
        setVisible(false);
        dispose();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        int val = ((JSlider)e.getSource()).getValue();
        jSlider.setToolTipText(""+val);
    }
}