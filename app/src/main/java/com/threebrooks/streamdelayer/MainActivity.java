package com.threebrooks.streamdelayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static String APP_NAME = "StreamDelayer";
    public static String TAG = "StreamDelayer";

    Context mCtx = null;
    StreamPlayer mStreamPlayer = null;
    PlayerService mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCtx = this.getApplicationContext();
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LinearLayout mainLL = findViewById(R.id.contentMainLL);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mainLL.setOrientation(LinearLayout.VERTICAL);
            {
                LinearLayout liveStreamerLL = findViewById(R.id.liveStreamerLL);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) liveStreamerLL.getLayoutParams();
                lp.height = 0;
                lp.width = LinearLayout.LayoutParams.MATCH_PARENT;
                liveStreamerLL.setLayoutParams(lp);
            }
            {
                View dividerView = findViewById(R.id.dividerView);
                ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) dividerView.getLayoutParams();
                lp.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());;
                lp.width = LinearLayout.LayoutParams.MATCH_PARENT;
                dividerView.setLayoutParams(lp);
            }
            {
                LinearLayout musicPlayerLL = findViewById(R.id.musicPlaylistLL);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) musicPlayerLL.getLayoutParams();
                lp.height = 0;
                lp.width = LinearLayout.LayoutParams.MATCH_PARENT;
                musicPlayerLL.setLayoutParams(lp);
            }
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return;
        }

        if (mStreamPlayer == null) mStreamPlayer = new StreamPlayer(this, findViewById(R.id.topLevelCL));

        final Intent intent = getIntent();
        String intentType = intent.getType();
        if (intentType != null && (intentType.equals("audio/mpegurl") ||
                intentType.equals("application/vnd.apple.mpegurl") ||
                intentType.equals("application/x-mpegurl"))) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mStreamPlayer.addM3U(intent.getData());
                }
            }, 1000);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, PlayerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
            mService = binder.getService();
            mStreamPlayer.mDelayCircleView.setPlayerService(mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_links) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://github.com/threebrooks/StreamDelayer"));
                startActivity(i);
            } catch (Exception e) {
                Toast(this, "Could not open link");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static void Toast(Context ctx, String t) {
        Toast toast = Toast.makeText(ctx, t, Toast.LENGTH_LONG);
        View view = toast.getView();
        view.getBackground().setColorFilter(ContextCompat.getColor(ctx, R.color.purple200Grey), PorterDuff.Mode.SRC_IN);
        TextView text = view.findViewById(android.R.id.message);
        text.setBackgroundColor(ContextCompat.getColor(ctx, R.color.purple200Grey));
        text.setTextColor(ContextCompat.getColor(ctx, R.color.fontPrimary));
        toast.show();
    }
}
