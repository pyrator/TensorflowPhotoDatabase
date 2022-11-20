# Photo Database using TensorFlow for Java

##Introduction

This project uses Tensorflow and various Classification models to classify photos and store results in a sqlite database.

It searches photos in a directory and adds the information to the database. The User Interface allows the use to view the photos with overlays showing positions of classes found by the Classification model.
The photographs themselves are not changed by the overlays.

The Java Tensorflow Project can be found in its own [repository](https://github.com/tensorflow/java)
Many thanks to the team there for helping me put together the initial [FasterRCNN demo](https://github.com/tensorflow/java-models/tree/master/tensorflow-examples/src/main/java/org/tensorflow/model/examples/cnn/fastrcnn) found at [Java Models Github](https://github.com/tensorflow/java-models)

##Getting started

This project is still WIP and shouldn't be seen as complete.

Download [sqlite](https://sqlite.org/download.html) and install in the root directory

If you want to use the [inception_resnet_v2_1024x1024](https://tfhub.dev/tensorflow/faster_rcnn/inception_resnet_v2_1024x1024/1) model then run uk.khall.sql.CreatePhotoDatabase class,
otherwise use the [openimages_v4_inception_resnet_v2](https://tfhub.dev/google/faster_rcnn/openimages_v4/inception_resnet_v2/1) model then run uk.khall.sql.CreateOpenImagePhotoDatabase class.

The inception_resnet_v2_1024x1024 has been trained on [COCO 2017](https://cocodataset.org/#home).
The openimages_v4_inception_resnet_v2 model has been trained on [Open Images V4](https://storage.googleapis.com/openimages/web/index.html) classifying 600 classes. I've put more work into using this model. I've created some utility code to download and expand the tfhub models.

Next you may want scan a folder of images to add class information to your database. For FasterRcnn use uk.khall.cnn.rcnn.FasterRcnn. It's really basic at the moment. Edit your main procedure accordingly.
Ditto for uk.khall.cnn.rcnn.OpenImages. It's all a bit basic so far.

Finally run uk.khall.ui.interact.RcnnSelectionUI or uk.khall.ui.interact.OpenImageSelectionUI to see a list of classes associate with the objects found in your photos. 
Click on one and you'll be presented with a list of images containing the class. Click on an image then press "View Image"

* `Things to do:`
  * Edit the database.
  * Implement some form of CombinedNonMaxSuppression perhaps - there's a skeleton of the code commented out.
  * Really tidy up.
  * Look at all the new Java goodness.(Records etc.)

* `Issues:`
  * Tensorflow 0.4.1 / 0.4.2 and openimages_v4_inception_resnet_v2 appear to have issues. Before I journey down that rabbit hole I'll leave it at 0.4.0.
