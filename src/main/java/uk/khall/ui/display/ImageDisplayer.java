package uk.khall.ui.display;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;


public class ImageDisplayer extends javax.swing.JFrame {
    BufferedImage sourceImage = null;
    private javax.swing.JScrollPane jScrollPane;
    private ImageCanvas canvasPanel = null;
    private int canvasWidth = 0;
    private int canvasHeight = 0;

    public ImageDisplayer(int canvasWidth, int canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        GridBagConstraints gridBagConstraints;
        jScrollPane = new javax.swing.JScrollPane();
        canvasPanel = new ImageCanvas();
        getContentPane().setLayout(new GridBagLayout());
        setTitle("Image Messer");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        jScrollPane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane.setViewportBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.RAISED));
        jScrollPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jScrollPaneMouseReleased(evt);
            }
        });

        canvasPanel.setFont(new Font("Dialog", 0, 11));
        canvasPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                canvasPanelMouseClicked(evt);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                canvasPanelMouseReleased(evt);
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                canvasPanelMousePressed(evt);
            }
        });
        canvasPanel.addMouseMotionListener(new java.awt.event.MouseAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                canvasPanelMouseMoved(evt);
            }

        });

        jScrollPane.setViewportView(canvasPanel);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = 6;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 4.0;
        gridBagConstraints.weighty = 4.0;
        getContentPane().add(jScrollPane, gridBagConstraints);
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (getCanvasWidth() == 0 || getCanvasHeight() == 0) {
            setSize(new Dimension(900, 1000));
        } else {
            setSize(new Dimension(getCanvasWidth(), getCanvasHeight()));
        }
        setLocation(10, 10);
    }

    private void jScrollPaneMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jScrollPaneMouseReleased
        // repaint the image Panel:
        canvasPanel.repaint();
    }//GEN-LAST:event_jScrollPaneMouseReleased

    private void canvasPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_demImagePanelMouseClicked
        // find the x and y co-ordinate of image panel:
        int clickX = evt.getX();
        int clickY = evt.getY();
        Color col = getColorPixel(clickX, clickY);

    }//GEN-LAST:event_canvasPanelMouseClicked

    private void canvasPanelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jScrollPaneMouseReleased
        // repaint the image Panel:

    }//GEN-LAST:event_jScrollPaneMouseReleased

    private void canvasPanelMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jScrollPaneMouseReleased
        // repaint the image Panel:

    }//GEN-LAST:event_jScrollPaneMouseReleased

    private void canvasPanelMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jScrollPaneMouseReleased
        // repaint the image Panel:

    }//GEN-LAST:event_jScrollPaneMouseReleased

    public BufferedImage getImage() {
        return canvasPanel.getImage();
    }

    public void setImage(BufferedImage image){
        canvasPanel.setImage(image);
    }
    /**
     * return theColorPixel representation of color at x y coordinates
     *
     * @param x
     * @param y
     * @return
     */
    public Color getColorPixel(int x, int y) {
        int avgr = 0;
        int avgg = 0;
        int avgb = 0;
        int avga = 0;
        int[] pixels = new int[1];
        Color sample = null;
        try {
            if (sourceImage != null) {
                PixelGrabber pix = new PixelGrabber(sourceImage, x, y, 1, 1, pixels, 0, 1);
                pix.grabPixels();
                int pixel = pixels[0];
                avga = ((pixel >> 24) & 0xff);
                avgr = ((pixel >> 16) & 0xff);
                avgg = ((pixel >> 8) & 0xff);
                avgb = ((pixel) & 0xff);
                sample = new Color(avgr, avgg, avgb, avga);
                System.out.println(" a= " + avga + " r= " + avgr + " g= " + avgg + " b= " + avgb);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sample;
    }
    /**
     *
     *
     * @return the canvasWidth
     */
    private int getCanvasWidth() {
        return canvasWidth;
    }

    /**
     * @param canvasWidth the canvasWidth to set
     */
    private void setCanvasWidth(int canvasWidth) {
        this.canvasWidth = canvasWidth;
    }

    /**
     * @return the canvasHeight
     */
    private int getCanvasHeight() {
        return canvasHeight;
    }

    /**
     * @param canvasHeight the canvasHeight to set
     */
    private void setCanvasHeight(int canvasHeight) {
        this.canvasHeight = canvasHeight;
    }

    /**
     * Exit the Application
     */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm

    }//GEN-LAST:event_exitForm
}
