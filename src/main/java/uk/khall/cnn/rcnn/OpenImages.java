package uk.khall.cnn.rcnn;


import ch.qos.logback.classic.Logger;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.LoggerFactory;

import org.tensorflow.op.image.NonMaxSuppression;
import org.tensorflow.op.image.ResizeBilinear;
import uk.khall.beans.ImageBean;
import uk.khall.beans.ObjectBean;
import uk.khall.imagenet.OpenImagesClasses;
import uk.khall.sql.JdbiBridge;
import uk.khall.sql.OpenImagesJdbiInterface;
import uk.khall.sql.SqlLiteBridge;
import org.tensorflow.Graph;
import org.tensorflow.Operand;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.op.Ops;
import org.tensorflow.op.core.Constant;
import org.tensorflow.op.core.Reshape;

import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.op.io.ReadFile;
import org.tensorflow.op.math.Div;
import org.tensorflow.proto.framework.ConfigProto;
import org.tensorflow.proto.framework.GPUOptions;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.TInt64;
import org.tensorflow.types.TString;
import org.tensorflow.types.TUint8;
import uk.khall.ui.GUIThread;
import uk.khall.utils.ModelHubUtils;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ]
 * Detector using the https://tfhub.dev/google/faster_rcnn/openimages_v4/nception_resnet_v2 hub module
 */
public class OpenImages implements Detector {
    private static boolean resize = false;
    Logger stdOutLogger;
    SavedModelBundle model;
    private OpenImagesClasses imageNetClass;
    private TreeMap<Float, String> imageNetTreeMap;
    private Connection connection;
    private static final long nullId = -99999;
    private static final float detectionCutOff = 0.30f;
    private static final String modelFolder = "models";
    private static final String urlStart = "https://tfhub.dev/google/faster_rcnn/openimages_v4";
    private static final String modelName = "inception_resnet_v2";
    private static final String version = "1";

    public OpenImages() {
        stdOutLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("uk.khall.cnn.rcnn.OpenImages");
        stdOutLogger.debug("Testing std out logger");
        //https://tfhub.dev/google/faster_rcnn/openimages_v4/inception_resnet_v2/1
        // get path to model folder (currently in resources
        String modelPath = modelFolder + "/" + modelName;
        try {
            if (!new File(modelPath).exists()) {
                ModelHubUtils.extractTarGZ(urlStart, modelName, modelFolder, version);
            }
        } catch (IOException e) {
            stdOutLogger.error(e.getMessage());
        }
        // load saved model
        model = loadModel(modelPath, new String[0]);
        model.session().run("hub_input/index_to_string_1/table_init");
        model.session().run("hub_input/index_to_string/table_init");
        imageNetClass = new OpenImagesClasses();
        imageNetTreeMap = imageNetClass.getOpenImageTreeMap();
    }

