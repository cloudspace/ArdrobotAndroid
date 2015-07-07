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
import android.view.View;
import android.widget.ImageButton;

import com.cloudspace.ardrobot.util.BleService;
import com.cloudspace.ardrobot.util.SettingsProvider;
import com.cloudspace.ardrobot.util.Typewriter;


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
                        } else if (mBtService.mConnectionState == mBtService.STATE_DISCONNECTED) {
                            status.setTextInternally("Unable to connect to device : " + SettingsProvider.getEdisonName(MainActivity.this));
                        } else if (mBtService.mConnectionState == mBtService.STATE_CONNECTED) {
                            status.setTextInternally("Connected to device : " + SettingsProvider.getEdisonName(MainActivity.this));
                        }

                        h.postDelayed(this, 1000);
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
            mBtService = null;
        }
    };
    private Typewriter status;
    private ImageButton action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (Typewriter) findViewById(R.id.status);
        String message = "Connecting to " + SettingsProvider.getEdisonName(this) + ".....";
        status.animateText(message, message.length() - 5);
        action = (ImageButton) findViewById(R.id.action);
        action.setVisibility(SettingsProvider.getEdisonAddress(this).isEmpty() ? View.GONE : View.VISIBLE);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(view.getContext(), DeviceSelectionActivity.class);
                i.putExtra("force", true);
                startActivity(i);
                finish();
            }
        });
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
