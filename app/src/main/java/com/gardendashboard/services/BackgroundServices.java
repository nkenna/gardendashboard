package com.gardendashboard.services;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import com.gardendashboard.threads.PubConSubThread;
import com.gardendashboard.threads.PubPublishThread;
import com.generalsteinacoz.gardendashboard.R;
import com.helpers.PubnubConfig;
import com.pubnub.api.PubNub;
import com.pubnub.api.PubNubException;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;

import java.util.Arrays;

/**
 * Created by General Steinacoz on 11/20/2017.
 */
public class BackgroundServices extends Service {

    PubConSubThread pubConSubThread;
    PubPublishThread pubPublishThread;

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


       //pubConSubThread.start();
        //pubPublishThread.start();
        return START_STICKY;

    }


}
