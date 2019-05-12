package com.example.streamdelayer;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StreamPlayer {

    AudioPlayer mPlayer = null;
    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";

    View mRootView = null;
    Context mCtx = null;

    AppCompatButton mStreamDelayMinus5Button = null;
    AppCompatButton mStreamDelayMinusPoint5Button = null;
    AppCompatButton mStreamDelayPlusPoint5Button = null;
    AppCompatButton mStreamDelayPlus5Button = null;
    AppCompatButton mStreamDelayTapButton = null;
    DelayCircleView mDelayCircleView = null;

    RecyclerView mStreamList = null;

    float mCurrentDelay = 0.0f;
    boolean mPlay = false;
    URL mUrl = null;
    long mStreamDelayTapMillisStart = 0;

    public void playStream(URL url) {
        mUrl = url;
        mPlay = !mPlay;
    }

    View.OnClickListener mDelayButtonsCL = new  View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mStreamDelayMinus5Button) {
                setDelay(mCurrentDelay-5f);
            } else if (v == mStreamDelayMinusPoint5Button) {
                setDelay(mCurrentDelay-.5f);
            }else if (v == mStreamDelayPlusPoint5Button) {
                setDelay(mCurrentDelay+.5f);
            } else if (v == mStreamDelayPlus5Button) {
                setDelay(mCurrentDelay+5f);
            } else if (v == mStreamDelayTapButton) {
                if (mStreamDelayTapMillisStart != 0) {
                    float secondsElapsed = (System.currentTimeMillis()-mStreamDelayTapMillisStart)/1000.0f;
                    setDelay(secondsElapsed);
                    mStreamDelayTapMillisStart = 0;
                    mStreamDelayTapButton.setText("TAP");
                } else {
                    mStreamDelayTapMillisStart = System.currentTimeMillis();
                    mStreamDelayTapButton.setText("Counting...");
                }
            }
        }
    };

    public StreamPlayer(Context ctx, View rootView) {
        mCtx = ctx;
        mRootView = rootView;

        mDelayCircleView = mRootView.findViewById(R.id.delayCircleView);

        mStreamDelayMinus5Button = mRootView.findViewById(R.id.streamDelayMinus5Button);
        mStreamDelayMinus5Button.setOnClickListener(mDelayButtonsCL);
        mStreamDelayMinusPoint5Button = mRootView.findViewById(R.id.streamDelayMinusPoint5Button);
        mStreamDelayMinusPoint5Button.setOnClickListener(mDelayButtonsCL);
        mStreamDelayPlusPoint5Button = mRootView.findViewById(R.id.streamDelayPlusPoint5Button);
        mStreamDelayPlusPoint5Button.setOnClickListener(mDelayButtonsCL);
        mStreamDelayPlus5Button = mRootView.findViewById(R.id.streamDelayPlus5Button);
        mStreamDelayPlus5Button.setOnClickListener(mDelayButtonsCL);
        mStreamDelayTapButton = mRootView.findViewById(R.id.streamDelayTapButton);
        mStreamDelayTapButton.setOnClickListener(mDelayButtonsCL);

        mStreamList = mRootView.findViewById(R.id.streamListRcv);
        mStreamList.setLayoutManager(new LinearLayoutManager(mCtx));
        mStreamList.setAdapter(new MusicListAdapter(mCtx, getDummyItemList(), this));
        mStreamList.setHasFixedSize(true);

        mLoadPlayThread.start();
    }

    List<MusicListItem> getDummyItemList() {
        List<MusicListItem> list = new ArrayList<>();
        try {
            list.add(new MusicListItem("BvB", new URL("https://bvb-live.cast.addradio.de/bvb/live/mp3/high")));
            list.add(new MusicListItem("Rock", new URL("http://us4.internet-radio.com:8258/")));
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
        }
        return list;
    }

    Thread mLoadPlayThread = new Thread() {
        public void run() {
            while(true) {
                try {
                    if (mPlay) {
                        // https://bvb-live.cast.addradio.de/bvb/live/mp3/high
                        // "https://wpr-ice.streamguys1.com/wpr-ideas-mp3-64"
                        HttpMediaSource httpSource = null;
                        try {
                            httpSource = new HttpMediaSource(mUrl);
                        } catch (Exception e) {
                            Log.d(MainActivity.TAG,e.getMessage());
                            Thread.sleep(1000);
                            continue;
                        }
                        try {
                            mPlayer = new AudioPlayer(mCtx, httpSource);
                            mPlayer.play();
                        } catch (Exception e) {
                            Log.d(MainActivity.TAG,e.getMessage());
                            Thread.sleep(1000);
                            continue;
                        }
                        mDelayCircleView.setAudioPlayer(mPlayer);
                        while(mPlay && httpSource.ok() && mPlayer.ok()) {
                            Thread.sleep(1000);
                        }
                        if (!mPlay) {
                            mPlayer.stop();
                        }
                    } else {
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
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

}
