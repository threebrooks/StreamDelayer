package com.example.streamdelayer;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayer {

    RecyclerView mRecyclerView = null;
    Context mCtx = null;
    List<MusicListItem> mSongs = null;

    public MusicPlayer(Context ctx, View viewRoot) {
        mCtx = ctx;
        mRecyclerView = viewRoot.findViewById(R.id.music_player_rcv);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(ctx));
        mSongs = getAllAudios();
        MusicListAdapter rcAdapter = new MusicListAdapter(ctx, mSongs, this);
        mRecyclerView.setAdapter(rcAdapter);
    }

    public void playSong(int index) {
        Log.d(MainActivity.TAG, mSongs.get(index).getUrl());
    }

    public List<MusicListItem> getAllAudios() {
        List<MusicListItem> files = new ArrayList<MusicListItem>();
        String[] projection = { MediaStore.Audio.AudioColumns.DATA ,MediaStore.Audio.Media.DISPLAY_NAME};
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        try {
            Cursor intCursor = mCtx.getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, selection, null, null);
            intCursor.moveToFirst();
            do{
                files.add(
                        new MusicListItem(
                                intCursor.getString(intCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)),
                                intCursor.getString(intCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))));
            }while(intCursor.moveToNext());
            intCursor.close();

            Cursor extCursor = mCtx.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null);
            extCursor.moveToFirst();
            do{
                files.add(
                        new MusicListItem(
                                extCursor.getString(extCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)),
                                extCursor.getString(extCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))));
            }while(extCursor.moveToNext());
            extCursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }
}
