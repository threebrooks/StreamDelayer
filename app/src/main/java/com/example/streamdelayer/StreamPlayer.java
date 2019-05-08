package com.example.streamdelayer;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

public class StreamPlayer {

    AudioPlayer mPlayer = null;
    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";

    View mRootView = null;
    Context mCtx = null;
    EditText mStreamURLEditText = null;
    AppCompatButton mStreamStartStopButton = null;
    TextView mStreamDelaySecondsTV = null;
    SeekBar mStreamDelaySeekBar = null;

    AppCompatButton mStreamDelayMinus5Button = null;
    AppCompatButton mStreamDelayMinusPoint5Button = null;
    AppCompatButton mStreamDelayPlusPoint5Button = null;
    AppCompatButton mStreamDelayPlus5Button = null;
    AppCompatButton mStreamDelayTapButton = null;

    float mCurrentDelay = 0.0f;
    boolean mPlay = false;
    String mUrl = "";
    long mStreamDelayTapMillisStart = 0;

    View.OnClickListener mStreamStartStopCL = new  View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPlay = !mPlay;
            if (mPlay) {
                mStreamStartStopButton.setText("Trying to play...");
            } else {
                mStreamStartStopButton.setText("PLAY");
            }
        }
    };

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

    TextWatcher mStreamURLTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mUrl = s.toString();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    private float seekBarProgressToSeconds(int progress) {
        return AudioPlayer.MAX_DELAY_SECONDS*(progress/(float)mStreamDelaySeekBar.getMax());
    }

    private int seekBarSecondsToProgress(float seconds) {
        return (int)((seconds/AudioPlayer.MAX_DELAY_SECONDS)*mStreamDelaySeekBar.getMax());
    }


    SeekBar.OnSeekBarChangeListener mSeekBarCL = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) setDelay(seekBarProgressToSeconds(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    public StreamPlayer(Context ctx, View rootView) {
        mCtx = ctx;
        mRootView = rootView;

        mStreamURLEditText = mRootView.findViewById(R.id.streamURLEditText);
        mUrl = "https://wpr-ice.streamguys1.com/wpr-ideas-mp3-64";
        mStreamURLEditText.setText(mUrl);
        mStreamURLEditText.addTextChangedListener(mStreamURLTextWatcher);

        mStreamStartStopButton = mRootView.findViewById(R.id.streamStartStopButton);
        mStreamStartStopButton.setOnClickListener(mStreamStartStopCL);

        mStreamDelaySecondsTV = mRootView.findViewById(R.id.streamDelaySecondsTextView);

        mStreamDelaySeekBar = mRootView.findViewById(R.id.streamDelaySeekBar);
        mStreamDelaySeekBar.setOnSeekBarChangeListener(mSeekBarCL);

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

        mLoadPlayThread.start();
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
                            httpSource = new HttpMediaSource(Uri.encode(mUrl, ALLOWED_URI_CHARS));
                        } catch (Exception e) {
                            mStreamStartStopButton.setText("Not available, retrying...");
                            Log.d(MainActivity.TAG,e.getMessage());
                            Thread.sleep(1000);
                            continue;
                        }
                        try {
                            mPlayer = new AudioPlayer(mCtx, httpSource);
                            mPlayer.play();
                        } catch (Exception e) {
                            mStreamStartStopButton.setText("Couldn't play stream, retrying...");
                            Log.d(MainActivity.TAG,e.getMessage());
                            Thread.sleep(1000);
                            continue;
                        }
                        while(mPlay && httpSource.ok() && mPlayer.ok()) {
                            mStreamStartStopButton.setText("Playing");
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
                    mStreamStartStopButton.setText("Unexpected failure, retrying...");
                }
            }
        }
    };

    private void setDelay(float seconds) {
        mCurrentDelay = seconds;
        mCurrentDelay = Math.max(0.0f, Math.min(AudioPlayer.MAX_DELAY_SECONDS, mCurrentDelay));
        mCurrentDelay = Math.round(2*mCurrentDelay)/2.0f; // Round to 0.5 seconds increments
        mStreamDelaySecondsTV.setText(Float.toString(mCurrentDelay)+" s");
        mStreamDelaySeekBar.setProgress(seekBarSecondsToProgress(mCurrentDelay));
        if (mPlayer != null) mPlayer.setDelay(mCurrentDelay);
    }

}
