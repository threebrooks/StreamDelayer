package com.threebrooks.streamdelayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class StreamPlayer {
    long mStreamDelayTapMillisStart = 0;

    View mRootView = null;
    Context mCtx = null;

    AppCompatButton mStreamDelayMinus5Button = null;
    AppCompatButton mStreamDelayMinusPoint5Button = null;
    AppCompatButton mStreamDelayPlusPoint5Button = null;
    AppCompatButton mStreamDelayPlus5Button = null;
    AppCompatButton mStreamDelayTapButton = null;
    DelayCircleView mDelayCircleView = null;

    TabLayout mTabLayout = null;
    RecyclerView mPlaylistRcv = null;

    ImageButton mPlaylistStopButton = null;
    ImageButton mPlaylistAddItemButton = null;

    ArrayList<StreamListDatabase> mStreamListDBs = new ArrayList<>();
    private static String CUSTOM_DB = "Custom";
    private static int CUSTOM_DB_IDX = 0;

    View.OnClickListener mDelayButtonsCL = new  View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent delayIntent = new Intent(mCtx, PlayerService.class);
            delayIntent.setAction(PlayerService.ACTION_DELAY);
            if (v == mStreamDelayMinus5Button) {
                delayIntent.putExtra("delta", -5.0f);
                mCtx.startService(delayIntent);
            } else if (v == mStreamDelayMinusPoint5Button) {
                delayIntent.putExtra("delta", -0.5f);
                mCtx.startService(delayIntent);
            }else if (v == mStreamDelayPlusPoint5Button) {
                delayIntent.putExtra("delta", +0.5f);
                mCtx.startService(delayIntent);
            } else if (v == mStreamDelayPlus5Button) {
                delayIntent.putExtra("delta", +5.0f);
                mCtx.startService(delayIntent);
            } else if (v == mStreamDelayTapButton) {
                if (mStreamDelayTapMillisStart != 0) {
                    float secondsElapsed = (System.currentTimeMillis()-mStreamDelayTapMillisStart)/1000.0f;
                    delayIntent.putExtra("absolute", secondsElapsed);
                    mCtx.startService(delayIntent);
                    mStreamDelayTapMillisStart = 0;
                    mStreamDelayTapButton.setText("TAP");
                } else {
                    mStreamDelayTapMillisStart = System.currentTimeMillis();
                    mStreamDelayTapButton.setText("...");
                }
            }
        }
    };

    public void EditPlaylistEntry(final int pos) {
        AlertDialog.Builder alert = new AlertDialog.Builder(mCtx);
        alert.setTitle("Edit stream list item");

        try {
            final StreamListDatabase.StreamListItem playlistEntry = mStreamListDBs.get(CUSTOM_DB_IDX).getItem(pos);

            LayoutInflater inflater = (LayoutInflater) mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.playlist_edit_popup, null);
            final EditText nameET = ll.findViewById(R.id.playlistItemEditName);
            nameET.setText(playlistEntry.mName);
            final EditText urlET = ll.findViewById(R.id.playlistItemEditURL);
            urlET.setText(playlistEntry.mUrl);
            alert.setView(ll);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    try {
                        playlistEntry.mUrl = urlET.getText().toString();
                        playlistEntry.mName = nameET.getText().toString();
                        mStreamListDBs.get(CUSTOM_DB_IDX).setItem(playlistEntry, pos);
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
                        SharedPreferences.Editor prefsEditor = prefs.edit();
                        prefsEditor.putString(StreamListDatabase.CUSTOM_STREAM_LIST_PREFERENCE, mStreamListDBs.get(CUSTOM_DB_IDX).toJson());
                        prefsEditor.commit();
                        switchToTab(CUSTOM_DB_IDX);
                    } catch (Exception e) {
                        Log.d(MainActivity.TAG, e.getMessage());
                    }
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });

            alert.show();
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
        }
    }

    View.OnClickListener mPlaylistButtonsCL = new  View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mPlaylistStopButton) {
                Intent startIntent = new Intent(mCtx, PlayerService.class);
                startIntent.setAction(PlayerService.ACTION_STOP);
                mCtx.startService(startIntent);
            } else if (v == mPlaylistAddItemButton) {
                try {
                    EditPlaylistEntry(-1);
                } catch (Exception e) {
                    Log.d(MainActivity.TAG, e.getMessage());
                }
            }
        }
    };

    TabLayout.OnTabSelectedListener mTabSelectL = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            switchToTab(tab.getPosition());
        }
        @Override
        public void onTabUnselected(TabLayout.Tab tab) {}
        @Override
        public void onTabReselected(TabLayout.Tab tab) {}
    };

    private void AddTab(String tabName, JSONArray tabArray) {
        mStreamListDBs.add(new StreamListDatabase(mCtx,tabArray));
        TabLayout.Tab newTab = new TabLayout.Tab();
        newTab.setText(tabName);
        mTabLayout.addTab(newTab);
    }

    class LinkDBAsync extends AsyncTask<Void, Void, Void> {
        private Exception exception;
        protected Void doInBackground(Void... nop) {
            try {
                String jsonString = StreamListDatabase.DownloadDatabase(new URL(LINK_DB_URL));
                try {
                    JSONObject rootObj = new JSONObject(jsonString);
                    Iterator tabIt = rootObj.keys();
                    while(tabIt.hasNext()) {
                        String tabName = (String)tabIt.next();
                        JSONArray tabArray = rootObj.getJSONArray(tabName);
                        AddTab(tabName, tabArray);
                    }

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
                    String customJsonList = prefs.getString(StreamListDatabase.CUSTOM_STREAM_LIST_PREFERENCE, StreamListDatabase.EMPTY_DB);
                    AddTab(CUSTOM_DB, new JSONArray(customJsonList));
                    CUSTOM_DB_IDX = mStreamListDBs.size()-1;
                    switchToTab(CUSTOM_DB_IDX);
                } catch (Exception e) {
                    Log.d(MainActivity.TAG, e.getMessage());
                }
                return null;
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }
    }

    private void switchToTab(int idx) {
        mPlaylistRcv.setAdapter(new MusicListAdapter(mCtx, mStreamListDBs.get(idx), StreamPlayer.this));
        mPlaylistRcv.getAdapter().notifyDataSetChanged();
        mRootView.invalidate();
    }

    private static String LINK_DB_URL = "https://raw.githubusercontent.com/threebrooks/StreamDelayer/master/misc/Links.json";

    public StreamPlayer(Context ctx, View rootView) {
        mCtx = ctx;
        mRootView = rootView;

        mTabLayout = mRootView.findViewById(R.id.playlistTabLayout);
        mTabLayout.addOnTabSelectedListener(mTabSelectL);

        mPlaylistRcv = mRootView.findViewById(R.id.playlistRcv);
        mPlaylistRcv.setLayoutManager(new LinearLayoutManager(mCtx));

        mDelayCircleView = mRootView.findViewById(R.id.delayCircleView);

        mStreamDelayMinus5Button = mRootView.findViewById(R.id.streamDelayMinus5Button);
        mStreamDelayMinus5Button.setOnClickListener(mDelayButtonsCL);
        mStreamDelayMinusPoint5Button = mRootView.findViewById(R.id.streamDelayMinusPoint5Button);
        mStreamDelayMinusPoint5Button.setOnClickListener(mDelayButtonsCL);
        mStreamDelayPlusPoint5Button = mRootView.findViewById(R.id.streamDelayPlusPoint5Button);
        mStreamDelayPlusPoint5Button.setOnClickListener(mDelayButtonsCL);
        mStreamDelayPlus5Button = mRootView.findViewById(R.id.streamDelayPlus5Button);
        mStreamDelayPlus5Button.setOnClickListener(mDelayButtonsCL);
        mStreamDelayTapButton = mRootView.findViewById(R.id.streamDelayTapButton);
        mStreamDelayTapButton.setOnClickListener(mDelayButtonsCL);

        mPlaylistStopButton = mRootView.findViewById(R.id.playlistStopButton);
        mPlaylistStopButton.setOnClickListener(mPlaylistButtonsCL);
        mPlaylistAddItemButton = mRootView.findViewById(R.id.playlistAddItemButton);
        mPlaylistAddItemButton.setOnClickListener(mPlaylistButtonsCL);

        new LinkDBAsync().execute();
    }

}
