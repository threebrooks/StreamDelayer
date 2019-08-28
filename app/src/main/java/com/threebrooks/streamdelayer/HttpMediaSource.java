package com.threebrooks.streamdelayer;

import android.media.MediaDataSource;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpMediaSource extends MediaDataSource {

    BufferedInputStream mStream = null;
    HttpURLConnection mUrlConnection = null;
    boolean mOk = true;

    public HttpMediaSource(URL url) throws Exception {
        mUrlConnection = (HttpURLConnection)url.openConnection();
        mStream = new BufferedInputStream(mUrlConnection.getInputStream());
        mOk = true;
    }

    public boolean isOk() { return mOk;}

    @Override
    public long getSize() { return Long.MAX_VALUE;}

    @Override
    public int readAt(long position, byte[] outBuffer, int offset, int size) {
            try {
                return mStream.read(outBuffer, offset, size);
            } catch (Exception e) {
                try {
                    try{mStream.close();}catch (Exception e2) {}
                    mStream = null;
                    mUrlConnection.disconnect();
                    mUrlConnection = null;
                    Log.d(MainActivity.TAG, e.getMessage());
                    Thread.sleep(100);
                    mOk = false;
                } catch (Exception e3) {}
            }
            mOk = false;
            return -1;
    }

    @Override
    public void close() {

    }
}
