package com.gardendashboard.threads;

import android.graphics.Color;
import android.widget.Toast;
import com.generalsteinacoz.gardendashboard.MainActivity;
import com.helpers.PubnubConfig;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;

/**
 * Created by General Steinacoz on 11/21/2017.
 */
public class PubPublishThread extends Thread {
    public static final String Tag = "Pubnub Publish Thread";
    private static final int DELAY = 5000;
    PubNub pubnub;
    PubnubConfig pubnubConfig;
    MainActivity mainActivity = new MainActivity();


    @Override
    public void run() {
        super.run();
        try {
            //this.pubnubPublish();
        }catch (Exception e){
            final String err = e.getMessage();
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(null, err, Toast.LENGTH_LONG).show();
                }
            });
        }
    }



    public void pubnubPublish(String payload, String channel){
        pubnub.publish().message(payload).channel(channel).async(new PNCallback<PNPublishResult>() {
            @Override
            public void onResponse(PNPublishResult result, PNStatus status) {
                if (!status.isError()){
                    publishProgress(Color.GREEN, "publish success");

                }else{
                    publishProgress(Color.RED, status.getCategory().toString());

                    status.retry();
                }

            }
        });
    }

    private void publishProgress(final int colour, final String publishStatus){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.updatePubTextView(colour, publishStatus);
            }
        });
    }
}
