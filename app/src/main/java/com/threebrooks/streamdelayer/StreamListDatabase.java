package com.threebrooks.streamdelayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

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
    public final String STREAM_LIST_PREFERENCE = "stream_list_preference";

    private String DB_INIT = "[ {\"name\": \"BvB NetRadio\", \"url\": \"https://bvb-live.cast.addradio.de/bvb/live/mp3/high\"} ]";

    public StreamListDatabase(Context ctx) {
        mCtx = ctx;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
        String jsonList = prefs.getString(STREAM_LIST_PREFERENCE,DB_INIT);
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        String jsonString = "[ ";
        for(int idx = 0; idx < mDB.length(); idx++) {
            JSONObject obj = mDB.getJSONObject(idx);
            jsonString += "{\"name\": \""+obj.getString("name")+"\", \"url\": \""+obj.getString("url")+"\"}";
            if (idx != mDB.length()-1) jsonString += ", ";
        }
        jsonString += " ]";
        prefsEditor.putString(STREAM_LIST_PREFERENCE, jsonString);
        prefsEditor.commit();
        return pos;

    }

    public int getItemCount() { return mDB.length();}
}
