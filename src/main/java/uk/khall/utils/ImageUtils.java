package uk.khall.utils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
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

    public static void rotate(BufferedImage from, BufferedImage to, double rotate)
    {
        // rotate around the center
        AffineTransform trans
                = AffineTransform.getRotateInstance(rotate,
                from.getWidth()/2, from.getHeight()/2);
        AffineTransformOp op = new AffineTransformOp(trans,
                AffineTransformOp.TYPE_BILINEAR);
        op.filter(from, to);
    }
    public static void rotate(Graphics2D g2d, BufferedImage img, double rotate)
    {
        // rotate around the center
        AffineTransform trans
                = AffineTransform.getRotateInstance(rotate,
                img.getWidth()/2, img.getHeight()/2);
        g2d.drawImage(img, trans, null);
    }

    public static BufferedImage rotateImage(BufferedImage image, int quadrants) {

        int w0 = image.getWidth();
        int h0 = image.getHeight();
        int w1 = w0;
        int h1 = h0;
        int centerX = w0 / 2;
        int centerY = h0 / 2;

        if (quadrants % 2 == 1) {
            w1 = h0;
            h1 = w0;
        }

        if (quadrants % 4 == 1) {
            centerX = h0 / 2;
            centerY = h0 / 2;
        } else if (quadrants % 4 == 3) {
            centerX = w0 / 2;
            centerY = w0 / 2;
        }

        AffineTransform affineTransform = new AffineTransform();
        affineTransform.setToQuadrantRotation(quadrants, centerX, centerY);
        AffineTransformOp opRotated = new AffineTransformOp(affineTransform,
                AffineTransformOp.TYPE_BILINEAR);
        BufferedImage transformedImage = new BufferedImage(w1, h1,
                image.getType());
        transformedImage = opRotated.filter(image, transformedImage);

        return transformedImage;

    }
}
