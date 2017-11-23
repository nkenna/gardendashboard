package com.generalsteinacoz.gardendashboard;



import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.*;
import com.helpers.PubnubConfig;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    PubnubConfig pubnubConfig;
    PubNub pubnub;

    //create button widgets
    Button btn_up, btn_down, btn_right, btn_left, connect_btn;

    //create Switch Widgets
    Switch btn_arm, btn_water, btn_torch;

    //create textview widgets
    TextView activity_txtView, status_txtView, connect_txtView, a_temp_txtView, s_temp_txtView, humdity_txtView, s_moisture_txtView;

    //pubnub channels
    private final String direction = "direction";
    private final String operation = "operation";
    private final String feedbacks = "feedbacks";
    private final String a_temp = "a_temp";
    private final String s_temp = "s_temp";
    private final String humdity = "humdity";
    private final String soil_moisture = "soil_moisture";

    PubConSubThread pubConSubThread;// new PubConSubThread();
    PubPublishThread pubPublishThread;// new PubPublishThread();

    // message and channel that will be used during publishing
    String payload;
    String channel;


    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;


    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private MenuItem               mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;
    private MenuItem mItemType;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;







    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };




    public MainActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
            Toast.makeText(this, "OpenCV not loaded", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "OpenCV loaded");
            Toast.makeText(this, "OpenCV loaded", Toast.LENGTH_LONG).show();
        }

        //initialize Pubnub Configuration and PubNub
        pubnubConfig = new PubnubConfig();
        pubnub = new PubNub(pubnubConfig.pConfig());

        //getting the buttons
        btn_up = (Button) findViewById(R.id.btn_up);
        btn_down = (Button) findViewById(R.id.btn_down);
        btn_right = (Button) findViewById(R.id.btn_right);
        btn_left = (Button) findViewById(R.id.btn_left);
        connect_btn = (Button) findViewById(R.id.connect_btn);

        btn_arm = (Switch) findViewById(R.id.switch_arm);
        btn_water = (Switch) findViewById(R.id.switch_water);
        btn_torch = (Switch) findViewById(R.id.switch_torch);

        //getting textviews
        activity_txtView = (TextView) findViewById(R.id.activity_txtView);
        status_txtView = (TextView) findViewById(R.id.status_txtView);
        connect_txtView = (TextView) findViewById(R.id.connect_txtView);
        a_temp_txtView = (TextView) findViewById(R.id.a_temp_txtView);
        s_temp_txtView = (TextView) findViewById(R.id.s_temp_txtView);
        humdity_txtView = (TextView) findViewById(R.id.humdity_txtView);
        s_moisture_txtView = (TextView) findViewById(R.id.s_moisture_txtView);


        //methods that handle button events
        btn_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnUpPressed();
            }
        });

        btn_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnDownPressed();
            }
        });

        btn_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnRightPressed();
            }
        });

        btn_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnLeftPressed();
            }
        });

        connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //subscribe to pubnub channels
                pubnubSubscribe();
            }
        });




        btn_arm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnArmPressed(isChecked);
            }
        });

        btn_water.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnWaterPressed(isChecked);
            }
        });

        btn_torch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnTorchPressed(isChecked);
            }
        });



        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);

        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);



    }


    void pubnubSubscribe(){

        pubPublishThread = new PubPublishThread();
        pubConSubThread = new PubConSubThread();


        try {

            pubConSubThread.start();
            pubPublishThread.start();
        }catch (Exception e){
            connect_btn.setText(e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }



    }

  /**public void updateConnectBtn(int colour, String connectStatus){
      connect_btn.setTextColor(colour);
      connect_btn.setText(connectStatus);
  }**/

  //this method is called from the PubConSub thread
  public void updateActivityTxtView(String message){
      activity_txtView.setText(message);
  }

    /**this method is called from the Pubpublish thread
        and updates the status textView **/
    public void updatePubTextView(int colour, String publishStatus){
        status_txtView.setTextColor(colour);
        status_txtView.setText(publishStatus);
    }

    /**this method is called from the PubConSub thread
     and updates the connect textView **/
    public void updateConnectTextView(int colour, String connectStatus){
        connect_txtView.setTextColor(colour);
        connect_txtView.setText(connectStatus);
    }

    /**this method is called from the Pubpublish thread
     and updates the amb. temp textView **/
    public void updateAmbTempTextView(String value){
        if (Integer.parseInt(value) > 35){
            a_temp_txtView.setTextColor(Color.RED);
            a_temp_txtView.setText(value);
        }else{
            a_temp_txtView.setTextColor(Color.GREEN);
            a_temp_txtView.setText(value);
        }

    }

    /**this method is called from the Pubpublish thread
     and updates the soil temp textView **/
    public void updateSoilTempTextView(String value){
        if (Integer.parseInt(value) < 35){
            s_temp_txtView.setTextColor(Color.RED);
            s_temp_txtView.setText(value);
        }else{
            s_temp_txtView.setTextColor(Color.GREEN);
            s_temp_txtView.setText(value);
        }
    }

    /**this method is called from the Pubpublish thread
     and updates the status textView **/
    public void updateHumdityTextView(String value){
        if (Integer.parseInt(value) < 70){
            humdity_txtView.setTextColor(Color.RED);
            humdity_txtView.setText(value);
        }else{
            humdity_txtView.setTextColor(Color.GREEN);
            humdity_txtView.setText(value);
        }
    }

    public void updateSoilMositureTextView(String value){
        if (Integer.parseInt(value) < 75){
            s_moisture_txtView.setTextColor(Color.RED);
            s_moisture_txtView.setText(value);
        }else{
            s_moisture_txtView.setTextColor(Color.GREEN);
            s_moisture_txtView.setText(value);
        }

    }





    void btnUpPressed(){
        payload = "up";
        channel = direction;
        pubPublishThread.pubnubPublish(payload, channel);

    }

    void btnDownPressed(){

        payload = "down";
        channel = direction;
        pubPublishThread.pubnubPublish(payload, channel);

    }

    void btnRightPressed(){

        payload = "right";
        channel = direction;
        pubPublishThread.pubnubPublish(payload, channel);


    }

    void btnLeftPressed(){
    
        payload = "left";
        channel = direction;
        pubPublishThread.pubnubPublish(payload, channel);

    }

    void btnArmPressed(boolean chk){
        if(chk){
            payload = "arm";
            pubPublishThread.pubnubPublish(payload, channel);
            channel = operation;
        }else{
            payload = "disarm";
            pubPublishThread.pubnubPublish(payload, channel);
            channel = operation;
        }


    }

    void btnWaterPressed(boolean chk){
        if (chk){
            payload = "water on";
            channel = operation;
            pubPublishThread.pubnubPublish(payload, channel);
        }else{
            payload = "water off";
            channel = operation;
            pubPublishThread.pubnubPublish(payload, channel);
        }


    }

    void btnTorchPressed(boolean chk){
        if (chk){
            payload = "torch on";
            channel = operation;
            pubPublishThread.pubnubPublish(payload, channel);
        }else {
            payload = "torch off";
            channel = operation;
            pubPublishThread.pubnubPublish(payload, channel);
        }


    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public void onCameraViewStarted(int i, int i1) {
        mGray = new Mat();
        mRgba = new Mat();

    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {


        mRgba = cvCameraViewFrame.rgba();
        mGray = cvCameraViewFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        return mRgba;

    }




    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
            }
        }
    }



    /**
     * Created by General Steinacoz on 11/21/2017.
     */
    public class PubConSubThread extends Thread {
        public static final String Tag = "Pubnub Connect Subscribe Thread";
        private static final int DELAY = 5000;

        //MainActivity mainActivity = new MainActivity();

        @Override
        public void run() {
            try {
                pubnubConSubscribe();
            }catch (Exception e){
                final String err = e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, err, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        //pubnub connect and subscribe
        private void pubnubConSubscribe(){

            connectProgress(Color.RED, "Connecting");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "inside pubnubConSubscribe method", Toast.LENGTH_LONG).show();
                }
            });


            //final String direction = "direction";


            pubnub.addListener(new SubscribeCallback() {
                @Override
                public void status(PubNub pubnub, PNStatus status) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "inside status pubnub addlistener", Toast.LENGTH_LONG).show();
                        }
                    });

                    if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory){
                        pubnub.reconnect();
                    }else if (status.getCategory() == PNStatusCategory.PNConnectedCategory){
                        if (status.getCategory() == PNStatusCategory.PNConnectedCategory){
                            connectProgress(Color.GREEN, "Connected");
                            pubnub.subscribe().channels(Arrays.asList(direction)).execute();
                        }else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory){

                            connectProgress(Color.GREEN, "Reconnection");
                            pubnub.subscribe().channels(Arrays.asList(direction)).execute();
                        }else if (status.getCategory() == PNStatusCategory.PNDecryptionErrorCategory){
                            pubnub.reconnect();
                        }else if (status.getCategory() == PNStatusCategory.PNTimeoutCategory){
                            pubnub.reconnect();
                        }else {
                            pubnub.reconnect();

                            connectProgress(Color.RED, "No Connection");
                        }
                    }
                }

                @Override
                public void message(PubNub pubnub, final PNMessageResult message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (message.getChannel().equalsIgnoreCase(feedbacks) || message.getChannel().equalsIgnoreCase(operation) || message.getChannel().equalsIgnoreCase(direction)){
                                updateActivityTxtView(message.getMessage().getAsString() );
                            }else if (message.getChannel().equalsIgnoreCase(a_temp)){
                                updateAmbTempTextView(message.getMessage().getAsString());
                            }else if (message.getChannel().equalsIgnoreCase(s_temp)){
                                updateSoilTempTextView(message.getMessage().getAsString());
                            }else if (message.getChannel().equalsIgnoreCase(humdity)){
                                updateHumdityTextView(message.getMessage().getAsString());
                            }else if (message.getChannel().equalsIgnoreCase(soil_moisture)){
                                updateSoilMositureTextView(message.getMessage().getAsString());
                            }

                        }
                    });
                }

                @Override
                public void presence(PubNub pubnub, PNPresenceEventResult presence) {

                }
            });

            pubnub.subscribe().channels(Arrays.asList(direction, operation, feedbacks, a_temp, s_temp, soil_moisture, humdity )).execute();
        }

        private void connectProgress(int colour, String connectStatus){
            final int col = colour;
            final String cStatus = connectStatus;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateConnectTextView(col, cStatus);
                }
            });

        }


    }



    /**
     * Created by General Steinacoz on 11/21/2017.
     */
    public class PubPublishThread extends Thread {
        public static final String Tag = "Pubnub Publish Thread";
        private static final int DELAY = 5000;

        MainActivity mainActivity = new MainActivity();


        @Override
        public void run() {
            super.run();

        }



        public void pubnubPublish(String payload, String channel){

            try {
                pubnub.publish().message(payload).channel(channel).async(new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        if (!status.isError()) {
                            publishProgress(Color.GREEN, "publish success");
                        } else {
                            publishProgress(Color.RED, status.getCategory().toString());
                            status.retry();
                        }

                    }
                });
            }catch (Exception e){
               final String err = e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, err, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        private void publishProgress(final int colour, final String publishStatus){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updatePubTextView(colour, publishStatus);
                }
            });
        }
    }




}
