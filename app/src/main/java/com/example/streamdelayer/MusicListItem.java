package com.example.streamdelayer;

public class MusicListItem
{
    private String mSongName;
    private String mUrl;

    public MusicListItem(String name, String url)
    {
        mSongName = name;
        mUrl = url;
    }

    public String getName()
    {
        return mSongName;
    }

    public void setName(String name)
    {
        mSongName = name;
    }

    public String getUrl()
    {
        return mUrl;
    }

    public void setUrl(String url)
    {
        mUrl = url;
    }
}