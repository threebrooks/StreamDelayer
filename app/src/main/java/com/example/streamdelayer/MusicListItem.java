package com.example.streamdelayer;

import java.net.URL;

public class MusicListItem
{
    private String mStreamName;
    private URL mUrl;

    public MusicListItem(String name, URL url)
    {
        mStreamName = name;
        mUrl = url;
    }

    public String getName()
    {
        return mStreamName;
    }

    public void setName(String name)
    {
        mStreamName = name;
    }

    public URL getUrl()
    {
        return mUrl;
    }

    public void setUri(URL url)
    {
        mUrl = url;
    }
}