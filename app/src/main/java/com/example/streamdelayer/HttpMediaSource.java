package com.example.streamdelayer;

import android.media.MediaDataSource;
import android.util.Log;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HttpMediaSource extends MediaDataSource {

    BufferedInputStream mStream = null;
    HttpURLConnection mUrlConnection = null;

    Map<String, List<String>> mHeaders = null;

    public HttpMediaSource(String url) {
        try {
            mUrlConnection = (HttpURLConnection) (new URL(url)).openConnection();
            Log.d(MainActivity.TAG, "Connection established");
            mStream = new BufferedInputStream(mUrlConnection.getInputStream());
            Log.d(MainActivity.TAG, "Got input stream");
            mHeaders = mUrlConnection.getHeaderFields();
            for(String key : mHeaders.keySet()) {
                Log.d(MainActivity.TAG, "Header key: "+key);
                for(String val : mHeaders.get(key)) {
                    Log.d(MainActivity.TAG, "Header val: "+val);
                }
            }
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
        }
    }

    Map<String, List<String>> getHeaders() {return mHeaders;}

    @Override
    public long getSize() { return Long.MAX_VALUE;}

    @Override
    public int readAt(long position, byte[] outBuffer, int offset, int size) {
        try {
            return mStream.read(outBuffer, offset, size);
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
            return 0;
        }
    }

    @Override
    public void close() {

    }
}
