package com.threebrooks.streamdelayer;

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
import android.widget.Toast;

import java.net.URL;

import static com.threebrooks.streamdelayer.AudioPlayer.MAX_DELAY_SECONDS;

public class PlayerService extends Service {
    private static final String TAG = "PlayerService";

    public static String ACTION_START = "com.threebrooks.streamdelayer.action.start";
    public static String ACTION_MAIN = "com.threebrooks.streamdelayer.action.main";
    public static String ACTION_STOP = "com.threebrooks.streamdelayer.action.stop";
    public static String ACTION_DELAY = "com.threebrooks.streamdelayer.action.delay";
    public static String CHANNEL_ID = MainActivity.APP_NAME;
    public static int PLAYER_SERVICE = 101;

    AudioPlayer mPlayer = null;

    boolean mPlay = false;
    URL mUrl = null;
    String mName = "";
    private String mStatus = "";

    PowerManager.WakeLock mWakelock = null;
    WifiManager.WifiLock mWifilock = null;

    LoadPlayThread mLoadPlayThread = null;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        PlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayerService.this;
        }
    }

    public String getStatus() {return mStatus;}

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_START)) {
            Log.i(TAG, "Received Start Foreground Intent ");

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(ACTION_MAIN);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            String channelId;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                channelId = createChannel();
            else {
                channelId = "";
            }

            mName = intent.getStringExtra("name");
            String url = intent.getStringExtra("url");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle(MainActivity.APP_NAME)
                    .setContentText("Playing "+mName)
                    .setSmallIcon(R.drawable.ic_baseline_play_arrow_24px)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

            startForeground(PLAYER_SERVICE, builder.build());

            PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MainActivity.TAG);
            WifiManager wm = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            mWifilock = wm.createWifiLock(MainActivity.TAG);

            if (mLoadPlayThread != null) {
                mPlay = false;
                try {
                    mLoadPlayThread.join();
                } catch (Exception e) {}
            }

            try {
                mUrl = new URL(url);
                mPlay = true;
            } catch (Exception e) {
                mStatus = "Invalid URL";
            }

            mLoadPlayThread = new LoadPlayThread();
            mLoadPlayThread.start();
        } else if (intent.getAction().equals(ACTION_STOP)) {
            mPlay = false;
            try {
                if (mLoadPlayThread != null) {
                    mLoadPlayThread.join();
                    mLoadPlayThread = null;
                } else {
                    MainActivity.Toast(this, getResources().getString(R.string.stop_nothing_is_playing));
                }
            } catch (Exception e) {
                Log.d(MainActivity.TAG, e.getMessage());
            }
            stopForeground(true);
            stopSelf();
        } else if (intent.getAction().equals(ACTION_DELAY)) {
            if (mPlayer != null) {
                if (intent.hasExtra("delta")) {
                    mPlayer.addToDelay(((float)intent.getFloatExtra("delta", 0.0f)));
                }
                if (intent.hasExtra("absolute")) {
                    mPlayer.setAbsoluteDelay(intent.getFloatExtra("absolute", (float)(mPlayer.getCurrentDelay())));
                }
            }else {
                MainActivity.Toast(this, getResources().getString(R.string.stop_nothing_is_playing));
            }
        }
        return START_NOT_STICKY;
    }


    class LoadPlayThread extends Thread {
        public void run() {
            int maxBytesPerSecond = 48000*2;
            RingBuffer ringBuffer = new RingBuffer((int)(MAX_DELAY_SECONDS*maxBytesPerSecond), 0);
            while(mPlay) {
                try {
                    if (mPlayer != null) {
                        mStatus = "Stopped";
                        mPlayer.destroy();
                        mPlayer = null;
                    }

                    try {
                        mPlayer = new AudioPlayer(PlayerService.this, mUrl, ringBuffer);
                        mPlayer.play();
                    } catch (Exception e) {
                        mStatus = "Error, retrying...";
                        Log.d(MainActivity.TAG,e.getMessage());
                        Thread.sleep(1000);
                        continue;
                    }
                    mWakelock.acquire();
                    mWifilock.acquire();
                    while(mPlay) {
                        mStatus = "Playing "+mName;
                        Thread.sleep(1000);
                    }
                    if (!mPlay) {
                        mStatus = "Stopped";
                        mPlayer.destroy();
                        mPlayer = null;
                        mWakelock.release();
                        mWifilock.release();
                    }
                } catch (Exception e) {
                    Log.d(MainActivity.TAG, e.getMessage());
                    if (mWakelock.isHeld()) mWakelock.release();
                    if (mWifilock.isHeld()) mWifilock.release();
                }
            }
        }
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
        if (mLoadPlayThread != null) {
            mPlay = false;
            try {
                mLoadPlayThread.join();
            } catch (Exception e) {}
        }
        Log.i(TAG, "In onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}