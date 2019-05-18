package com.example.streamdelayer;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.net.URL;

public class PlayerService extends Service {
    private static final String TAG = "PlayerService";

    public static String ACTION_START_PLAYER_SERVICE = "com.example.streamdelayer.action.start_player_service";
    public static String ACTION_STOP_PLAYER_SERVICE = "com.example.streamdelayer.action.start_player_service";
    public static String ACTION_MAIN = "com.example.streamdelayer.action.main";
    public static String ACTION_PLAY = "com.example.streamdelayer.action.play";
    public static String ACTION_PAUSE = "com.example.streamdelayer.action.pause";
    public static String ACTION_DELAY = "com.example.streamdelayer.action.delay";
    public static String CHANNEL_ID = MainActivity.APP_NAME;
    public static int PLAYER_SERVICE = 101;

    AudioPlayer mPlayer = null;

    float mCurrentDelay = 0.0f;
    boolean mPlay = false;
    URL mUrl = null;
    private String mStatus = "";

    PowerManager.WakeLock mWakelock = null;
    WifiManager.WifiLock mWifilock = null;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        PlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayerService.this;
        }
    }

    public void playStream(URL url) {
        mUrl = url;
        mPlay = !mPlay;
    }

    public String getStatus() {return mStatus;}

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_START_PLAYER_SERVICE)) {
            Log.i(TAG, "Received Start Foreground Intent ");

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(ACTION_MAIN);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Intent playIntent = new Intent(this, PlayerService.class);
            playIntent.setAction(ACTION_PLAY);
            PendingIntent pPlayIntent = PendingIntent.getService(this, 0, playIntent, 0);

            Intent pauseIntent = new Intent(this, PlayerService.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pPauseIntent = PendingIntent.getService(this, 0, pauseIntent, 0);

            //Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_play_arrow_24px);

            String channelId;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                channelId = createChannel();
            else {
                channelId = "";
            }

            // Get the layouts to use in the custom notification
            //RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_small);
            //RemoteViews notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.notification_big);

            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle(MainActivity.APP_NAME)
                    .setTicker(MainActivity.APP_NAME)
                    //.setContentText(MainActivity.APP_NAME)
                    .setSmallIcon(R.drawable.ic_baseline_folder_open_24px)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle()).build();
                    //.setCustomContentView(notificationLayout)
                    //.setCustomBigContentView(notificationLayoutExpanded).build();
                    //.addAction(R.drawable.ic_baseline_play_arrow_24px,"Play", pPlayIntent)
                    //.addAction(R.drawable.ic_baseline_pause_24px, "Pause", pPauseIntent).build();
            startForeground(PLAYER_SERVICE, notification);
            mLoadPlayThread.start();

            PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MainActivity.TAG);
            WifiManager wm = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            mWifilock = wm.createWifiLock(MainActivity.TAG);

        } else if (intent.getAction().equals(ACTION_PLAY)) {
            try {
                if (intent.hasExtra("url")) mUrl = new URL(intent.getStringExtra("url"));
            } catch (Exception e) {
                mStatus = "Invalid URL";
            }
            mPlay = true;
        } else if (intent.getAction().equals(ACTION_PAUSE)) {
            mPlay = false;
        } else if (intent.getAction().equals(ACTION_DELAY)) {
            if (intent.hasExtra("delta")) {
                mPlayer.setDelay(mPlayer.getDelay()+ intent.getFloatExtra("delta", 0.0f));
            }
            if (intent.hasExtra("absolute")) {
                mPlayer.setDelay(intent.getFloatExtra("absolute", mPlayer.getDelay()));
            }
        } else if (intent.getAction().equals(ACTION_STOP_PLAYER_SERVICE)) {
            Log.i(TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }


    Thread mLoadPlayThread = new Thread() {
        public void run() {
            while(true) {
                try {
                    if (mPlay) {
                        HttpMediaSource httpSource = null;
                        try {
                            httpSource = new HttpMediaSource(mUrl);
                        } catch (Exception e) {
                            mStatus = "Connecting...";
                            Log.d(MainActivity.TAG,e.getMessage());
                            Thread.sleep(1000);
                            continue;
                        }
                        try {
                            mPlayer = new AudioPlayer(PlayerService.this, httpSource);
                            mPlayer.setDelay(mCurrentDelay);
                            mPlayer.play();
                        } catch (Exception e) {
                            mStatus = "Error, retrying...";
                            Log.d(MainActivity.TAG,e.getMessage());
                            Thread.sleep(1000);
                            continue;
                        }
                        mWakelock.acquire();
                        mWifilock.acquire();
                        while(mPlay && httpSource.ok() && mPlayer.ok()) {
                            mStatus = "Playing";
                            Thread.sleep(1000);
                        }
                        if (!mPlay) {
                            mStatus = "Paused";
                            mPlayer.stop();
                            mWakelock.release();
                            mWifilock.release();
                        }
                    } else {
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    mWakelock.release();
                    mWifilock.release();
                    Log.d(MainActivity.TAG,e.getMessage());
                }
            }
        }
    };

    private void setDelay(float seconds) {
        mCurrentDelay = seconds;
        mCurrentDelay = Math.max(0.0f, Math.min(AudioPlayer.MAX_DELAY_SECONDS, mCurrentDelay));
        mCurrentDelay = Math.round(2*mCurrentDelay)/2.0f; // Round to 0.5 seconds increments
        //mStreamDelaySecondsTV.setText(Float.toString(mCurrentDelay)+" s");
        if (mPlayer != null) mPlayer.setDelay(mCurrentDelay);
    }

    @NonNull
    @TargetApi(26)
    private synchronized String createChannel() {
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);
        if (mNotificationManager != null) {
            mNotificationManager.createNotificationChannel(mChannel);
        } else {
            stopSelf();
        }
        return CHANNEL_ID;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "In onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}