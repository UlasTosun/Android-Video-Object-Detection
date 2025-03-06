package com.example.ulas.ObjectDetector;

// Android libraries
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.WindowManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.view.View;

// OpenCV libraries
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Net;
import org.opencv.dnn.Dnn;
import org.opencv.imgproc.Imgproc;

// Java libraries
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {

    private static final String TAG = "ObjectDetector";
    private static final String[] classNames = {"background" , "person" , "bicycle" , "car" , "motorcycle" ,
            "airplane" , "bus" , "train" , "truck" , "boat" , "traffic light",
            "fire hydrant", "N/A" , "stop sign", "parking meter", "bench" ,
            "bird" , "cat" , "dog" , "horse" , "sheep" , "cow" , "elephant" ,
            "bear" , "zebra" , "giraffe" , "N/A" , "backpack" , "umbrella" ,
            "N/A" , "N/A" , "handbag" , "tie" , "suitcase" , "frisbee" , "skis" ,
            "snowboard" , "sports ball", "kite" , "baseball bat", "baseball glove",
            "skateboard" , "surfboard" , "tennis racket", "bottle" , "N/A" ,
            "wine glass", "cup" , "fork" , "knife" , "spoon" , "bowl" , "banana" ,
            "apple" , "sandwich" , "orange" , "broccoli" , "carrot" , "hot dog",
            "pizza" , "donut" , "cake" , "chair" , "couch" , "potted plant",
            "bed" , "N/A" , "dining table", "N/A" , "N/A" , "toilet" , "N/A" ,
            "tv" , "laptop" , "mouse" , "remote" , "keyboard" , "cell phone",
            "microwave" , "oven" , "toaster" , "sink" , "refrigerator" , "N/A" ,
            "book" , "clock" , "vase" , "scissors" , "teddy bear", "hair drier",
            "toothbrush", "EXTRA", "EXTRA", "EXTRA", "EXTRA", "EXTRA", "EXTRA"};

    private Net net;    // the neural network
    private CameraBridgeViewBase mOpenCvCameraView;

    // time data
    long deltaTimeInMs = 0L;
    long currentTimeFPS = 0L;
    long lastTimeFPS = 0L;
    float fps = 0f;

    // edit fields
    private TextView fpsMeter;



    // Initialize OpenCV manager.
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fpsMeter = findViewById(R.id.textFPS);

        // Handle quit button
        final Button quitButton = findViewById(R.id.quit);
        quitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View d) {
                MainActivity.this.finishAffinity();
                System.exit(0);
            }
        });

        // Switch to full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Handle permissions
        HandleCameraPermission();
        HandleStoragePermission();

    }



    private void HandleCameraPermission(){
        // Check for camera access permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            Log.i(TAG, "Camera access permission is granted");
            // Set up camera listener.
            mOpenCvCameraView =  findViewById(R.id.camera_view);
            mOpenCvCameraView.setMaxFrameSize(1280, 720);
            mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
        }
        // Ask for camera access permission if needed
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }
    }



    private void HandleStoragePermission(){
        // Check for writing external storage permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            Log.i(TAG, "Writing external memory permission is granted");
        }
        // Ask for writing external storage permission if needed
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }



    @Override
    public void onResume() {
        super.onResume();

        // Check for OpenCV libraries
        if(!OpenCVLoader.initDebug()){
            Log.i(TAG, "OpenCv Libraries are not loaded");
            // If OpenCV libraries is not loaded, trying to load it again
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.i(TAG, "OpenCv Libraries are loaded successfully");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        // Load model files after loading OpenCV libraries
        String proto = getPath("SSDLite.prototxt", this);       // Read the model
        String weights = getPath("SSDLite.caffemodel", this);   // Read the model

        // Initialize network
        net = Dnn.readNetFromCaffe(proto, weights);
        Log.i(TAG, "Network loaded successfully");
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            // The permission with the request code 0 (camera access)
            case 0: {
                // If request is cancelled, the result arrays are empty.
                // Permission is granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Camera access permission is granted");
                    Toast.makeText(this, "Camera access permission is granted", Toast.LENGTH_SHORT).show();
                    // Set up camera listener.
                    mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
                    //mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.setMaxFrameSize(1280, 720);
                    mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
                    mOpenCvCameraView.setCvCameraViewListener(this);
                }
                // Permission is denied
                else {
                    Log.i(TAG, "Camera access permission is denied");
                    Toast.makeText(this, "Camera access permission is denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }

            // The permission with the request code 1 (writing external storage)
            case 1: {
                // If request is cancelled, the result arrays are empty.
                // Permission is granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Writing external memory permission is granted");
                }
                // Permission is denied
                else {
                    Log.i(TAG, "Writing external memory permission is denied");
                }
                return;
            }
        }
    }



    public void onCameraViewStarted(int width, int height) {}



    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        // calculate delta time between two frames
        currentTimeFPS = Calendar.getInstance().getTimeInMillis();
        deltaTimeInMs = (long)(currentTimeFPS - lastTimeFPS);
        fps = 1f / (((float)(int)(long) (currentTimeFPS - lastTimeFPS)) / 1000f);
        lastTimeFPS = currentTimeFPS;

        // set FPS
        fpsMeter.setText("FPS: " + String.format("%.1f", fps));

        // set variables
        final int IN_WIDTH = 300;
        final int IN_HEIGHT = 300;
        final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
        final double IN_SCALE_FACTOR = 0.007843;
        final double MEAN_VAL = 127.5;
        final double THRESHOLD = 0.5;   // confidence threshold

        // Get a new frame
        Mat frame = inputFrame.rgba();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

        // Forward image through network.
        Mat blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR, new Size(IN_WIDTH, IN_HEIGHT), new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false, false);
        net.setInput(blob);
        Mat detections = net.forward();

        int cols = frame.cols();
        int rows = frame.rows();
        Size cropSize;
        if ((float)cols / rows > WH_RATIO) {
            cropSize = new Size(rows * WH_RATIO, rows);
        } else {
            cropSize = new Size(cols, cols / WH_RATIO);
        }

        detections = detections.reshape(1, (int)detections.total() / 7);

        for (int i = 0; i < detections.rows(); ++i) {
            double confidence = detections.get(i, 2)[0];

            if (confidence > THRESHOLD) {
                int classId = (int)detections.get(i, 1)[0];
                int left = (int)(detections.get(i, 3)[0] * cols);
                int top = (int)(detections.get(i, 4)[0] * rows);
                int right   = (int)(detections.get(i, 5)[0] * cols);
                int bottom   = (int)(detections.get(i, 6)[0] * rows);

                // Draw rectangle around detected object.
                Imgproc.rectangle(frame, new Point(left, top), new Point(right, bottom), new Scalar(0, 255, 0), 2);
                String label = classNames[classId] + ": " + String.format("%.2f", confidence);
                Imgproc.putText(frame, label, new Point(left, top - 5), Core.FONT_HERSHEY_TRIPLEX, 1.2, new Scalar(0, 255, 0));

            }
        }

        return frame;
    }



    public void onCameraViewStopped() {}



    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            Log.i(TAG, "File loaded successfully");
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }


}
