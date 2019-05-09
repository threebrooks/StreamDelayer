package com.example.streamdelayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListItemHolder>
{
    private List<MusicListItem> mItemList;
    private Context mCtx;
    private MusicPlayer mMusicPlayer;

    public MusicListAdapter(Context ctx, List<MusicListItem> itemList, MusicPlayer musicPlayer)
    {
        mItemList = itemList;
        mCtx = ctx;
        mMusicPlayer = musicPlayer;
    }

    @Override
    public MusicListItemHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_player_item, null);
        MusicListItemHolder rcv = new MusicListItemHolder(layoutView, mMusicPlayer);
        return rcv;
    }

    @Override
    public void onBindViewHolder(MusicListItemHolder holder, int position)
    {
        holder.songNameTV.setText(mItemList.get(position).getName());
    }

    @Override
    public int getItemCount()
    {
        return mItemList.size();
    }
}