package com.example.streamdelayer;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.net.URL;
import java.util.List;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListItemHolder>
{
    private StreamListDatabase mDB;
    private Context mCtx;
    private StreamPlayer mStreamPlayer;

    public MusicListAdapter(Context ctx, StreamListDatabase db, StreamPlayer streamPlayer)
    {
        mDB = db;
        mCtx = ctx;
        mStreamPlayer = streamPlayer;
    }

    @Override
    public MusicListItemHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_player_item, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        MusicListItemHolder rcv = new MusicListItemHolder(layoutView, this);
        return rcv;
    }

    @Override
    public void onBindViewHolder(MusicListItemHolder holder, final int position)
    {
        try {
            StreamListDatabase.StreamListItem item = mDB.getItem(position);
            holder.mMainLL.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mStreamPlayer.EditPlaylistEntry(position);
                    return true;
                }
            });
            holder.streamUrlET.setText(item.mUrl);
            holder.streamNameET.setText(item.mName);
            holder.mAdapterPosition = position;
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
        }
    }

    @Override
    public int getItemCount()
    {
        return mDB.getItemCount();
    }

    public void playItem(int pos) {
        try {
            URL url = new URL(mDB.getItem(pos).mUrl);
            mStreamPlayer.playStream(url);
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
        }
    }
}