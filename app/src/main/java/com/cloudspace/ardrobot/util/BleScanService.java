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
import android.util.Log;

import com.cloudspace.ardrobot.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class BleScanService extends Service {
    boolean isScanning = false, isBound = false;
    private final static String TAG = BleScanService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();

    private BluetoothManager mBluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;
    ArrayList<ScanCallback> listeners = new ArrayList();

    public void addScanCallback(ScanCallback cb) {
        listeners.add(cb);
    }

    public void removeScanCallback(ScanCallback cb) {
        listeners.remove(cb);
    }

    ScanCallback scanBack = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            for (ScanCallback cb : listeners) {
                if (cb != this) {
                    cb.onScanResult(callbackType, result);
                }
            }
            super.onScanResult(callbackType, result);
            broadcastOnDeviceFound(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanCallback cb : listeners) {
                if (cb != this) {
                    cb.onBatchScanResults(results);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            for (ScanCallback cb : listeners) {
                if (cb != this) {
                    cb.onScanFailed(errorCode);
                }
            }
        }
    };

    public class LocalBinder extends Binder {
        public BleScanService getService() {
            return BleScanService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        isBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isBound = false;
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long timeToScan = 1000 * 60 * 2;
        startScan(timeToScan);

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Initializes a reference to the local bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        if (!listeners.contains(scanBack)) {
            listeners.add(scanBack);
        }
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

        if (!isReady()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(enableBtIntent);

                final Handler h = new Handler();
                Runnable checkRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!isReady()) {
                            h.postDelayed(this, 1000);
                        } else {
                            startScan();
                        }
                    }
                };

                h.post(checkRunnable);
            }
        }
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
     */
    protected void broadcastOnDeviceFound(ScanResult res) {
        if (!SettingsProvider.getEdisonAddress(this).isEmpty() &&
                SettingsProvider.getEdisonAddress(this).equals(res.getDevice().getAddress())) {
            stopScan();
            Intent i = new Intent(BleScanService.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            i.putExtra("data", res);
            startActivity(i);
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
        if (!isReady() || isScanning)
            return false;

        if (delayStopTimeInMillis <= 0) {
            Log.w(TAG, "Did not start scanning with automatic stop delay time of " + delayStopTimeInMillis);
            return false;
        }

        Log.d(TAG, "Auto-Stop scan after " + delayStopTimeInMillis + " ms");
        getMainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
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
        if (!isReady() || isScanning)
            return false;
        if (mBluetoothAdapter != null) {
            Log.d(TAG, "Started scan.");
            mBluetoothAdapter.getBluetoothLeScanner().startScan(scanBack);
            isScanning = true;
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
        if (!isReady() || isBound)
            return;
        if (mBluetoothAdapter != null) {
            Log.d(TAG, "Stopped scan.");
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanBack);
            isScanning = false;
        } else {
            Log.d(TAG, "BluetoothAdapter is null.");
        }
    }

}