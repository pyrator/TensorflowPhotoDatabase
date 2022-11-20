package uk.khall.ui.interact;

import uk.khall.sql.CocoSqlUtils;
import uk.khall.ui.display.ImageDisplayer;
import uk.khall.utils.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ViewRcnnImageClass implements ViewImage {

    public void view(String imageName, String selectedClass, int imageWidth, int imageHeight, Component comp) {

        try {
            BufferedImage bufferedImage = ImageIO.read(new File(imageName));
            Graphics2D graphics2D = bufferedImage.createGraphics();

            ArrayList<PhotoObjectProperties> properties = CocoSqlUtils.getPhotoObjectProperties(imageName);
            for (PhotoObjectProperties property : properties){
                if(selectedClass.equals(property.className)) {
                    graphics2D.setPaint(Color.RED);
                    graphics2D.setStroke(new BasicStroke(10));
                    graphics2D.drawRect(property.pointX1, property.pointY1,
                            property.pointX2 - property.pointX1,
                            property.pointY2 - property.pointY1);
                    graphics2D.setPaint(Color.CYAN);
                    Font font = new Font(Font.SANS_SERIF, Font.BOLD, 24);
                    graphics2D.setFont(font);
                    graphics2D.drawString(property.className + " " + property.classScore, property.pointX1, (property.pointY1 > 5 ? property.pointY1 - 5 : property.pointY1 + 10));
                }
            }
            BufferedImage resizedImage = ImageUtils.resizeImage(bufferedImage, imageWidth, imageHeight, comp);
            ImageDisplayer imageDisplayer = new ImageDisplayer(resizedImage.getWidth(), resizedImage.getHeight());
            imageDisplayer.setImage(resizedImage);
            imageDisplayer.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
