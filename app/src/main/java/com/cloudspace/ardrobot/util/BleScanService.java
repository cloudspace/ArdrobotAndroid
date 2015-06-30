package com.cloudspace.ardrobot.util;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.cloudspace.ardrobot.AccessoryActivity;

import java.util.List;

public class BleScanService extends Service {

    private final static String TAG = BleScanService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();

    private BluetoothManager mBluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;

    ScanCallback scanBack = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "Found ble device " + result.getDevice().getName() + " " + result.getDevice().getAddress());
            broadcastOnDeviceFound(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public class LocalBinder extends Binder {
        public BleScanService getService() {
            return BleScanService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long timeToScan = 30000000;
        startScan(timeToScan);

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Initializes a reference to the local bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                return false;
            }
        }

        Log.d(TAG, "Initialzed scanner.");
        return true;
    }

    /**
     * Checks if bluetooth is correctly set up.
     *
     * @return
     */
    protected boolean isInitialized() {
        return mBluetoothManager != null && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    /**
     * Checks if ble is ready and bluetooth is correctly setup.
     *
     * @return
     */
    protected boolean isReady() {
        return isInitialized() && isBleReady();
    }

    /**
     * Checks if the device is ble ready.
     *
     * @return
     */
    protected boolean isBleReady() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Broadcasts a message with the given device.
     *

     */
    protected void broadcastOnDeviceFound(ScanResult res) {
        if (res.getDevice().getAddress().equals(AccessoryActivity.EDISON_ADDRESS)) {
            stopScan();
            Intent intent = new Intent("action_device");
            intent.putExtra("data", res);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    /**
     * Starts the bluetooth low energy scan It scans at least the
     * delayStopTimeInMillis.
     *
     * @param delayStopTimeInMillis the duration of the scan
     * @return <code>true</code> if the scan is successfully started.
     */
    public boolean startScan(long delayStopTimeInMillis) {
        if (!isReady())
            return false;

        if (delayStopTimeInMillis <= 0) {
            Log.w(TAG, "Did not start scanning with automatic stop delay time of " + delayStopTimeInMillis);
            return false;
        }

        Log.d(TAG, "Auto-Stop scan after " + delayStopTimeInMillis + " ms");
        getMainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Stopped scan.");
                stopScan();
            }
        }, delayStopTimeInMillis);
        return startScan();
    }

    /**
     * @return an handler with the main (ui) looper.
     */
    private Handler getMainHandler() {
        return new Handler(getMainLooper());
    }

    /**
     * Starts the bluetooth low energy scan. It scans without time limit.
     *
     * @return <code>true</code> if the scan is successfully started.
     */
    public boolean startScan() {
        if (!isReady())
            return false;
        if (mBluetoothAdapter != null) {
            Log.d(TAG, "Started scan.");
            mBluetoothAdapter.getBluetoothLeScanner().startScan(scanBack);
        } else {
            Log.d(TAG, "BluetoothAdapter is null.");
            return false;
        }

        return true;
    }

    /**
     * Stops the bluetooth low energy scan.
     */
    public void stopScan() {
        if (!isReady())
            return;

        if (mBluetoothAdapter != null)
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanBack);
        else {
            Log.d(TAG, "BluetoothAdapter is null.");
        }
    }

}