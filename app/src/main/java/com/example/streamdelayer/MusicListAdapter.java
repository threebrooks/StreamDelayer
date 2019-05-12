package com.example.streamdelayer;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListItemHolder>
{
    private List<MusicListItem> mItemList;
    private Context mCtx;
    private StreamPlayer mStreamPlayer;

    public MusicListAdapter(Context ctx, List<MusicListItem> itemList, StreamPlayer streamPlayer)
    {
        mItemList = itemList;
        mCtx = ctx;
        mStreamPlayer = streamPlayer;
    }

    @Override
    public MusicListItemHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_player_item, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        layoutView.setBackgroundColor(ContextCompat.getColor(mCtx,R.color.l2Whiten));
        MusicListItemHolder rcv = new MusicListItemHolder(layoutView, this);
        return rcv;
    }

    @Override
    public void onBindViewHolder(MusicListItemHolder holder, int position)
    {
        holder.streamUrlET.setText(mItemList.get(position).getUrl().toString());
        holder.streamNameET.setText(mItemList.get(position).getName());
    }

    @Override
    public int getItemCount()
    {
        return mItemList.size();
    }

    public void playItem(int pos) {
        mStreamPlayer.playStream(mItemList.get(pos).getUrl());
    }
}