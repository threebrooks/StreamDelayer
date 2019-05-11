package com.example.streamdelayer;

import android.net.Uri;

public class MusicListItem
{
    private String mSongName;
    private Uri mUri;

    public MusicListItem(String name, Uri uri)
    {
        mSongName = name;
        mUri = uri;
    }

    public String getName()
    {
        return mSongName;
    }

    public void setName(String name)
    {
        mSongName = name;
    }

    public Uri getUrl()
    {
        return mUri;
    }

    public void setUri(Uri uri)
    {
        mUri = uri;
    }
}