package com.example.kelvi.eee508androidopencvtutorial;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.lang.Math;

import android.widget.TextView;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener,CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    double x = -1;
    double y = -1;
    TextView touch_coordinates;
    TextView touch_color;
    TextView touch_motion;
    Button setMotionDetection;
    Button setBrightnessAdjustment;

    private boolean firstFrame = true;
    private Mat previousFrame;
    double sumTotal = 0;
    double sumTotalRGB = 0;
    int beta = 0;
    double sumR=0;
    double sumG=0;
    double sumB=0;
    boolean MotionDetection=false;
    int brightnessSet=0;
    int betaTemp;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        touch_coordinates = (TextView) findViewById(R.id.touch_coordinates); //display the touch coordinates
        touch_color = (TextView) findViewById(R.id.touch_color); //display the touch color
        touch_motion = (TextView) findViewById(R.id.touch_motion); //display the mode of operation
        setMotionDetection = (Button) findViewById(R.id.setMotionDetection); //button to select Motion Detection
        setMotionDetection.setOnClickListener(onClickListener);
        setBrightnessAdjustment = (Button) findViewById(R.id.setBrightnessAdjustment); //button to select Brightness Adjustment
        setBrightnessAdjustment.setOnClickListener(onClickListener);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_tutorial_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    //Method to invoke the two button clicks, Set Motion Detection and Brightness Adjustment
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            switch(v.getId()){
                case R.id.setMotionDetection:
                    MotionDetection=true;   //sets the mode as Motion Detection
                    break;
                case R.id.setBrightnessAdjustment:
                    MotionDetection=false;  //sets the mode as Brightness Adjustment
                    break;
            }

        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        double yLow = (double) mOpenCvCameraView.getHeight() * 0.2401961;
        double yHigh = (double) mOpenCvCameraView.getHeight() * 0.7696078;
        double xScale = (double) cols / (double) mOpenCvCameraView.getWidth();
        double yScale = (double) rows / (yHigh - yLow);
        x = motionEvent.getX();
        y = motionEvent.getY();
        y = y - yLow;
        x = x * xScale;
        y = y * yScale;
        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
        touch_coordinates.setText("X: " + Double.valueOf(x) + ", Y: " + Double.valueOf(y));
        Rect touchedRect = new Rect();
        touchedRect.x = (int) x;
        touchedRect.y = (int) y;
        touchedRect.width = 8;
        touchedRect.height = 8;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);

        touch_color.setText("Color: #" + String.format("%02X", (int) mBlobColorRgba.val[0])
                + String.format("%02X", (int) mBlobColorRgba.val[1])
                + String.format("%02X", (int) mBlobColorRgba.val[2]));

        touch_color.setTextColor(Color.rgb((int) mBlobColorRgba.val[0],
                (int) mBlobColorRgba.val[1],
                (int) mBlobColorRgba.val[2]));

        touch_coordinates.setTextColor(Color.rgb((int) mBlobColorRgba.val[0],
                (int) mBlobColorRgba.val[1],
                (int) mBlobColorRgba.val[2]));

        return false;
    }

    //Method to check the pixel values of 6 X 6 subimage in the centre of the frame, based on the threshold value (350),
    // the pixel values are increased or decreased by 99
    private int controlBrightness()
    {
        int diffBrightness;
        sumR=0;
        sumG=0;
        sumB=0;
        for(int i=(mRgba.rows()/2-3); i<=(mRgba.rows()/2+2) ; i++)
            for(int j=(mRgba.cols()/2-3) ; j<=(mRgba.cols()/2+2) ; j++)
            {
                sumR = sumR+mRgba.get(i,j)[0];
                sumG = sumG+mRgba.get(i,j)[1];
                sumB = sumB+mRgba.get(i,j)[2];
            }
        sumR=sumR/36;
        sumG=sumG/36;
        sumB=sumB/36;
        if((sumR+sumG+sumB) < 350)
            diffBrightness=99; //pixel value increased by 99
        else
            diffBrightness=-99; //pixel value decreased by 99

        return diffBrightness;
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
        return new Scalar(pointMatRgba.get(0, 0));
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        mRgba = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        sumR = 0;
        sumG = 0;
        sumB = 0;

        if(MotionDetection) { //if Motion Detection selected
            if(firstFrame)
            {
                //if first frame, do nothing since there is no previous frame to compare
                previousFrame=inputFrame.rgba();
                firstFrame = false;
            }
            else
            {
                //compare the RGB values with the previous frame, compute the RGB value of  40 X 40 subimage at the center of the image
                for (int i = (mRgba.rows()/2-20); i < (mRgba.rows()/2+19); i++)
                    for (int j = (mRgba.cols()/2-20); j < (mRgba.cols()/2+19); j++) {
                        sumR = sumR + Math.abs(mRgba.get(i, j)[0] - previousFrame.get(i, j)[0]);
                        sumG = sumG + Math.abs(mRgba.get(i, j)[1] - previousFrame.get(i, j)[1]);
                        sumB = sumB + Math.abs(mRgba.get(i, j)[2] - previousFrame.get(i, j)[2]);
                    }
                //}
                previousFrame = inputFrame.rgba();
            }
            sumTotalRGB=sumR+sumG+sumB; //add all R,G,B values
            if(sumTotalRGB>15000) { //if total RGB value exceeds threshold(15000), display motion detected
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        touch_motion.setText("Motion :Detected");
                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        touch_motion.setText("Motion :");
                    }
                });
            }
        }
        else
        {
            //if brightness adjustment is selected
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    touch_motion.setText("Brightness adjustment"); //display the mode
                }
            });
            //computing the brightness level and setting the new brightness after 10 frames(logic added to detect the changes in the UI)
            if(brightnessSet <= 9) {

                if(brightnessSet == 0)
                    betaTemp = controlBrightness();

                if(brightnessSet == 9)
                {
                    beta = betaTemp;
                    brightnessSet = 0;
                }
                else
                    brightnessSet = brightnessSet + 1;
            }
            //based on the brightness computed update all pixel values
            for(int i=0; i< mRgba.rows();i++)
                for(int j=0; j< mRgba.cols();j++)
                {
                    double[] data = mRgba.get(i, j);
                    data[0] = data[0]+beta;
                    data[1] = data[1]+beta;
                    data[2] = data[2]+beta;
                    mRgba.put(i, j, mRgba.get(i, j));
            }
        }

        return mRgba;
    }
}
