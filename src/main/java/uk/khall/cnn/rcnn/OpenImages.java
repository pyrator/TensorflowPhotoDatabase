package uk.khall.cnn.rcnn;


import org.tensorflow.op.image.ResizeBilinear;
import uk.khall.imagenet.OpenImagesClasses;
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
import uk.khall.utils.ModelHubUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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

public class OpenImages implements Detector {
    private static boolean resize = false;

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

        //https://tfhub.dev/google/faster_rcnn/openimages_v4/inception_resnet_v2/1
        // get path to model folder (currently in resources
        String modelPath = modelFolder+"/"+modelName;
        try {
            if (!new File(modelPath).exists()){
                ModelHubUtils.extractTarGZ(urlStart,modelName,modelFolder,version);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // load saved model
        model = loadModel(modelPath, new String[0]);
        model.session().run("hub_input/index_to_string_1/table_init");
        model.session().run("hub_input/index_to_string/table_init");
        imageNetClass = new OpenImagesClasses();
        imageNetTreeMap = imageNetClass.getOpenImageTreeMap();
    }

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

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void doObjectDetection(String imagePath) {

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
                                /*      //Create a combinedNonMaxSuppression
                                        ExpandDims<TFloat32> reshapeBoxes = tf.expandDims(tf.constant(detectionBoxes), tf.constant(0));
                                        ExpandDims<TFloat32> reshapeScore = tf.expandDims(tf.constant(detectionScores), tf.constant(0));
                                        try (TFloat32 reshapeDetectBoxes = (TFloat32) s.runner().fetch(reshapeBoxes).run().get(0);
                                             TFloat32 reshapeDetectScores = (TFloat32) s.runner().fetch(reshapeScore).run().get(0)) {
                                            CombinedNonMaxSuppression.Options clipBoxes = CombinedNonMaxSuppression.clipBoxes(false);
                                            CombinedNonMaxSuppression combinedNonMaxSuppression = tf.image.combinedNonMaxSuppression(tf.constant(reshapeDetectBoxes),
                                                    tf.constant(reshapeDetectScores), tf.constant(10), tf.constant(10),
                                                    tf.constant(0.5f), tf.constant(0.5f),
                                                    clipBoxes);

                                            try (TFloat32 nmsedBoxes = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedBoxes()).run().get(0);
                                                 TFloat32 nmsedScores = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedScores()).run().get(0);
                                                 TFloat32 nmsedClasses = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedClasses()).run().get(0);
                                                 TInt32 validDetections = (TInt32) s.runner().fetch(combinedNonMaxSuppression.validDetections()).run().get(0)) {

                                            }
                                        }*/
                                for (int n = 0; n < numDetects; n++) {
                                    Long classLabel = detectionClassLabels.getLong(n);
                                    //System.out.println(openImagesTreeMap.get(classLabel));
                                    //put probability and position in outputMap
                                    float detectionScore = detectionScores.getFloat(n);
                                    //only include those classes with detection score greater than 0.3f
                                    if (detectionScore > detectionCutOff) {
                                        FloatNdArray detectionBox = detectionBoxes.get(n);
                                        //add class name to temp bufferedimage
                                        insertClassesIntoDatabase(imageId, classLabel, detectionBox, detectionScore);
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
    public void doResisizedObjectDetection(String imagePath){
        doResisizedObjectDetection(imagePath, imgRcnnSize, imgRcnnSize);
    }
    public void doResisizedObjectDetection(String imagePath, int newHeight, int newWidth) {

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
                                        /*
                    //2D tensor of shape `[num_boxes, 4]` - This uses the full image
                    Operand<TFloat32> boxes = tf.constant(new float[][]{{0.0f, 0.0f, 1.0f, 1.0f}});
                    //A A 1-D tensor of shape `[num_boxes]`
                    Operand<TInt32> boxInd = tf.constant(new int[]{0});

                    //A 1-D tensor of 2 elements, `size = [crop_height, crop_width]`
                    // Its possible to create a tensor like this

                    TInt32 cropTensor = TInt32.vectorOf(imgSize, imgSize);
                    Operand<TInt32> cropSize = tf.constant(cropTensor);

                    //or use an array
                    //Operand<TInt32> cropSize = tf.constant(new int[]{newHeight, newWidth});
                    //CropAndResize options - default is bilinear anyway
                    CropAndResize.Options cropOptions = CropAndResize.method("bilinear");
                    CropAndResize cropAndResizeImage = tf.image.cropAndResize(inputImage, boxes, boxInd, cropSize,
                            cropOptions);
                    //looks like CropAndResize casts image to a TFloat32*/
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
/*                                        //Create a combinedNonMaxSuppression
                                        ExpandDims<TFloat32> reshapeBoxes = tf.expandDims(tf.constant(detectionBoxes), tf.constant(0));
                                        ExpandDims<TFloat32> reshapeScore = tf.expandDims(tf.constant(detectionScores), tf.constant(0));
                                        try (TFloat32 reshapeDetectBoxes = (TFloat32) s.runner().fetch(reshapeBoxes).run().get(0);
                                             TFloat32 reshapeDetectScores = (TFloat32) s.runner().fetch(reshapeScore).run().get(0)) {
                                            CombinedNonMaxSuppression.Options clipBoxes = CombinedNonMaxSuppression.clipBoxes(false);
                                            CombinedNonMaxSuppression combinedNonMaxSuppression = tf.image.combinedNonMaxSuppression(tf.constant(reshapeDetectBoxes),
                                                    tf.constant(reshapeDetectScores), tf.constant(10), tf.constant(10),
                                                    tf.constant(0.5f), tf.constant(0.5f),
                                                    clipBoxes);

                                            try (TFloat32 nmsedBoxes = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedBoxes()).run().get(0);
                                                 TFloat32 nmsedScores = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedScores()).run().get(0);
                                                 TFloat32 nmsedClasses = (TFloat32) s.runner().fetch(combinedNonMaxSuppression.nmsedClasses()).run().get(0);
                                                 TInt32 validDetections = (TInt32) s.runner().fetch(combinedNonMaxSuppression.validDetections()).run().get(0)) {

                                            }
                                        }*/
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
            pstmt = connection.prepareStatement("insert into openimagephotoobjects (id, imageid, x1, y1, x2, y2, score) values (?, ?, ?, ?, ?, ?, ?)");
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
        OpenImages lfr = new OpenImages();
        boolean doResize=false;
        String dirName = "C:\\Users\\????????????";
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
                        lfr.doResisizedObjectDetection(dirName+imagePath, 1024, 1024);
                    else
                        lfr.doObjectDetection(dirName + imagePath);
                }
            }
        }
    }
}
