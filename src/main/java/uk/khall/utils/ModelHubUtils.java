package uk.khall.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ModelHubUtils {

    public static void extractTarGZ(String urlStart, String modelName, String modelFolder, String version) throws IOException {
        int BUFFER_SIZE = 2048;
        File fm = new File(modelFolder + "/"+modelName);
        boolean modelFolderCreated = fm.mkdir();
        if (!modelFolderCreated) {
            System.out.printf("Unable to create directory '%s', during extraction of archive contents.\n",
                    fm.getAbsolutePath());
        }
        String url = urlStart + "/" +  modelName + "/"+version+"?tf-hub-format=compressed";
        InputStream in = new URL(url).openStream();
        GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
            TarArchiveEntry entry;

            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                /** If the entry is a directory, create the directory. **/
                if (entry.isDirectory()) {
                    String entryName = entry.getName();
                    if(entryName.startsWith(".")){
                        entryName = entryName.replace(".","");
                    }

                    File f = new File(modelFolder + "/"+modelName+"/"+entryName);
                    boolean created = f.mkdir();
                    //False message????
                    //if (!created) {
                    //    System.out.printf("Unable to create directory '%s', during extraction of archive contents.\n",
                    //            f.getAbsolutePath());
                    //}
                } else {
                    int count;
                    byte data[] = new byte[BUFFER_SIZE];
                    FileOutputStream fos = new FileOutputStream(modelFolder + "/"+modelName+"/"+entry.getName(), false);
                    try (BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE)) {
                        while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
                            dest.write(data, 0, count);
                        }
                    }
                }
            }

            System.out.println("Untar completed successfully!");
        }
    }
    public static void main(String[] params){
        try {
            //https://tfhub.dev/google/faster_rcnn/openimages_v4/inception_resnet_v2/1
            //https://tfhub.dev/tensorflow/faster_rcnn/inception_resnet_v2_1024x1024/1
            //https://tfhub.dev/google/imagenet/inception_v3/classification/5
            //String urlStart = "https://tfhub.dev/google/imagenet/inception_v3";
            //String modelName = "classification";
            //String modelFolder = "models";
            //String version = "5";
            //String urlStart = "https://tfhub.dev/tensorflow/faster_rcnn";
            //String modelName = "inception_resnet_v2_1024x1024";
            //String modelFolder = "models";
            //String version = "1";
            String urlStart = "https://tfhub.dev/google/faster_rcnn/openimages_v4";
            String modelName = "inception_resnet_v2";
            String modelFolder = "models";
            String version = "1";
            extractTarGZ(urlStart,modelName,modelFolder,version);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
