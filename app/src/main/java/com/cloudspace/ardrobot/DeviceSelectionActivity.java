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
import android.os.IBinder;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.cloudspace.ardrobot.util.BleScanService;
import com.cloudspace.ardrobot.util.LeDeviceListAdapter;
import com.cloudspace.ardrobot.util.OnRecyclerViewItemClickListener;
import com.cloudspace.ardrobot.util.SettingsProvider;


public class DeviceSelectionActivity extends Activity {
    LeDeviceListAdapter mLeDeviceListAdapter;
    BleScanService mBtService;

    @Override
    protected void onStart() {
        super.onStart();
        Intent btI = new Intent(this, BleScanService.class);
        startService(btI);
        bindService(btI, mBtConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mBtConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_chooser);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        RecyclerView rView = ((RecyclerView) findViewById(R.id.list));
        rView.setLayoutManager(new LinearLayoutManager(this));
        rView.setItemAnimator(new DefaultItemAnimator());
        rView.setAdapter(mLeDeviceListAdapter);
        mLeDeviceListAdapter.setOnItemClickListener(new OnRecyclerViewItemClickListener<BluetoothDevice>() {
            @Override
            public void onItemClick(View view, BluetoothDevice model) {
                Toast.makeText(view.getContext(), "Setting " + model.getName() + " as your robot device.", Toast.LENGTH_LONG).show();
                SettingsProvider.setAddress(model.getAddress(), view.getContext());
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

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mBtService = ((BleScanService.LocalBinder) service).getService();
            mBtService.initialize();
            mBtService.addScanCallback(scanBack);

        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mBtService.removeScanCallback(scanBack);
            mBtService = null;
        }
    };

}
