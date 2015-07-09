/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudspace.ardrobot.util;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.whitebyte.wifihotspotutils.WifiApManager;

import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BleService extends Service {
    private final static String TAG = BleService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    public int mConnectionState = STATE_DISCONNECTED;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
//                Log.i(TAG, "Connected to GATT server, starting discovery " + mBluetoothGatt.discoverServices());
                Log.i(TAG, "Connected to GATT server, starting discovery");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    setCharacteristicNotification(mBluetoothGatt.getService(UUID.fromString(RelevantGattAttributes.IP_BROADCASTER))
                            .getCharacteristic(UUID.fromString(RelevantGattAttributes.IP_BROADCASTER_CHARACTERISTIC)), true);
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Broadcasting IP data");

                final WifiApManager apMan = new WifiApManager(BleService.this);
                apMan.setWifiApEnabled(false);

                apMan.setWifiApEnabled(true);
                getMainHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        writeCharacteristic(mBluetoothGatt.getService(UUID.fromString(RelevantGattAttributes.IP_BROADCASTER))
                                        .getCharacteristic(UUID.fromString(RelevantGattAttributes.IP_BROADCASTER_CHARACTERISTIC)),
                                apMan.getWifiApConfiguration().SSID);
                    }
                });
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().toString().equals(RelevantGattAttributes.IP_BROADCASTER_CHARACTERISTIC)) {
                    final byte[] data = characteristic.getValue();
                    if (data != null && data.length > 0) {
                        final StringBuilder stringBuilder = new StringBuilder(data.length);
                        for (byte byteChar : data) {
                            stringBuilder.append(String.format("%02X ", byteChar));
                        }
                    }
                    SettingsProvider.setIp(data, BleService.this);
                }
            }
            getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    gatt.setCharacteristicNotification(characteristic, true);
                }
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().toString().equals(RelevantGattAttributes.IP_BROADCASTER_CHARACTERISTIC)) {
                String ip = new String(characteristic.getValue());
                if (ip != SettingsProvider.getIp(BleService.this)) {
                    SettingsProvider.setIp(characteristic.getValue(), BleService.this);
                }
            }
        }
    };

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
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

        if (!isReady()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(enableBtIntent);
            }
        }
        return true;
    }

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

    protected boolean isBleReady() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *                {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *                callback.
     */
    public void connect(final String address) {
        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothAdapter == null || address == null) {
                    Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
                    return;
                }

                mBluetoothAdapter.cancelDiscovery();

                // Previously connected device.  Try to reconnect.
                if (mBluetoothDeviceAddress != null
                        && address.equals(mBluetoothDeviceAddress)
                        && mBluetoothGatt != null) {
                    Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
                    if (mBluetoothGatt.connect()) {
                        mConnectionState = STATE_CONNECTING;
                        return;
                    }
                }

                final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                if (device == null) {
                    Log.w(TAG, "Device not found.  Unable to connect.");
                    return;
                }
                // We want to directly connect to the device, so we are setting the autoConnect
                // parameter to false.
                mBluetoothGatt = device.connectGatt(BleService.this, false, mGattCallback);
                Log.d(TAG, "Trying to create a new connection.");
                mBluetoothDeviceAddress = address;
                mConnectionState = STATE_CONNECTING;
            }
        });
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothGatt == null) {
                    return;
                }
                mBluetoothGatt.close();
                mBluetoothGatt.disconnect();
                mBluetoothGatt = null;
            }
        });
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, String data) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        characteristic.setValue(data);

        Log.d(TAG, "Write - " + mBluetoothGatt.writeCharacteristic(characteristic));
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(RelevantGattAttributes.NOTIFICATION_ENABLE_ATTRIBUTE));
        descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * @return an handler with the main (ui) looper.
     */
    private Handler getMainHandler() {
        return new Handler(getMainLooper());
    }

}