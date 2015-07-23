package com.cloudspace.ardrobot;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ViewSwitcher;

import com.cloudspace.ardrobot.util.BleScanService;
import com.cloudspace.ardrobot.util.BleService;
import com.cloudspace.ardrobot.util.MarginDecoration;
import com.cloudspace.ardrobot.util.OnRecyclerViewItemClickListener;
import com.cloudspace.ardrobot.util.OptionsAdapter;
import com.cloudspace.ardrobot.util.SettingsProvider;
import com.cloudspace.ardrobot.util.Typewriter;
import com.getbase.floatingactionbutton.FloatingActionButton;


public class MainActivity extends Activity {
    boolean isAnimatingConnection = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private BleService mBtService;
    int retryAttempts = 0;
    OptionsAdapter a;

    Handler h = new Handler();
    Runnable r = new Runnable() {
        @Override
        public void run() {
            actionRefresh.setVisibility(
                    ((mBtService.mConnectionState == BleService.STATE_DISCONNECTED) || retryAttempts >= 44)
                            ? View.VISIBLE : View.GONE);
            ViewSwitcher vs = (ViewSwitcher) findViewById(R.id.viewSwitcher);
            if (mBtService.mConnectionState == mBtService.STATE_CONNECTING) {
                Log.d("BLE CONNECTION", mBtService.mConnectionState + " attempt : " + retryAttempts);
                retryAttempts = retryAttempts + 1;
                animateConnecting();
            } else if (mBtService.mConnectionState == mBtService.STATE_DISCONNECTED) {
                isAnimatingConnection = false;
                status.setTextInternally("Unable to connect to device : " + SettingsProvider.getEdisonName(MainActivity.this));
                if (vs.getDisplayedChild() == 1) {
                    vs.setDisplayedChild(0);
                }
            } else if (mBtService.mConnectionState == mBtService.STATE_CONNECTED) {
                isAnimatingConnection = false;
                status.setTextInternally("Connected to " + SettingsProvider.getEdisonName(MainActivity.this) + " - " + SettingsProvider.getIp(MainActivity.this));
                if (vs.getDisplayedChild() == 0) {
                    vs.setDisplayedChild(1);
                }
            }

            h.postDelayed(this, 1000);
        }
    };

    private void animateConnecting() {
        if (!isAnimatingConnection) {
            isAnimatingConnection = true;
            String message = "Connecting to " + SettingsProvider.getEdisonName(MainActivity.this) + ".....";
            status.animateText(message, message.length() - 5);
        }
    }

    private ServiceConnection mBtConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mBtService = ((BleService.LocalBinder) service).getService();
            mBtService.initialize();

            connectToDevice();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBtService = null;
        }
    };

    private void connectToDevice() {
        if (mBtService == null) {
            return;
        }

        String address = SettingsProvider.getEdisonAddress(MainActivity.this);
        mBtService.connect(address);
        retryAttempts = 0;
        h.removeCallbacks(r);
        h.post(r);
    }

    private Typewriter status;
    private FloatingActionButton actionPick, actionRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (Typewriter) findViewById(R.id.status);
        animateConnecting();

        actionPick = (FloatingActionButton) findViewById(R.id.action_pick);
        actionPick.setVisibility(SettingsProvider.getEdisonAddress(this).isEmpty() ? View.GONE : View.VISIBLE);
        actionPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(view.getContext(), DeviceSelectionActivity.class);
                i.putExtra("force", true);
                startActivity(i);
                finish();
            }
        });

        actionRefresh = (FloatingActionButton) findViewById(R.id.action_refresh);
        actionRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToDevice();
            }
        });

        a = new OptionsAdapter();
        a.setOnItemClickListener(new OnRecyclerViewItemClickListener<OptionsAdapter.Option>() {
            @Override
            public void onItemClick(View view, OptionsAdapter.Option model) {
                model.handleClick(view.getContext());
            }
        });

        GridLayoutManager manager = new GridLayoutManager(this, 2);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return a.getSpanSize(position);
            }
        });

        RecyclerView list = (RecyclerView) findViewById(
                R.id.list);
        list.addItemDecoration(new MarginDecoration(this));
        list.setHasFixedSize(true);
        list.setLayoutManager(manager);
        list.setAdapter(a);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindServices();
        connectToDevice();
    }

    private void bindServices() {
        bindService(new Intent(this, BleService.class), mBtConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        unbindService(mBtConnection);
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService();
        startService(new Intent(this, BleScanService.class));
        h.removeCallbacks(r);
    }

}
