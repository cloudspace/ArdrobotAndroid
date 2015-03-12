package com.cloudspace.ardrobot;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.cloudspace.rosserial_android.ROSSerialADK;
import com.cloudspace.rosserial_java.NodeConnectionUtils;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import std_msgs.Int8;

public class ArduinoBlinkLEDActivity extends RosActivity {
    public static final boolean D = BuildConfig.DEBUG; // This is automatically set when building
    private static final String TAG = "ArduinoBlinkLEDActivity"; // TAG is used to debug in Android logcat console
    private static final String ACTION_USB_PERMISSION = "com.cloudspace.ardrobot.action.USB_PERMISSION";

    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    TextView connectionStatus;
    private String mMasterUri;
    ConnectedNode connectedNode;
    ROSSerialADK adk;
    Publisher<Int8> publisher;

    public ArduinoBlinkLEDActivity(String notificationTicker, String notificationTitle) {
        super("", "");
    }

    public ArduinoBlinkLEDActivity() {
        super("", "");

    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        mMasterUri = getMasterUri().toString();
        NodeConfiguration config = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        nodeMainExecutor.execute(connectionUtils, config);
    }

    NodeConnectionUtils connectionUtils = new NodeConnectionUtils("node", new NodeConnectionUtils.OnNodeConnectedListener() {
        @Override
        public void onNodeConnected(ConnectedNode node) {
            connectedNode = node;
            publisher = connectedNode.newPublisher("led_state", std_msgs.Int8._TYPE);
            attemptToSetAdk();
        }
    });


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                        openAccessory(accessory);
                    else {
                        if (D)
                            Log.d(TAG, "Permission denied for accessory " + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(mAccessory))
                    closeAccessory();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        connectionStatus = (TextView) findViewById(R.id.connectionStatus);

        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAccessory != null) {
            setConnectionStatus(true);
            return;
        }

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory))
                openAccessory(accessory);
            else {
                setConnectionStatus(false);
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            setConnectionStatus(false);
            if (D)
                Log.d(TAG, "mAccessory is null");
        }
    }

    @Override
    public void onBackPressed() {
        if (mAccessory != null) {
            finish();
        } else
            finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAccessory();
        unregisterReceiver(mUsbReceiver);
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

            setConnectionStatus(true);
            attemptToSetAdk();
            if (D)
                Log.d(TAG, "Accessory opened");
        } else {
            setConnectionStatus(false);
            if (D)
                Log.d(TAG, "Accessory open failed");
        }
    }

    private void setConnectionStatus(boolean connected) {
        connectionStatus.setText(connected ? "Connected" : "Disconnected");
    }

    private void closeAccessory() {
        setConnectionStatus(false);

        try {
            if (mInputStream != null)
                mInputStream.close();
        } catch (Exception ignored) {
        } finally {
            mInputStream = null;
        }
        try {
            if (mOutputStream != null)
                mOutputStream.close();
        } catch (Exception ignored) {
        } finally {
            mOutputStream = null;
        }
        try {
            if (mFileDescriptor != null)
                mFileDescriptor.close();
        } catch (IOException ignored) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    public void blinkLED(View v) {
        Int8 msg = publisher.newMessage();
        byte payload = (byte) ((0xFF) & ((((ToggleButton) v).isChecked()) ? 1 : 0));
        msg.setData(payload);
        publisher.publish(msg);
    }

    private void attemptToSetAdk() {
        if (connectedNode != null && mAccessory != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adk = new ROSSerialADK(ArduinoBlinkLEDActivity.this, connectedNode, mAccessory, mFileDescriptor, mInputStream, mOutputStream);
                }
            });
        }
    }
}