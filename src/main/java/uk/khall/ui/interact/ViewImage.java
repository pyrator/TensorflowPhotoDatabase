package uk.khall.ui.interact;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.SQLException;

public interface ViewImage {
    public void view(String imageName, String className,  int imageWidth, int imageHeight, Component comp) throws SQLException;

}
