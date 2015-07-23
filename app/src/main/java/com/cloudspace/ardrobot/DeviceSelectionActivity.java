package com.cloudspace.ardrobot;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.cloudspace.ardrobot.util.BleScanService;
import com.cloudspace.ardrobot.util.DividerItemDecoration;
import com.cloudspace.ardrobot.util.LeDeviceListAdapter;
import com.cloudspace.ardrobot.util.OnRecyclerViewItemClickListener;
import com.cloudspace.ardrobot.util.SettingsProvider;
import com.cloudspace.ardrobot.util.Typewriter;


public class DeviceSelectionActivity extends Activity {
    LeDeviceListAdapter mLeDeviceListAdapter;
    BleScanService mBtService;
    Typewriter status;

    @Override
    protected void onResume() {
        super.onResume();
        Intent btI = new Intent(this, BleScanService.class);
        startService(btI);
        bindService(btI, mBtConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mBtConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SettingsProvider.getEdisonAddress(this).isEmpty() &&
                !getIntent().hasExtra("force")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.device_chooser);
        status = (Typewriter) findViewById(R.id.status);
        String message = "Scanning for devices.....";
        status.animateText(message, message.length() - 5);

        mLeDeviceListAdapter = new LeDeviceListAdapter(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                return false;
            }
        });
        RecyclerView rView = ((RecyclerView) findViewById(R.id.list));
        rView.setLayoutManager(new LinearLayoutManager(this));
        rView.setItemAnimator(new DefaultItemAnimator());
        rView.setAdapter(mLeDeviceListAdapter);
        rView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        mLeDeviceListAdapter.setOnItemClickListener(new OnRecyclerViewItemClickListener<BluetoothDevice>() {
            @Override
            public void onItemClick(View view, BluetoothDevice model) {
                Toast.makeText(view.getContext(), "Setting " + model.getName() + " as your robot device.", Toast.LENGTH_LONG).show();
                SettingsProvider.setAddress(model, view.getContext());
                finish();
                startActivity(new Intent(view.getContext(), MainActivity.class));
            }
        });
    }

    private ServiceConnection mBtConnection = new ServiceConnection() {
        public ScanCallback scanBack = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                mLeDeviceListAdapter.addDevice(result.getDevice());
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        };

        public void onServiceConnected(ComponentName className, IBinder service) {
            mBtService = ((BleScanService.LocalBinder) service).getService();
            mBtService.isBound = true;
            mBtService.initialize();
            mBtService.addScanCallback(scanBack);
        }

        public void onServiceDisconnected(ComponentName className) {
            mBtService.removeScanCallback(scanBack);
            mBtService.isBound = false;
            mBtService = null;
        }
    };

}
