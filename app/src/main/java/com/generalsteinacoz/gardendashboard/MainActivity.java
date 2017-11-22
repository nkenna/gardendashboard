package com.generalsteinacoz.gardendashboard;



import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.gardendashboard.services.BackgroundServices;
//import com.gardendashboard.threads.PubConSubThread;
//import com.gardendashboard.threads.PubPublishThread;
import com.helpers.PubnubConfig;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.util.Arrays;

public class MainActivity extends Activity {


    PubnubConfig pubnubConfig;
    PubNub pubnub;

    //create button widgets
    Button btn_up, btn_down, btn_right, btn_left, connect_btn;
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

    PubConSubThread pubConSubThread;// = new PubConSubThread();
    PubPublishThread pubPublishThread;// = new PubPublishThread();
    BackgroundServices backgroundServices;


    String payload;
    String channel;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize Pubnub Configuration and PubNub
        pubnubConfig = new PubnubConfig();
        pubnub = new PubNub(pubnubConfig.pConfig());

        //connecting buttons
        btn_up = (Button) findViewById(R.id.btn_up);
        btn_down = (Button) findViewById(R.id.btn_down);
        btn_right = (Button) findViewById(R.id.btn_right);
        btn_left = (Button) findViewById(R.id.btn_left);
        connect_btn = (Button) findViewById(R.id.connect_btn);

        btn_arm = (Switch) findViewById(R.id.switch_arm);
        btn_water = (Switch) findViewById(R.id.switch_water);
        btn_torch = (Switch) findViewById(R.id.switch_torch);

        //connecting textviews
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






    }


    void pubnubSubscribe(){
        //backgroundServices = new BackgroundServices();
        pubPublishThread = new PubPublishThread();
        pubConSubThread = new PubConSubThread();
        //connect_btn.setTextColor(Color.BLUE);
        //connect_btn.setText("Connecting");

        try {
            //Intent is = new Intent(MainActivity.this, BackgroundServices.class);
            //backgroundServices.startService(is);
            pubConSubThread.start();
            pubPublishThread.start();
        }catch (Exception e){
            connect_btn.setText(e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        //Intent serviceIntent = new Intent(MainActivity.this, BackgroundServices.class);
        //backgroundServices.startService(serviceIntent);

    }

  public void updateConnectBtn(int colour, String connectStatus){
      connect_btn.setTextColor(colour);
      connect_btn.setText(connectStatus);
  }

  public void updateActivityTxtView(String message){
      activity_txtView.setText(message);
  }

    public void updatePubTextView(int colour, String publishStatus){
        status_txtView.setTextColor(colour);
        status_txtView.setText(publishStatus);
    }

    public void updateConnectTextView(int colour, String connectStatus){
        connect_txtView.setTextColor(colour);
        connect_txtView.setText(connectStatus);
    }

    public void updateAmbTempTextView(String value){
        if (Integer.parseInt(value) > 35){
            a_temp_txtView.setTextColor(Color.RED);
            a_temp_txtView.setText(value);
        }else{
            a_temp_txtView.setTextColor(Color.GREEN);
            a_temp_txtView.setText(value);
        }

    }

    public void updateSoilTempTextView(String value){
        if (Integer.parseInt(value) < 35){
            s_temp_txtView.setTextColor(Color.RED);
            s_temp_txtView.setText(value);
        }else{
            s_temp_txtView.setTextColor(Color.GREEN);
            s_temp_txtView.setText(value);
        }
    }

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
