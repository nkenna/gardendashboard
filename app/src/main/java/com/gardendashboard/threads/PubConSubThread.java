package com.gardendashboard.threads;

import android.graphics.Color;
import android.widget.Toast;
import com.generalsteinacoz.gardendashboard.MainActivity;
import com.helpers.PubnubConfig;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.util.Arrays;

/**
 * Created by General Steinacoz on 11/21/2017.
 */
public class PubConSubThread extends Thread {
    public static final String Tag = "Pubnub Connect Subscribe Thread";
    private static final int DELAY = 5000;
    PubNub pubnub;
    PubnubConfig pubnubConfig;
    MainActivity mainActivity = new MainActivity();

    @Override
    public void run() {
        try {
            pubnubConSubscribe();
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

    //pubnub connect and subscribe
    private void pubnubConSubscribe(){
        final String direction = "direction";
        pubnub = new PubNub(pubnubConfig.pConfig());

        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
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
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.updateActivityTxtView(message.getMessage().getAsString());
                    }
                });
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {

            }
        });
    }

    private void connectProgress(final int colour, final String connectSatus){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.updateConnectBtn(colour, connectSatus);
            }
        });

    }


}
