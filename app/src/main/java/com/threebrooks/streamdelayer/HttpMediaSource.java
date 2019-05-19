package com.threebrooks.streamdelayer;

import android.media.MediaDataSource;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HttpMediaSource extends MediaDataSource {

    BufferedInputStream mStream = null;
    HttpURLConnection mUrlConnection = null;
    boolean mOk = false;

    public HttpMediaSource(URL url) throws Exception {
        mUrlConnection = (HttpURLConnection)url.openConnection();
        Log.d(MainActivity.TAG, "Connection established");
        mStream = new BufferedInputStream(mUrlConnection.getInputStream());
        Log.d(MainActivity.TAG, "Got input stream");
        mOk = true;
    }

    public boolean ok() {return mOk;}

    @Override
    public long getSize() { return Long.MAX_VALUE;}

    @Override
    public int readAt(long position, byte[] outBuffer, int offset, int size) {
        try {
            return mStream.read(outBuffer, offset, size);
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
            mOk = false;
            return 0;
        }
    }

    @Override
    public void close() {

    }
}
