package uk.khall.utils;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;

import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;

import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;


import java.io.*;

import java.util.List;


/**
 * Created by Keith on 09/07/2016.
 */
public class CheckXmpExifData {
    /*
        https://google-developers.appspot.com/streetview/spherical-metadata

    •-xmp:ProjectionType=equirectangular
    •-xmp:UsePanoramaViewer=True
    •-xmp:CroppedAreaImageWidthPixels=6000
    •-xmp:CroppedAreaImageHeightPixels=3000
    •-xmp:FullPanoWidthPixels=6000
    •-xmp:FullPanoHeightPixels=3000
    •-xmp:CroppedAreaLeftPixels=0
    •-xmp:CroppedAreaTopPixels=0
    •-xmp:PoseHeadingDegrees=0.0
    •-xmp:InitialViewHeadingDegrees=0
    •-xmp:InitialViewPitchDegrees=0
    •-xmp:InitialViewRollDegrees=0
    •-xmp:InitialHorizontalFOVDegrees=90.0

     */
    public void viewExifMetadata(final File jpegImageFile)
            throws IOException, ImageReadException, ImageWriteException {
        OutputStream os = null;
        boolean canThrow = false;
        TiffOutputSet outputSet = null;

        // note that metadata might be null if no metadata is found.
        final String adobeMetaData = Imaging.getXmpXml(jpegImageFile);
        System.out.println(adobeMetaData);

        final ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
        final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
        if (null != jpegMetadata) {
            // note that exif might be null if no Exif metadata is found.
            final TiffImageMetadata exif = jpegMetadata.getExif();

            if (null != exif) {
                // TiffImageMetadata class is immutable (read-only).
                // TiffOutputSet class represents the Exif data to write.
                //
                // Usually, we want to update existing Exif metadata by
                // changing
                // the values of a few fields, or adding a field.
                // In these cases, it is easiest to use getOutputSet() to
                // start with a "copy" of the fields read from the image.
                outputSet = exif.getOutputSet();
            }
        }

        // if file does not contain any exif metadata, we create an empty
        // set of exif metadata. Otherwise, we keep all of the other
        // existing tags.
        if (null == outputSet) {
            outputSet = new TiffOutputSet();
        }

        {
            // Example of how to add a field/tag to the output set.
            //
            // Note that you should first remove the field/tag if it already
            // exists in this directory, or you may end up with duplicate
            // tags. See above.
            //
            // Certain fields/tags are expected in certain Exif directories;
            // Others can occur in more than one directory (and often have a
            // different meaning in different directories).
            //
            // TagInfo constants often contain a description of what
            // directories are associated with a given tag.
            //
            final TiffOutputDirectory exifDirectory = outputSet
                    .getOrCreateExifDirectory();
            // make sure to remove old value if present (this method will
            // not fail if the tag does not exist).
            // <b>get all metadata stored in EXIF format (ie. from JPEG or
            // TIFF). </b>
            final ImageMetadata metadata2 = Imaging.getMetadata(jpegImageFile);

            // <b>print a dump of information about an image to stdout. </b>
            Imaging.dumpImageFile(jpegImageFile);

            List<TiffOutputField> fields= exifDirectory.getFields();
            for (TiffOutputField field: fields){

                System.out.println("fields " + field.fieldType + " " + field.tag + " " + field.tagInfo.name + " " + field.tagInfo.getDescription());
            }
            //exifDirectory
            //        .removeField(ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
            //exifDirectory.add(ExifTagConstants.EXIF_TAG_APERTURE_VALUE,
            //       new RationalNumber(3, 10));
        }


    }






    public static void main(String[] params){
        CheckXmpExifData addXmpExifData = new CheckXmpExifData();

        try {
            addXmpExifData.viewExifMetadata(new File("D:\\Users\\theke\\flashair\\upload\\P1300415.JPG"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ImageReadException e) {
            e.printStackTrace();
        } catch (ImageWriteException e) {
            e.printStackTrace();
        }

    }

}
