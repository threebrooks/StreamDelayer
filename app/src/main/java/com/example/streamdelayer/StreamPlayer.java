package com.example.streamdelayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.Image;
import android.net.Uri;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

    ImageButton mPlaylistStartButton = null;
    ImageButton mPlaylistPauseButton = null;
    ImageButton mPlaylistAddItemButton = null;

    StreamListDatabase mStreamListDB = null;

    RecyclerView mStreamList = null;

    float mCurrentDelay = 0.0f;
    boolean mPlay = false;
    URL mUrl = null;
    long mStreamDelayTapMillisStart = 0;
    private String mStatus = "";

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

    public void EditPlaylistEntry(final int pos) {
        AlertDialog.Builder alert = new AlertDialog.Builder(mCtx);

        alert.setTitle("Edit stream list item");

        try {

            final StreamListDatabase.StreamListItem playlistEntry = mStreamListDB.getItem(pos);

            LayoutInflater inflater = (LayoutInflater) mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.playlist_edit_popup, null);
            final EditText nameET = ll.findViewById(R.id.playlistItemEditName);
            nameET.setText(playlistEntry.mName);
            final EditText urlET = ll.findViewById(R.id.playlistItemEditURL);
            urlET.setText(playlistEntry.mUrl);
            alert.setView(ll);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    try {
                        playlistEntry.mUrl = urlET.getText().toString();
                        playlistEntry.mName = nameET.getText().toString();
                        int newPos = mStreamListDB.setItem(playlistEntry, pos);
                        mStreamList.getAdapter().notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.d(MainActivity.TAG, e.getMessage());
                    }
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });

            alert.show();
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
        }
    }

    public String getStatus() {return mStatus;}

    View.OnClickListener mPlaylistButtonsCL = new  View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mPlaylistStartButton) {
                mPlay = true;
            } else if (v == mPlaylistPauseButton) {
                mPlay = false;
            } else if (v == mPlaylistAddItemButton) {
                try {
                    EditPlaylistEntry(-1);
                } catch (Exception e) {
                    Log.d(MainActivity.TAG, e.getMessage());
                }
            }
        }
    };

    public StreamPlayer(Context ctx, View rootView) {
        mCtx = ctx;
        mRootView = rootView;

        mStreamListDB = new StreamListDatabase(mCtx);

        mDelayCircleView = mRootView.findViewById(R.id.delayCircleView);
        mDelayCircleView.setStreamPlayer(this);

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
        mStreamList.setAdapter(new MusicListAdapter(mCtx, mStreamListDB, this));
        //mStreamList.setHasFixedSize(true);

        mPlaylistStartButton = mRootView.findViewById(R.id.playlistStartButton);
        mPlaylistStartButton.setOnClickListener(mPlaylistButtonsCL);
        mPlaylistPauseButton = mRootView.findViewById(R.id.playlistPauseButton);
        mPlaylistPauseButton.setOnClickListener(mPlaylistButtonsCL);
        mPlaylistAddItemButton = mRootView.findViewById(R.id.playlistAddItemButton);
        mPlaylistAddItemButton.setOnClickListener(mPlaylistButtonsCL);

        mLoadPlayThread.start();
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
                            mPlayer = new AudioPlayer(mCtx, httpSource);
                            mPlayer.setDelay(mCurrentDelay);
                            mPlayer.play();
                        } catch (Exception e) {
                            mStatus = "Error, retrying...";
                            Log.d(MainActivity.TAG,e.getMessage());
                            Thread.sleep(1000);
                            continue;
                        }
                        mDelayCircleView.setAudioPlayer(mPlayer);
                        while(mPlay && httpSource.ok() && mPlayer.ok()) {
                            mStatus = "Playing";
                            Thread.sleep(1000);
                        }
                        if (!mPlay) {
                            mStatus = "Paused";
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
