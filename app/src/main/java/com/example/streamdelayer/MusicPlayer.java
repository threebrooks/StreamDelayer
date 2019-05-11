package com.example.streamdelayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MusicPlayer {

    public final static int MUSIC_FOLDER_INTENT = 56783;

    RecyclerView mRecyclerView = null;
    Activity mAct = null;
    List<MusicListItem> mSongs = null;

    ImageButton mPlayButton = null;
    ImageButton mFolderButton = null;

    MediaPlayer mMediaPlayer = null;

    public MusicPlayer(Activity act, View viewRoot) {
        mAct = act;
        mRecyclerView = viewRoot.findViewById(R.id.music_player_rcv);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mAct));

        mPlayButton = viewRoot.findViewById(R.id.musicPlayButton);
        mPlayButton.setOnClickListener(mButtonOCL);
        mFolderButton = viewRoot.findViewById(R.id.musicFolderButton);
        mFolderButton.setOnClickListener(mButtonOCL);

        mMediaPlayer = new MediaPlayer();
    }

    View.OnClickListener mButtonOCL = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mFolderButton) {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                mAct.startActivityForResult(Intent.createChooser(i, "Choose directory"), MUSIC_FOLDER_INTENT);
            } else if (v == mPlayButton) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                    mMediaPlayer.reset();
                    mPlayButton.setImageResource(mAct.getResources().getIdentifier("@android:drawable/ic_media_play", null, null));
                }
            }
        }
    };

    public void setMusicFolder(Uri uri) {
        Log.d(MainActivity.TAG, "setMusicFolder: "+uri.toString());
        mSongs = getAllAudios(uri);
        MusicListAdapter rcAdapter = new MusicListAdapter(mAct, mSongs, this);
        mRecyclerView.setAdapter(rcAdapter);
    }


    public void playSong(int index) throws Exception {
        Log.d(MainActivity.TAG, mSongs.get(index).getUrl().toString());
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mMediaPlayer.setDataSource(mAct, mSongs.get(index).getUrl());
        mMediaPlayer.setOnPreparedListener(mMediaPlayerOPL);
        mMediaPlayer.prepareAsync();
        mMediaPlayer.start();
    }

    MediaPlayer.OnPreparedListener mMediaPlayerOPL = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer player) {
            player.start();
            mPlayButton.setImageResource(mAct.getResources().getIdentifier("@android:drawable/ic_media_stop", null, null));
        }
    };

    public List<MusicListItem> getAllAudios(Uri uri) {
        List<MusicListItem> files = new ArrayList<MusicListItem>();
        DocumentFile documentFile = DocumentFile.fromTreeUri(mAct, uri);
        for (DocumentFile file : documentFile.listFiles()) {
            if(file.isFile() && file.getName().endsWith(".mp3")){
                files.add(new MusicListItem(file.getName(), file.getUri()));
            }
        }
        return files;
    }
}
