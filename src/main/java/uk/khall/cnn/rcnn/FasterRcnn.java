package uk.khall.cnn.rcnn;

import org.tensorflow.op.core.ExpandDims;
import org.tensorflow.op.image.CombinedNonMaxSuppression;
import uk.khall.coco.CocoClasses;
import uk.khall.sql.SqlLiteBridge;
import org.tensorflow.*;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.op.Ops;
import org.tensorflow.op.core.Constant;
import org.tensorflow.op.core.Reshape;
import org.tensorflow.op.dtypes.Cast;
import org.tensorflow.op.image.CropAndResize;
import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.op.io.ReadFile;
import org.tensorflow.proto.framework.ConfigProto;
import org.tensorflow.proto.framework.GPUOptions;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.TString;
import org.tensorflow.types.TUint8;
import uk.khall.ui.GUIThread;
import uk.khall.utils.ModelHubUtils;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Object detection class utilizing inception_resnet_v2_1024x1024 model
 */
public class FasterRcnn implements Detector {
    private static boolean resize = false;

    SavedModelBundle model;
    private CocoClasses cocoClasses;
    private TreeMap<Float, String> cocoTreeMap;
    private Connection connection;
    private static final long nullId = -99999;
    private static final float detectionCutOff = 0.30f;
    private static final String modelFolder = "models";
    private static final String urlStart = "https://tfhub.dev/tensorflow/faster_rcnn";
    private static final String modelName = "inception_resnet_v2_1024x1024";
    private static final String version = "1";

