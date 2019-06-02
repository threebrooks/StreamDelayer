package com.threebrooks.streamdelayer;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class MusicListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener
{
    public TextView streamNameET;
    public String streamUrl = "";
    View mMainLL;
    int mAdapterPosition = -1;
    MusicListAdapter mAdapter;
    Thread mCheckThread = null;

    public MusicListItemHolder(final Activity act, View itemView, MusicListAdapter adapter)
    {
        super(itemView);
        itemView.setOnClickListener(this);
        mMainLL = itemView;
        streamNameET = itemView.findViewById(R.id.streamName);
        mAdapter = adapter;
        mCheckThread = new Thread() {
            public void run() {
                while(true) {
                    try {
                        URL url = new URL(streamUrl);
                        URLConnection conn = url.openConnection();
                        conn.getInputStream();
                        act.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                streamNameET.setTextColor(ContextCompat.getColor(act, R.color.purple200));
                            }
                        });
                    } catch (Exception e) {
                        act.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                streamNameET.setTextColor(ContextCompat.getColor(act, R.color.l2Whiten));
                            }
                        });
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {}
                }
            }
        };
        mCheckThread.start();
    }

    @Override
    public void onClick(View view)
    {
        try {
            mAdapter.playItem(getAdapterPosition());
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
        }
    }
}