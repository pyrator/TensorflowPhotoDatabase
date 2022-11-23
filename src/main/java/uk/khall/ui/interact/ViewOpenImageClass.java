package uk.khall.ui.interact;

import uk.khall.sql.OpenImageJdbiUtils;
import uk.khall.ui.display.ImageDisplayer;
import uk.khall.utils.CheckXmpExifData;
import uk.khall.utils.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ViewOpenImageClass implements ViewImage {

    public void view(String imageName, String selectedClass, int imageWidth, int imageHeight, Component comp) {
        try {
            int displayImageWidth = imageWidth;
            int displayImageHeight = imageHeight;
            int rotation = CheckXmpExifData.getExifOrientationId(new File(imageName));
            BufferedImage bufferedImage = ImageIO.read(new File(imageName));
            Graphics2D graphics2D = null;
            if (rotation == 1){
                graphics2D = bufferedImage.createGraphics();
            } else if (rotation == 6 ){

                bufferedImage = ImageUtils.rotateImage(bufferedImage, 1);
                displayImageWidth = bufferedImage.getWidth();
                displayImageHeight = bufferedImage.getHeight() ;
                graphics2D = bufferedImage.createGraphics();

            } else if (rotation == 8 ){
                bufferedImage = ImageUtils.rotateImage(bufferedImage, 3);
                displayImageWidth = bufferedImage.getWidth();
                displayImageHeight = bufferedImage.getHeight() ;
                graphics2D = bufferedImage.createGraphics();

            } else {
                graphics2D = bufferedImage.createGraphics();
            }

            ArrayList<PhotoObjectProperties> properties = OpenImageJdbiUtils.getPhotoObjectProperties(imageName);
            for (PhotoObjectProperties property : properties){
                if(selectedClass.equals(property.className)) {
                    graphics2D.setPaint(Color.RED);
                    graphics2D.setStroke(new BasicStroke(10));
                    if (rotation == 1) {
                        graphics2D.drawRect(property.pointX1, property.pointY1,
                                property.pointX2 - property.pointX1,
                                property.pointY2 - property.pointY1);
                        graphics2D.setPaint(Color.CYAN);
                        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 24);
                        graphics2D.setFont(font);
                        graphics2D.drawString(property.className + " " + property.classScore, property.pointX1, (property.pointY1 > 5 ? property.pointY1 - 5 : property.pointY1 + 10));
                    } else if (rotation == 6 ){
                        System.out.println(property + ":" + imageWidth + ": " + imageHeight + ": " + displayImageWidth+ ": " + displayImageHeight);
                        graphics2D.drawRect(displayImageWidth - (property.pointY1 + (property.pointY2 - property.pointY1)) , property.pointX1,
                                property.pointY2 - property.pointY1,
                                property.pointX2 - property.pointX1);
                        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 24);
                        graphics2D.setPaint(Color.CYAN);
                        graphics2D.setFont(font);
                        graphics2D.drawString(property.className + " " + property.classScore, displayImageWidth - (property.pointY1 + (property.pointY2 - property.pointY1)), (property.pointX1 > 5 ? property.pointX1 - 5 : property.pointX1 + 10));
                    } else if (rotation == 8 ){
                        graphics2D.drawRect(property.pointY1, property.pointX1,
                                property.pointY2 - property.pointY1,
                                property.pointX2 - property.pointX1);
                        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 24);
                        graphics2D.setPaint(Color.CYAN);
                        graphics2D.setFont(font);
                        graphics2D.drawString(property.className + " " + property.classScore, property.pointY1, (property.pointX1 > 5 ? property.pointX1 - 5 : property.pointX1 + 10));
                    } else {
                        graphics2D.drawRect(property.pointX1, property.pointY1,
                                property.pointX2 - property.pointX1,
                                property.pointY2 - property.pointY1);
                        graphics2D.setPaint(Color.CYAN);
                        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 24);
                        graphics2D.setFont(font);
                        graphics2D.drawString(property.className + " " + property.classScore, property.pointX1, (property.pointY1 > 5 ? property.pointY1 - 5 : property.pointY1 + 10));
                    }

                }
            }
            BufferedImage resizedImage = ImageUtils.resizeImage(bufferedImage, imageHeight, imageWidth, comp);
            ImageDisplayer imageDisplayer = new ImageDisplayer(resizedImage.getWidth(), resizedImage.getHeight());
            imageDisplayer.setImage(resizedImage);
            imageDisplayer.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
