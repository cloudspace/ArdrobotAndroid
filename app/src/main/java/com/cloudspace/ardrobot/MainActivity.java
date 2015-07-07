package com.cloudspace.ardrobot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.cloudspace.ardrobot.util.BleService;
import com.cloudspace.ardrobot.util.BootReciever;
import com.cloudspace.ardrobot.util.SettingsProvider;


public class MainActivity extends Activity {

    private BleService mBtService;
    int retryAttempts = 0;

    private ServiceConnection mBtConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mBtService = ((BleService.LocalBinder) service).getService();
            mBtService.initialize();

            Log.d("CONNECTED TO BT SERVCE", "NOW");
            String address = SettingsProvider.getEdisonAddress(MainActivity.this);
            if (!address.isEmpty()) {
                mBtService.connect(address);
                final Handler h = new Handler();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (mBtService.mConnectionState == mBtService.STATE_CONNECTING) {
                            retryAttempts = retryAttempts + 1;
                            if (retryAttempts >= 60) {
                                mBtService.mConnectionState = mBtService.STATE_DISCONNECTED;
                            }
                            h.postDelayed(this, 1000);
                        } else if (mBtService.mConnectionState == mBtService.STATE_DISCONNECTED) {
                            sendBroadcast(new Intent(MainActivity.this, BootReciever.class));
                        }
                    }
                };

                h.post(r);
            } else {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setTitle("No robot specified")
                        .setMessage("On the next screen, please select the robot to use with this app.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivity(new Intent(MainActivity.this, DeviceSelectionActivity.class));
                            }
                        }).show();

            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mBtService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public void onStart() {
        super.onStart();
        bindServices();
    }

    private void bindServices() {
        bindService(new Intent(this, BleService.class), mBtConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        unbindService(mBtConnection);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService();
    }

}