    /**]
     * Standard FasterRCNN constructor
     */
    public FasterRcnn() {

        String modelPath = modelFolder+"/"+modelName;
        try {
            if (!new File(modelPath).exists()){
                ModelHubUtils.extractTarGZ(urlStart,modelName,modelFolder,version);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // get path to model folder (currently in resources
        //String modelPath = "models/inception_resnet_v2_1024x1024";
        // load saved model with 80% growth allocated to GPU
        GPUOptions gpu = ConfigProto.getDefaultInstance().getGpuOptions().toBuilder() //
                .setPerProcessGpuMemoryFraction(0.8) //
                .setAllowGrowth(true) //
                .build(); //

        ConfigProto configProto = ConfigProto.newBuilder(ConfigProto.getDefaultInstance()) //
                .setLogDevicePlacement(true) //
                .mergeGpuOptions(gpu) //
                .build(); //
        model = SavedModelBundle.loader(modelPath).withTags("serve").withConfigProto(configProto).load();
        //model = SavedModelBundle.load(modelPath, "serve");
        cocoClasses = new CocoClasses();
        cocoTreeMap = cocoClasses.getCocoTreeMap();
    }

    /**
     * Set connection to database
     * @param connection the jdbc connection
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Run standard detection
     * @param imagePath Path to image
     * @param NMS whether to use NMS
     */
    public void doObjectDetection(String imagePath, boolean NMS) {

        try (Graph g = new Graph(); Session s = new Session(g)) {
            Ops tf = Ops.create(g);
            Constant<TString> fileName = tf.constant(imagePath);
            //Constant<TString> fileName = tf.constant(LoadBirdSearchModel.class.getClassLoader().
            // getResource(imagePath).getPath());
            ReadFile readFile = tf.io.readFile(fileName);
            Session.Runner runner = s.runner();
            DecodeJpeg.Options options = DecodeJpeg.channels(3L);
            DecodeJpeg decodeImage = tf.image.decodeJpeg(readFile.contents(), options);
            //fetch image from file
            try (TUint8 outputImage = (TUint8) runner.fetch(decodeImage).run().get(0)) {
                Map<String, Tensor> feedDict = new HashMap<>();
                Shape imageShape = outputImage.shape();
                long[] shapeArray = imageShape.asArray();
                //The given SavedModel MetaGraphDef key
                Reshape<TUint8> reshape = tf.reshape(tf.constant(outputImage),
                        tf.array(1,
                                shapeArray[0],
                                shapeArray[1],
                                shapeArray[2]
                        )
                );
                try (TUint8 photoTUintTensor = (TUint8) s.runner().fetch(reshape).run().get(0)) {
                    feedDict.put("input_tensor", photoTUintTensor);
                    File imageFile = new File(imagePath);
                    try {
                        long imageId = insertImageIntoDatabase(imageFile.getCanonicalPath(), shapeArray);
                        if (imageId != nullId) {
                            Map<String, Tensor> outputTensorMap = model.function("serving_default").call(feedDict);
                            //detection_classes, detectionBoxes model output names
                            try (TFloat32 detectionClasses = (TFloat32) outputTensorMap.get("detection_classes");
                                 TFloat32 detectionBoxes = (TFloat32) outputTensorMap.get("detection_boxes");
                                 TFloat32 rawDetectionBoxes = (TFloat32) outputTensorMap.get("raw_detection_boxes");
                                 TFloat32 numDetections = (TFloat32) outputTensorMap.get("num_detections");
                                 TFloat32 detectionScores = (TFloat32) outputTensorMap.get("detection_scores");
                                 TFloat32 rawDetectionScores = (TFloat32) outputTensorMap.get("raw_detection_scores");
                                 TFloat32 detectionAnchorIndices = (TFloat32) outputTensorMap.get("detection_anchor_indices");
                                 TFloat32 detectionMulticlassScores = (TFloat32) outputTensorMap.get("detection_multiclass_scores")) {
                                int numDetects = (int) numDetections.getFloat(0);//System.out.println("numDetections  " + );

                                if (numDetects > 0) {
                                    if (NMS) {
                                        ExpandDims<TFloat32> reshapeBoxes = tf.expandDims(tf.constant(detectionBoxes), tf.constant(0));
                                        ExpandDims<TFloat32> reshapeScore = tf.expandDims(tf.constant(detectionScores), tf.constant(0));
                                        try (TFloat32 reshapeDetectBoxes = (TFloat32) s.runner().fetch(reshapeBoxes).run().get(0);
                                             TFloat32 reshapeDetectScores = (TFloat32) s.runner().fetch(reshapeScore).run().get(0)) {
                                            CombinedNonMaxSuppression.Options clipBoxes = CombinedNonMaxSuppression.clipBoxes(false);
                                            CombinedNonMaxSuppression combinedNonMaxSuppression = tf.image.combinedNonMaxSuppression(tf.constant(reshapeDetectBoxes),
                                                    tf.constant(reshapeDetectScores), tf.constant(2), tf.constant(10),
                                                    tf.constant(0.5f), tf.constant(0.5f),
                                                    clipBoxes);
                                            //Create a combinedNonMaxSuppression
                                            try (TFloat32 nmsedBoxes = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedBoxes()).run().get(0);
                                                 TFloat32 nmsedScores = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedScores()).run().get(0);
                                                 TFloat32 nmsedClasses = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedClasses()).run().get(0);
                                                 TInt32 validDetections = (TInt32) s.runner().fetch(combinedNonMaxSuppression.validDetections()).run().get(0)) {
                                                for (int n = 0; n < validDetections.getInt(0); n++) {
                                                    //put probability and position in outputMap
                                                    float detectionScore = nmsedScores.getFloat(0, n);
                                                    float classVal = detectionClasses.getFloat(0, (int) nmsedClasses.getFloat(0, n));
                                                    if (detectionScore > detectionCutOff) {
                                                        FloatNdArray detectionBox = nmsedBoxes.get(0, n);
                                                        insertClassesIntoDatabase(imageId, classVal, detectionBox, detectionScore);
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        for (int n = 0; n < numDetects; n++) {
                                            //put probability and position in outputMap
                                            float detectionScore = detectionScores.getFloat(0, n);
                                            float classVal = detectionClasses.getFloat(0, n);
                                            if (detectionScore > detectionCutOff) {
                                                FloatNdArray detectionBox = detectionBoxes.get(0, n);
                                                insertClassesIntoDatabase(imageId, classVal, detectionBox, detectionScore);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Do detection on resized images
     * @param imagePath
     * @param NMS
     */
    public void doResizedObjectDetection(String imagePath, boolean NMS) {
        doResizedObjectDetection(imagePath, imgRcnnSize, imgRcnnSize, NMS);
    }
    /**
     * Do detection on resized images
     * @param imagePath
     * @param NMS
     */
    public void doResizedObjectDetection(String imagePath, int newHeight, int newWidth, boolean NMS) {
        try (Graph g = new Graph(); Session s = new Session(g)) {

            Ops tf = Ops.create(g);
            Constant<TString> fileName = tf.constant(imagePath);
            //Constant<TString> fileName = tf.constant(LoadBirdSearchModel.class.getClassLoader().
            // getResource(imagePath).getPath());
            ReadFile readFile = tf.io.readFile(fileName);
            Session.Runner runner = s.runner();
            DecodeJpeg.Options options = DecodeJpeg.channels(3L);
            DecodeJpeg decodeImage = tf.image.decodeJpeg(readFile.contents(), options);
            //fetch image from file
            try (TUint8 outputImage = (TUint8) runner.fetch(decodeImage).run().get(0)) {
                Map<String, Tensor> feedDict = new HashMap<>();
                Shape imageShape = outputImage.shape();
                long[] shapeArray = imageShape.asArray();
                //int newHeight = (int) (shapeArray[0] / 4);
                //int newWidth = (int) (shapeArray[1] / 4);
                //create a 4D tensor of shape `[num_boxes, crop_height, crop_width, depth]`
                try (TUint8 reshapedOutput = reshapeTensor(outputImage)) {
                    //resize image to 299 x 299 assuming whole of original image
                    Operand<TUint8> inputImage = tf.constant(reshapedOutput);
                    //2D tensor of shape `[num_boxes, 4]` - This uses the full image
                    Operand<TFloat32> boxes = tf.constant(new float[][]{{0.0f, 0.0f, 1.0f, 1.0f}});
                    //A A 1-D tensor of shape `[num_boxes]`
                    Operand<TInt32> boxInd = tf.constant(new int[]{0});
                    //A 1-D tensor of 2 elements, `size = [crop_height, crop_width]`
                    // Its possible to create a tensor like this
                    /*
                    TInt32 cropTensor = TInt32.vectorOf(imgSize, imgSize);
                    Operand<TInt32> cropSize = tf.constant(cropTensor);
                    */
                    //or use an array
                    Operand<TInt32> cropSize = tf.constant(new int[]{newHeight, newWidth});
                    //CropAndResize options - default is bilinear anyway
                    CropAndResize.Options cropOptions = CropAndResize.method("bilinear");
                    CropAndResize cropAndResizeImage = tf.image.cropAndResize(inputImage, boxes, boxInd, cropSize,
                            cropOptions);
                    //looks like CropAndResize casts image to a TFloat32
                    try (TFloat32 cropVal = (TFloat32) s.runner().fetch(cropAndResizeImage).run().get(0)) {
                        Cast<TUint8> cast = tf.dtypes.cast(tf.math.div(
                                tf.constant(cropVal),
                                tf.constant(255.0f)
                        ), TUint8.class);
                        try (TUint8 photoTUintTensor = (TUint8) s.runner().fetch(cast).run().get(0)) {
                            //The given SavedModel SignatureDef input
                            feedDict.put("input_tensor", photoTUintTensor);
                            File imageFile = new File(imagePath);
                            try {
                                long imageId = insertImageIntoDatabase(imageFile.getCanonicalPath(), shapeArray);
                                if (imageId != nullId) {
                                    Map<String, Tensor> outputTensorMap = model.function("serving_default").call(feedDict);
                                    //detection_classes, detectionBoxes model output names
                                    try (TFloat32 detectionClasses = (TFloat32) outputTensorMap.get("detection_classes");
                                         TFloat32 detectionBoxes = (TFloat32) outputTensorMap.get("detection_boxes");
                                         TFloat32 rawDetectionBoxes = (TFloat32) outputTensorMap.get("raw_detection_boxes");
                                         TFloat32 numDetections = (TFloat32) outputTensorMap.get("num_detections");
                                         TFloat32 detectionScores = (TFloat32) outputTensorMap.get("detection_scores");
                                         TFloat32 rawDetectionScores = (TFloat32) outputTensorMap.get("raw_detection_scores");
                                         TFloat32 detectionAnchorIndices = (TFloat32) outputTensorMap.get("detection_anchor_indices");
                                         TFloat32 detectionMulticlassScores = (TFloat32) outputTensorMap.get("detection_multiclass_scores")) {
                                        int numDetects = (int) numDetections.getFloat(0);//System.out.println("numDetections  " + );
                                        if (numDetects > 0) {
                                            if (NMS) {
                                                ExpandDims<TFloat32> reshapeBoxes = tf.expandDims(tf.constant(detectionBoxes), tf.constant(0));
                                                ExpandDims<TFloat32> reshapeScore = tf.expandDims(tf.constant(detectionScores), tf.constant(0));
                                                try (TFloat32 reshapeDetectBoxes = (TFloat32) s.runner().fetch(reshapeBoxes).run().get(0);
                                                     TFloat32 reshapeDetectScores = (TFloat32) s.runner().fetch(reshapeScore).run().get(0)) {
                                                    CombinedNonMaxSuppression.Options clipBoxes = CombinedNonMaxSuppression.clipBoxes(false);
                                                    CombinedNonMaxSuppression combinedNonMaxSuppression = tf.image.combinedNonMaxSuppression(tf.constant(reshapeDetectBoxes),
                                                            tf.constant(reshapeDetectScores), tf.constant(2), tf.constant(10),
                                                            tf.constant(0.5f), tf.constant(0.5f),
                                                            clipBoxes);
                                                    //Create a combinedNonMaxSuppression
                                                    try (TFloat32 nmsedBoxes = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedBoxes()).run().get(0);
                                                         TFloat32 nmsedScores = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedScores()).run().get(0);
                                                         TFloat32 nmsedClasses = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedClasses()).run().get(0);
                                                         TInt32 validDetections = (TInt32) s.runner().fetch(combinedNonMaxSuppression.validDetections()).run().get(0)) {
                                                        for (int n = 0; n < validDetections.getInt(0); n++) {
                                                            //put probability and position in outputMap
                                                            float detectionScore = nmsedScores.getFloat(0, n);
                                                            float classVal = detectionClasses.getFloat(0, (int) nmsedClasses.getFloat(0, n));
                                                            if (detectionScore > detectionCutOff) {
                                                                FloatNdArray detectionBox = nmsedBoxes.get(0, n);
                                                                insertClassesIntoDatabase(imageId, classVal, detectionBox, detectionScore);
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                for (int n = 0; n < numDetects; n++) {
                                                    //put probability and position in outputMap
                                                    float detectionScore = detectionScores.getFloat(0, n);
                                                    float classVal = detectionClasses.getFloat(0, n);
                                                    if (detectionScore > detectionCutOff) {
                                                        FloatNdArray detectionBox = detectionBoxes.get(0, n);
                                                        insertClassesIntoDatabase(imageId, classVal, detectionBox, detectionScore);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public long insertImageIntoDatabase(String imageName, long[] shapeArray) {
        PreparedStatement pstmt = null;
        long imageId = nullId;
        try {
            pstmt = connection.prepareStatement("insert into images (imagename, width, height) values (?, ?, ?)");
            pstmt.setString(1, imageName);
            pstmt.setLong(2, shapeArray[1]);
            pstmt.setLong(3, shapeArray[0]);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                imageId = rs.getLong(1);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return imageId;
    }

    public void insertClassesIntoDatabase(long imageId, float classId, FloatNdArray detectionBox, float score) {
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement("insert into photoobjects (id, imageid, x1, y1, x2, y2, score) values (?, ?, ?, ?, ?, ?, ?)");
            pstmt.setFloat(1, classId);
            pstmt.setLong(2, imageId);
            pstmt.setFloat(3, detectionBox.getFloat(1));
            pstmt.setFloat(4, detectionBox.getFloat(0));
            pstmt.setFloat(5, detectionBox.getFloat(3));
            pstmt.setFloat(6, detectionBox.getFloat(2));
            pstmt.setFloat(7, score);
            pstmt.addBatch();
            pstmt.executeBatch();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }

    }

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
        boolean doResize = false;
        FasterRcnn lfr = new FasterRcnn();
        try (Connection connection = SqlLiteBridge.createSqliteConnection("photoobjects.db");

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
                        lfr.doResizedObjectDetection(dirName + imagePath, false);
                    else
                        lfr.doObjectDetection(dirName + imagePath, false);
                }
            }

        }
        System.exit(0);
    }
}
