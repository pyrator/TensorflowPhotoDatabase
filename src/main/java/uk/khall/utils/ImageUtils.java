package uk.khall.utils;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtils {
    public static BufferedImage resizeImage(BufferedImage bi, int width, int height, Component comp){
        Image scaledImage;
        if ((bi.getWidth() <= bi.getHeight())){
            scaledImage=bi.getScaledInstance(-1, height, BufferedImage.SCALE_SMOOTH);
        } else {
            scaledImage=bi.getScaledInstance(width, -1, BufferedImage.SCALE_SMOOTH);
        }
        return createBufferedImage(scaledImage, BufferedImage.TYPE_INT_RGB, comp);
    }


    public static BufferedImage createBufferedImage(Image imageIn, int imageType, Component comp) {
        MediaTracker mt = new MediaTracker(comp);
        mt.addImage(imageIn, 0);
        try {
            mt.waitForID(0);
        } catch (InterruptedException ie) {
        }
        int w = imageIn.getWidth(null);
        int h = imageIn.getHeight(null);
        if(w < 1){
            w = 1;
        }
        if(h < 1){
            h = 1;
        }
        BufferedImage bufferedImageOut = new BufferedImage(w, h, imageType);
        Graphics g = bufferedImageOut.getGraphics();
        g.drawImage(imageIn, 0, 0, null);

        return bufferedImageOut;
    }
}
