# Android-Object-Detection
A template and demo application for object detection on Android. **[You can find complete Android Studio project folder and APK file in here.](https://drive.google.com/drive/folders/1XXm1DYxNjEjSPo3TxjJjj5vTKAg1KSEU?usp=sharing)** Since, GitHub has file count and size limitations, I have uploaded the project to Google Drive.

Project uses MobileNet SSD for object detection which is a mobile friendly neural network.

## About SSD and MobileNet

Briefly, **[MobileNet](https://arxiv.org/abs/1704.04861)** is a backbone (the part of neural network which is used for feature extraction) for Convolutional Neural Networks (CNNs) and it uses depthwise separable convolution instead of classical convolution. Thanks to its specific convolution type, it is faster than similar backbones.

**[Single Shot Detector (SSD)](https://arxiv.org/abs/1512.02325)** is a Convolutional Neural Network (CNN) which is developed for object detection and it uses **[VGG-16](https://arxiv.org/abs/1409.1556)** as backbone by default.

MobileNet SSD is the faster (and less accurate sadly) version of SSD. It uses MobileNet as backbone which makes it more mobile friendly in terms of both computation power and memory consumption.

Following plot compares detection speed of both SSD and MobileNet SSD for an example video in an Android device (Xiaomi Mi 5s). Horizontal axis represents frames and vertical axis shows the time spent for each frame in miliseconds. As you can see clearly, the difference in speed is dramatic.

<img src="/Images/Speed.jpg">

Additionaly, following plot shows the performance of MobileNet SSD on **[MS COCO](https://cocodataset.org/#home)** 2014 test dataset with respect to different Intersection over Union (IoU) thresholds. Bars represet recall, precision and F-1 score, respectively.

<img src="/Images/Results.jpg">



## Android App

The project is based on this **[example](https://docs.opencv.org/3.4/d0/d6c/tutorial_dnn_android.html)** provided by OpenCV. It has been developed by using OpenCV 3.4.1. and related OpenCV files are already in the project folder, you do not need to add them again.

Model files were taken from **[Model Zoo.](https://modelzoo.co)**

Following video is an example of Android app.

https://user-images.githubusercontent.com/40580957/145451134-e4c3ea9b-19e6-44bd-ac34-aefb446fa8ed.mp4
