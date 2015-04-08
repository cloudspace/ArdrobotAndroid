package com.cloudspace.ardrobot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;


public abstract class AccessoryWatchingActivity extends Activity {
    UsbAccessory currentAccessory;

    public UsbAccessory getCurrentAccessory() {
        return currentAccessory;
    }

    UsbManager mUsbManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            if (action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                closeAccessory();
                handleClosedAccessory();
            }
        }
    };

    public abstract void handleClosedAccessory();

    public abstract void handleAccessory();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(UsbManager.EXTRA_ACCESSORY)) {
            currentAccessory = (UsbAccessory) intent.getExtras().get(UsbManager.EXTRA_ACCESSORY);
            handleAccessory();
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);

        registerReceiver(mUsbReceiver, filter); // system receiver

        if (currentAccessory == null) {
            UsbAccessory[] accessories = mUsbManager.getAccessoryList();
            UsbAccessory accessory = (accessories == null ? null : accessories[0]);
            if (accessory != null) {
                currentAccessory = accessory;
                handleAccessory();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onResume();
        unregisterReceiver(mUsbReceiver); // system receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUsbReceiver); // local receiver
    }

    private void closeAccessory() {
        currentAccessory = null;
    }

}
