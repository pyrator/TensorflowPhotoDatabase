package uk.khall.ui.display;


import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by IntelliJ IDEA.
 * User: keith.hall
 * Date: 10-Nov-2007
 * Time: 13:55:19
 * To change this template use File | Settings | File Templates.
 */
public class ImageCanvas extends javax.swing.JPanel {

    private BufferedImage image = null;

    public void setImage(BufferedImage di) {
        //set an image in the panel
        image = di;
        if (di != null) {
            setPreferredSize(new Dimension(di.getWidth(), di.getHeight()));
        }
        revalidate();
        repaint();

    }

    /**
     * overrides the paintComponent in the parent class
     *
     * @param graphics the graphics object
     */
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2d = (Graphics2D) graphics;
        super.paintComponent(graphics);
        g2d.drawImage(image, 0, 0, this);
    }

    /**
     * Sets the prefrred size to width and height
     * will be used in painting a tiled image
     *
     * @param width
     * @param height
     */
    public void setCanvasSize(int width, int height) {
        setPreferredSize(new Dimension(width, height));
    }

    /**
     * Get the buffered image
     *
     * @return returns the image created by the Demfile
     */
    public BufferedImage getImage() {
        return image;
    }
}
