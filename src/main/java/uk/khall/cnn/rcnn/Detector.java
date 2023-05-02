package uk.khall.cnn.rcnn;

import org.tensorflow.Graph;
import org.tensorflow.Operand;
import org.tensorflow.Session;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.op.Ops;
import org.tensorflow.op.image.CropAndResize;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.TUint8;

import java.sql.Connection;

public interface Detector {
    static final int imgSize = 299;
    static final int imgRcnnSize = 1024;

    public void setConnection(Connection connection);
    public void doObjectDetection(String imagePath, boolean NMS);
    public void doResizedObjectDetection(String imagePath, boolean NMS);
    public void doResizedObjectDetection(String imagePath, int newHeight, int newWidth, boolean NMS);
    /**
     * return a 4D tensor from 3D tensor
     *
     * @param tUint8Tensor 3D tensor
     * @return 4D tensor
     */
    default TUint8 reshapeTensor(TUint8 tUint8Tensor) {
        Ops tf = Ops.create();
        return tf.reshape(tf.constant(tUint8Tensor),
                tf.array(1,
                        tUint8Tensor.shape().asArray()[0],
                        tUint8Tensor.shape().asArray()[1],
                        tUint8Tensor.shape().asArray()[2]
                )
        ).asTensor();
    }


    /**
     * Divide tensor by 255
     *
     * @param tFloat32Tensor original tensor with values 0 - 255
     * @return tensor with values 0 - 1
     */
    default TFloat32 preprocessImagesNoReshape(TFloat32 tFloat32Tensor) {
        Ops tf = Ops.create();
        return tf.math.div(
                tf.constant(tFloat32Tensor),
                tf.constant(255.0f)
        ).asTensor();
    }

    default TUint8 castToInt(TFloat32 tFloat32Array) {
        Ops tf = Ops.create();
        return tf.dtypes.cast(tf.constant(tFloat32Array), TUint8.class).asTensor();
    }

    default TFloat32 getSubImage(TUint8 outputImage, FloatNdArray detectionBox) {
        try (Graph g = new Graph(); Session s = new Session(g)) {
            Ops tf = Ops.create(g);
            Session.Runner runner = s.runner();

            //create a 4D tensor of shape `[num_boxes, crop_height, crop_width, depth]`
            //resize image to 299 x 299 assuming whole of original image
            Operand<TUint8> inputImage = tf.constant(outputImage);// tf.constant(reshapedOutput);
            //2D tensor of shape `[num_boxes, 4]` - This uses subImage
            Operand<TFloat32> boxes = tf.constant(new float[][]{{
                    detectionBox.getFloat(0),
                    detectionBox.getFloat(1),
                    detectionBox.getFloat(2),
                    detectionBox.getFloat(3)}});
            //A A 1-D tensor of shape `[num_boxes]`
            Operand<TInt32> boxInd = tf.constant(new int[]{0});
            //A 1-D tensor of 2 elements, `size = [crop_height, crop_width]`
            // Its possible to create a tensor like this
                /*
                TInt32 cropTensor = TInt32.vectorOf(imgSize, imgSize);
                Operand<TInt32> cropSize = tf.constant(cropTensor);
                */
            //or use an array
            Operand<TInt32> cropSize = tf.constant(new int[]{imgSize, imgSize});
            //CropAndResize options - default is bilinear anyway
            CropAndResize.Options cropOptions = CropAndResize.method("bilinear");
            CropAndResize cropAndResizeImage = tf.image.cropAndResize(inputImage, boxes, boxInd, cropSize,
                    cropOptions);
            //looks like CropAndResize casts image to a TFloat32
            TFloat32 cropVal = (TFloat32) runner.fetch(cropAndResizeImage).run().get(0);
            TFloat32 resizeTensor = preprocessImagesNoReshape(cropVal);

            return resizeTensor;
        }
    }
    long insertImageIntoDatabase(String imageName, long[] shapeArray);
    void insertClassesIntoDatabase(long imageId, float classId, FloatNdArray detectionBox, float score);
}