    /**
     * load the model intp memory.
     *
     * @param dir  directory of the model.
     * @param tags declared tags.
     * @return a SavedModelBundle.
     */
    private SavedModelBundle loadModel(String dir, String[] tags) {
        //Set gpu memory growth to 80%
        GPUOptions gpu = ConfigProto.getDefaultInstance().getGpuOptions().toBuilder() //
                .setPerProcessGpuMemoryFraction(0.8) //
                .setAllowGrowth(true) //
                .build(); //

        ConfigProto configProto = ConfigProto.newBuilder(ConfigProto.getDefaultInstance()) //
                .setLogDevicePlacement(true) //
                .mergeGpuOptions(gpu) //
                .build(); //

        SavedModelBundle.Loader loader = SavedModelBundle.loader(dir);
        try {
            Field field = SavedModelBundle.Loader.class.getDeclaredField("tags");
            field.setAccessible(true);
            field.set(loader, tags);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return loader.withConfigProto(configProto).load();
    }

    /**
     * Set connection to the database.
     *
     * @param connection database conection.
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Do object detection on image.
     *
     * @param imagePath path to image.
     */
    public void doObjectDetection(String imagePath, boolean NMS) {

        try (Graph g = new Graph(); Session s = new Session(g)) {
            Ops tf = Ops.create(g);

            Constant<TString> fileName = tf.constant(imagePath);
            ReadFile readFile = tf.io.readFile(fileName);
            Session.Runner runner = s.runner();
            DecodeJpeg.Options options = DecodeJpeg.channels(3L);
            DecodeJpeg decodeImage = tf.image.decodeJpeg(readFile.contents(), options);
            //fetch image from file
            try (TUint8 outputImage = (TUint8) runner.fetch(decodeImage).run().get(0)) {
                Map<String, Tensor> feedDict = new HashMap<>();
                Shape imageShape = outputImage.shape();
                long[] shapeArray = imageShape.asArray();
                Reshape<TFloat32> reshape = tf.reshape(tf.math.div(
                        tf.dtypes.cast(tf.constant(outputImage), TFloat32.class),
                        tf.constant(255.0f)
                        ),
                        tf.array(1,
                                shapeArray[0],
                                shapeArray[1],
                                shapeArray[2]
                        )
                );

                try (TFloat32 photoTFloat32Tensor = (TFloat32) s.runner().fetch(reshape).run().get(0)) {
                    feedDict.put("images", photoTFloat32Tensor);
                    File imageFile = new File(imagePath);
                    try {
                        long imageId = insertImageIntoDatabase(imageFile.getCanonicalPath(), shapeArray);
                        if (imageId != nullId) {
                            Map<String, Tensor> outputTensorMap = model.function("default").call(feedDict);

                            //detection_classes, detectionBoxes model output names

                            try (TString detectionClasses = (TString) outputTensorMap.get("detection_class_entities");
                                 TFloat32 detectionBoxes = (TFloat32) outputTensorMap.get("detection_boxes");
                                 TInt64 detectionClassLabels = (TInt64) outputTensorMap.get("detection_class_labels");
                                 TString detectionClassNames = (TString) outputTensorMap.get("detection_class_names");
                                 TFloat32 detectionScores = (TFloat32) outputTensorMap.get("detection_scores")
                            ) {
                                int numDetects = (int) detectionClasses.size();
                                //Create a NonMaxSuppression
                                if (numDetects > 0) {
                                    if (NMS) {
                                        NonMaxSuppression.Options padToMaxOutputSize = NonMaxSuppression.padToMaxOutputSize(false);
                                        NonMaxSuppression nonMaxSuppression = tf.image.nonMaxSuppression(tf.constant(detectionBoxes),
                                                tf.constant(detectionScores), tf.constant(10),
                                                tf.constant(0.5f), tf.constant(0.5f), tf.constant(0.0f),
                                                padToMaxOutputSize);

                                        try (TInt32 selectedIndices = (TInt32) s.runner().fetch(nonMaxSuppression.selectedIndices()).run().get(0);
                                             TFloat32 selectedScores = (TFloat32) s.runner().fetch(nonMaxSuppression.selectedScores()).run().get(0);
                                             TInt32 validDetections = (TInt32) s.runner().fetch(nonMaxSuppression.validOutputs()).run().get(0)) {

                                            for (int n = 0; n < selectedIndices.size(); n++) {
                                                Integer selectedIndex = selectedIndices.getInt(n);
                                                float detectionScore = selectedScores.getFloat(n);
                                                Long classLabel = detectionClassLabels.getLong(selectedIndex);
                                                //only include those classes with detection score greater than 0.3f
                                                if (detectionScore > detectionCutOff) {
                                                    FloatNdArray detectionBox = detectionBoxes.get(selectedIndices.getInt(n));
                                                    //add class name to temp bufferedimage
                                                    insertClassesIntoDatabase(imageId, classLabel, detectionBox, detectionScore);
                                                }
                                            }
                                        }

                                    } else {
                                        for (int n = 0; n < numDetects; n++) {
                                            Long classLabel = detectionClassLabels.getLong(n);
                                            //System.out.println(openImagesTreeMap.get(classLabel));
                                            //put probability and position in outputMap
                                            float detectionScore = detectionScores.getFloat(n);
                                            //only include those classes with detection score greater than detectionCutOff
                                            if (detectionScore > detectionCutOff) {
                                                FloatNdArray detectionBox = detectionBoxes.get(n);
                                                //add class name to temp bufferedimage
                                                insertClassesIntoDatabase(imageId, classLabel, detectionBox, detectionScore);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        stdOutLogger.error(e.getMessage());
                    }
                }
            }
        }

    }

    /**
     * Resize object before detection.
     *
     * @param imagePath path to image.
     */
    public void doResizedObjectDetection(String imagePath, boolean NMS) {
        doResizedObjectDetection(imagePath, imgRcnnSize, imgRcnnSize, NMS);
    }

    /**
     * Resize object before detection.
     *
     * @param imagePath path to image.
     * @param newHeight new height of object.
     * @param newWidth  new width of object.
     */
    public void doResizedObjectDetection(String imagePath, int newHeight, int newWidth, boolean NMS) {

        try (Graph g = new Graph(); Session s = new Session(g)) {

            Ops tf = Ops.create(g);
            Constant<TString> fileName = tf.constant(imagePath);
            ReadFile readFile = tf.io.readFile(fileName);

            Session.Runner runner = s.runner();
            DecodeJpeg.Options options = DecodeJpeg.channels(3L);
            DecodeJpeg decodeImage = tf.image.decodeJpeg(readFile.contents(), options);
            //fetch image from file
            try (TUint8 outputImage = (TUint8) runner.fetch(decodeImage).run().get(0)) {
                Map<String, Tensor> feedDict = new HashMap<>();
                Shape imageShape = outputImage.shape();
                long[] shapeArray = imageShape.asArray();
                //create a 4D tensor of shape `[num_boxes, crop_height, crop_width, depth]`
                try (TUint8 reshapedOutput = reshapeTensor(outputImage)) {
                    //resize image to 299 x 299 assuming whole of original image
                    Operand<TUint8> inputImage = tf.constant(reshapedOutput);
                    ResizeBilinear.Options halfPixelCenters = ResizeBilinear.halfPixelCenters(true);
                    Operand<TInt32> reSize = tf.constant(new int[]{newHeight, newWidth});
                    ResizeBilinear resizeBilinear = tf.image.resizeBilinear(inputImage,reSize,halfPixelCenters );
                    try (TFloat32 cropVal = (TFloat32) s.runner().fetch(resizeBilinear).run().get(0)) {
                        //The given SavedModel SignatureDef input
                        Div<TFloat32> divPhoto = tf.math.div(
                                tf.constant(cropVal),
                                tf.constant(255.0f)
                        );
                        try (TFloat32 photoTFloat32Tensor = (TFloat32) s.runner().fetch(divPhoto).run().get(0)) {
                            feedDict.put("images", photoTFloat32Tensor);
                            File imageFile = new File(imagePath);
                            try {
                                long imageId = insertImageIntoDatabase(imageFile.getCanonicalPath(), shapeArray);
                                if (imageId != nullId) {
                                    Map<String, Tensor> outputTensorMap = model.function("default").call(feedDict);

                                    //detection_class_entities, detection_boxes model output names
                                    try (TString detectionClasses = (TString) outputTensorMap.get("detection_class_entities");
                                         TFloat32 detectionBoxes = (TFloat32) outputTensorMap.get("detection_boxes");
                                         TInt64 detectionClassLabels = (TInt64) outputTensorMap.get("detection_class_labels");
                                         TString detectionClassNames = (TString) outputTensorMap.get("detection_class_names");
                                         TFloat32 detectionScores = (TFloat32) outputTensorMap.get("detection_scores")
                                    ) {
                                        int numDetects = (int) detectionClasses.size();
                                        //Create a NonMaxSuppression
                                        if (numDetects > 0) {
                                            if (NMS) {
                                                NonMaxSuppression.Options padToMaxOutputSize = NonMaxSuppression.padToMaxOutputSize(false);
                                                NonMaxSuppression nonMaxSuppression = tf.image.nonMaxSuppression(tf.constant(detectionBoxes),
                                                        tf.constant(detectionScores), tf.constant(10),
                                                        tf.constant(0.5f), tf.constant(0.5f), tf.constant(0.0f),
                                                        padToMaxOutputSize);

                                                try (TInt32 selectedIndices = (TInt32) s.runner().fetch(nonMaxSuppression.selectedIndices()).run().get(0);
                                                     TFloat32 selectedScores = (TFloat32) s.runner().fetch(nonMaxSuppression.selectedScores()).run().get(0);
                                                     TInt32 validDetections = (TInt32) s.runner().fetch(nonMaxSuppression.validOutputs()).run().get(0)) {
                                                    //Gather??
                                                    for (int n = 0; n < selectedIndices.size(); n++) {
                                                        Integer selectedIndex = selectedIndices.getInt(n);
                                                        float detectionScore = selectedScores.getFloat(n);
                                                        Long classLabel = detectionClassLabels.getLong(selectedIndex);
                                                        //only include those classes with detection score greater than 0.3f
                                                        if (detectionScore > detectionCutOff) {
                                                            FloatNdArray detectionBox = detectionBoxes.get(selectedIndices.getInt(n));
                                                            //add class name to temp bufferedimage
                                                            insertClassesIntoDatabase(imageId, classLabel, detectionBox, detectionScore);
                                                        }
                                                    }
                                                }
                                            } else {
                                                for (int n = 0; n < numDetects; n++) {
                                                    Long classLabel = detectionClassLabels.getLong(n);
                                                    //System.out.println(openImagesTreeMap.get(classLabel));
                                                    //put probability and position in outputMap
                                                    float detectionScore = detectionScores.getFloat(n);
                                                    //only include those classes with detection score greater than 0.3f
                                                    if (detectionScore > 0.3f) {
                                                        FloatNdArray detectionBox = detectionBoxes.get(n);
                                                        //add class name to temp bufferedimage
                                                        insertClassesIntoDatabase(imageId, classLabel, detectionBox, detectionScore);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                stdOutLogger.error(e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Insert image details into the database
     *
     * @param imageName  full name and path to image
     * @param shapeArray width and height
     * @return the new id
     */
    public long insertImageIntoDatabase(String imageName, long[] shapeArray) {
        Jdbi jdbi = JdbiBridge.createSqliteJdbiConnection("openimagephotoobjects.db");
        jdbi.installPlugin(new SqlObjectPlugin());
        ImageBean imageBean = new ImageBean();
        imageBean.setImageName(imageName);
        imageBean.setWidth(shapeArray[1]);
        imageBean.setHeight(shapeArray[0]);
        long imageId = nullId;
        try {
            OpenImagesJdbiInterface jdbiInterface = jdbi.onDemand(OpenImagesJdbiInterface.class);
            imageId = jdbiInterface.insertImage(imageBean);
        } catch (Exception e){
            stdOutLogger.error(e.getMessage());
        }

        return imageId;
    }

    /**
     * Insert object detection details into database
     *
     * @param imageId      id of the image
     * @param classId      class id of the image
     * @param detectionBox the detection box
     * @param score        associated detection score
     */
    public void insertClassesIntoDatabase(long imageId, float classId, FloatNdArray detectionBox, float score) {
        Jdbi jdbi = JdbiBridge.createSqliteJdbiConnection("openimagephotoobjects.db");

        jdbi.installPlugin(new SqlObjectPlugin());
        ObjectBean objectBean = new ObjectBean();
        objectBean.setClassId(classId);
        objectBean.setImageId(imageId);
        objectBean.setX1(detectionBox.getFloat(1));
        objectBean.setY1(detectionBox.getFloat(0));
        objectBean.setX2(detectionBox.getFloat(3));
        objectBean.setY2(detectionBox.getFloat(2));
        objectBean.setScore(score);
        OpenImagesJdbiInterface jdbiInterface = jdbi.onDemand(OpenImagesJdbiInterface.class);
        jdbiInterface.insertObject(objectBean);
    }

    /**
     * main class
     *
     * @param params
     * @throws SQLException
     * @throws IOException
     */
    public static void main(String[] params) throws SQLException, IOException {
        String selDir = System.getProperty("user.dir") + File.separator;
        GUIThread guiThread = new GUIThread("open", selDir);
        try {
            SwingUtilities.invokeAndWait(guiThread);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        String dirName = guiThread.getFilePath() + File.separator;
        //System.out.println(dirName);

        OpenImages lfr = new OpenImages();
        boolean doResize = false;
        try (Connection connection = SqlLiteBridge.createSqliteConnection("openimagephotoobjects.db");
             Stream<Path> stream = Files.walk(Paths.get(dirName), 1)
        ) {
            lfr.setConnection(connection);
            Set<String> fileSet = stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
            for (String imagePath : fileSet) {
                if (imagePath.toLowerCase().endsWith(".jpg")) {
                    if (doResize)
                        lfr.doResizedObjectDetection(dirName + imagePath, 1024, 1024, false);
                    else
                        lfr.doObjectDetection(dirName + imagePath, false);
                }
            }
        }
        System.exit(0);
    }
}
