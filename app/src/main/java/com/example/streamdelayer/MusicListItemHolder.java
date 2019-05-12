package com.example.streamdelayer;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MusicListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener
{
    public TextView streamNameET;
    public TextView streamUrlET;
    MusicListAdapter mAdapter;

    public MusicListItemHolder(View itemView, MusicListAdapter adapter)
    {
        super(itemView);
        itemView.setOnClickListener(this);
        streamNameET = itemView.findViewById(R.id.streamName);
        streamUrlET = itemView.findViewById(R.id.streamURL);
        mAdapter = adapter;
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