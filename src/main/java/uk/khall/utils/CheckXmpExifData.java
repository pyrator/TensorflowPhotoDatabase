package uk.khall.utils;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;

import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;

import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoShort;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.*;

import java.io.*;

import java.util.List;
import java.util.Objects;


/**
 * Created by Keith on 09/07/2016.
 */
public class CheckXmpExifData {

    public static void viewExifMetadata(final File jpegImageFile)
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


    /**
     * Retrieve the orientation of a file from the EXIF data.
     *
     * @param   imageFile   The image file.
     * @return  The orientation value ID.
     */
    public static int getExifOrientationId(final File imageFile) {

        try {

            final ImageMetadata tmpMetadata = Imaging.getMetadata(imageFile);
            TiffImageMetadata tmpTiffImageMetadata;

            if (tmpMetadata instanceof JpegImageMetadata) {

                tmpTiffImageMetadata = ((JpegImageMetadata) tmpMetadata).getExif();
            } else if (tmpMetadata instanceof TiffImageMetadata) {

                tmpTiffImageMetadata = (TiffImageMetadata) tmpMetadata;
            } else {

                return TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL;
            }

            TiffField tmpTiffField = tmpTiffImageMetadata.findField(TiffTagConstants.TIFF_TAG_ORIENTATION);

            if (!Objects.isNull(tmpTiffField)) {

                return tmpTiffField.getIntValue();
            } else {

                // https://www.loc.gov/preservation/digital/formats/content/tiff_tags.shtml
                TagInfo tmpTagInfo = new TagInfoShort("Orientation", 274, TiffDirectoryType.TIFF_DIRECTORY_IFD0);
                tmpTiffField = tmpTiffImageMetadata.findField(tmpTagInfo);

                if (!Objects.isNull(tmpTiffField)) {

                    return tmpTiffField.getIntValue();
                } else {

                    return TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL;
                }
            }
        } catch (Exception e) {

            return TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL;
        }
    }



public static void main (String[] params){
    System.out.println(getExifOrientationId(new File("D:\\Users\\theke\\Pictures\\Bolam Lake Sep 2020\\P1010526.JPG")));
    System.out.println(getExifOrientationId(new File("D:\\Users\\theke\\Pictures\\Bolam Lake Sep 2020\\P1010516.JPG")));
}


}
