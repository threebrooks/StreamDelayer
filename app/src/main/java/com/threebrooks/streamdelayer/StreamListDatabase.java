package com.threebrooks.streamdelayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class StreamListDatabase {

    public class StreamListItem {
        String mUrl = "URL";
        String mName = "Name";

        public JSONObject toJsonObject() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", mName);
                obj.put("url", mUrl);
            } catch (Exception e) {
                Log.d(MainActivity.TAG, e.getMessage());
            }
            return obj;
        }
    }

    Context mCtx = null;
    JSONArray mDB = null;

    public StreamListDatabase(Context ctx, String jsonList) {
        mCtx = ctx;
        try {
            mDB = new JSONArray(jsonList);
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
        }
    }

    public StreamListItem getItem(int pos) throws Exception {
        StreamListItem sli = new StreamListItem();
        if (pos >= 0) {
            JSONObject item = mDB.getJSONObject(pos);
            sli.mName = item.getString("name");
            sli.mUrl = item.getString("url");
        }
        return sli;
    }

    public int setItem(StreamListItem item, int pos)  throws Exception {
        if (pos < 0) {
            pos = getItemCount();
        }
        mDB.put(pos, item.toJsonObject());
        return pos;
    }

    String toJson()  throws Exception {
        String jsonString = "[ ";
        for(int idx = 0; idx < mDB.length(); idx++) {
            JSONObject obj = mDB.getJSONObject(idx);
            jsonString += "{\"name\": \""+obj.getString("name")+"\", \"url\": \""+obj.getString("url")+"\"}";
            if (idx != mDB.length()-1) jsonString += ", ";
        }
        jsonString += " ]";
        return jsonString;
    }

    public int getItemCount() { return mDB.length();}

    public static String EMPTY_DB = "[ ]";

    public static String DownloadDatabase(URL url) {
        try {
            URLConnection conn = url.openConnection();
            int contentLength = conn.getContentLength();
            DataInputStream stream = new DataInputStream(url.openStream());
            byte[] buffer = new byte[contentLength];
            stream.readFully(buffer);
            stream.close();
            return new String(buffer, "UTF-8");
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return EMPTY_DB;
    }
}
