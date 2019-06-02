package com.threebrooks.streamdelayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

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

    public StreamListDatabase(Context ctx, JSONArray db) {
        mCtx = ctx;
        mDB = db;
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

    public void setItem(StreamListItem item, int pos)  throws Exception {
        if (pos < 0) {
            pos = getItemCount();
        }
        mDB.put(pos, item.toJsonObject());
    }

    public void deleteItem(int pos) {
        mDB.remove(pos);
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
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataInputStream stream = new DataInputStream(url.openStream());
            byte[] buffer = new byte[1024];
            int read;
            while((read = stream.read(buffer)) != -1) {
                bos.write(buffer,0,read);
            }
            stream.close();
            return bos.toString("UTF-8");
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return EMPTY_DB;
    }

    public static String CUSTOM_STREAM_LIST_PREFERENCE = "custom_stream_list_preference";
}
