package com.example.streamdelayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class StreamPlayer {
    long mStreamDelayTapMillisStart = 0;

    View mRootView = null;
    Context mCtx = null;

    AppCompatButton mStreamDelayMinus5Button = null;
    AppCompatButton mStreamDelayMinusPoint5Button = null;
    AppCompatButton mStreamDelayPlusPoint5Button = null;
    AppCompatButton mStreamDelayPlus5Button = null;
    AppCompatButton mStreamDelayTapButton = null;
    DelayCircleView mDelayCircleView = null;

    ImageButton mPlaylistStopButton = null;
    ImageButton mPlaylistAddItemButton = null;

    StreamListDatabase mStreamListDB = null;

    RecyclerView mStreamList = null;

    View.OnClickListener mDelayButtonsCL = new  View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent delayIntent = new Intent(mCtx, PlayerService.class);
            delayIntent.setAction(PlayerService.ACTION_DELAY);
            if (v == mStreamDelayMinus5Button) {
                delayIntent.putExtra("delta", -5.0f);
                mCtx.startService(delayIntent);
            } else if (v == mStreamDelayMinusPoint5Button) {
                delayIntent.putExtra("delta", -0.5f);
                mCtx.startService(delayIntent);
            }else if (v == mStreamDelayPlusPoint5Button) {
                delayIntent.putExtra("delta", +0.5f);
                mCtx.startService(delayIntent);
            } else if (v == mStreamDelayPlus5Button) {
                delayIntent.putExtra("delta", +5.0f);
                mCtx.startService(delayIntent);
            } else if (v == mStreamDelayTapButton) {
                if (mStreamDelayTapMillisStart != 0) {
                    float secondsElapsed = (System.currentTimeMillis()-mStreamDelayTapMillisStart)/1000.0f;
                    delayIntent.putExtra("absolute", secondsElapsed);
                    mCtx.startService(delayIntent);
                    mStreamDelayTapMillisStart = 0;
                    mStreamDelayTapButton.setText("TAP");
                } else {
                    mStreamDelayTapMillisStart = System.currentTimeMillis();
                    mStreamDelayTapButton.setText("...");
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

    View.OnClickListener mPlaylistButtonsCL = new  View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mPlaylistStopButton) {
                Intent startIntent = new Intent(mCtx, PlayerService.class);
                startIntent.setAction(PlayerService.ACTION_STOP);
                mCtx.startService(startIntent);
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

        mPlaylistStopButton = mRootView.findViewById(R.id.playlistStopButton);
        mPlaylistStopButton.setOnClickListener(mPlaylistButtonsCL);
        mPlaylistAddItemButton = mRootView.findViewById(R.id.playlistAddItemButton);
        mPlaylistAddItemButton.setOnClickListener(mPlaylistButtonsCL);
    }

}
