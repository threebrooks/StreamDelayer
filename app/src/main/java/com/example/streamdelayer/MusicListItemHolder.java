package com.example.streamdelayer;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MusicListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener
{
    public TextView songNameTV;
    private MusicPlayer mMusicPlayer;

    public MusicListItemHolder(View itemView, MusicPlayer musicPlayer)
    {
        super(itemView);
        itemView.setOnClickListener(this);
        songNameTV = itemView.findViewById(R.id.SongName);
        mMusicPlayer = musicPlayer;
    }

    @Override
    public void onClick(View view)
    {
        try {
            mMusicPlayer.playSong(getAdapterPosition());
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
        }
    }
}