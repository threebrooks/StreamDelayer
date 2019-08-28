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
    boolean mKeepTrying = true;
    URL mUrl = null;

    public HttpMediaSource(URL url) {
        mUrl = url;
        mKeepTrying = true;
    }

    public void openConnection() throws Exception {
        mUrlConnection = (HttpURLConnection)mUrl.openConnection();
        mStream = new BufferedInputStream(mUrlConnection.getInputStream());
        //mStream.skip(mStream.available()); // Skip to edge
        byte[] dummyBuffer = new byte[1024];
        long startTime = System.currentTimeMillis();
        long skipped = 0;
        do {
            skipped += mStream.read(dummyBuffer, 0, dummyBuffer.length);
        } while ((System.currentTimeMillis()-startTime) < 1000);
        Log.d(MainActivity.TAG, "Got input stream, skipped "+skipped+" lead-in");
    }

    public void stop() {
        mKeepTrying = false;
        try {
            mStream.close();
        } catch (Exception e) {}
    }

    @Override
    public long getSize() { return Long.MAX_VALUE;}

    @Override
    public int readAt(long position, byte[] outBuffer, int offset, int size) {
        while(mKeepTrying) {
            try {
                if (mUrlConnection == null) {
                    openConnection();
                }
                int read = mStream.read(outBuffer, offset, size);
                if (read == -1 && mKeepTrying) return 0;
                return read;
            } catch (Exception e) {
                try {
                    try{mStream.close();}catch (Exception e2) {}
                    mStream = null;
                    mUrlConnection.disconnect();
                    mUrlConnection = null;
                    Log.d(MainActivity.TAG, e.getMessage());
                    Thread.sleep(100);
                } catch (Exception e3) {}
            }
        }
        return -1;
    }

    @Override
    public void close() {

    }
}
