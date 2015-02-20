package com.cloudspace.ardrobot;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.cloudspace.rosserial.NodeConnectionUtils;
import com.cloudspace.rosserial.ROSSerialADK;
import com.cloudspace.rosserial.ROSSerialADKService;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;


public class SerialActivity extends RosActivity {
    private static final String TAG = "Ardrobot";

    private static final String ACTION_USB_PERMISSION = "com.cloudspace.ardrobot.action.USB_PERMISSION";
    ROSSerialADKService.LocalBinder mBinder;
    //    ParcelFileDescriptor mFileDescriptor;
//    FileInputStream  mInputStream;
//    FileOutputStream mOutputStream;
    UsbAccessory mAccessory;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    ConnectedNode connectedNode;
    ROSSerialADK adk;
    String mMasterUri;
    
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        Log.d(TAG, "permission denied for accessory "
                                + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
//                if (accessory != null && accessory.equals(mAccessory)) {
//                    closeAccessory();
//                }
            }
        }
    };

    public SerialActivity(String notificationTicker, String notificationTitle) {
        super("", "");
    }

    public SerialActivity() {
        super("", "");
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        if (mConnection != null) {
//            unbindService(mConnection);
        }
        super.onDestroy();
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
            SerialActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBinder != null) {
                        setAdk(mBinder.setConnectedNode(connectedNode, mAccessory));
                        Toast.makeText(SerialActivity.this, connectedNode.getName().toString() + " node connected", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    });

    @Override
    public void onResume() {
        super.onResume();

//        if (mInputStream != null && mOutputStream != null) {
//            return;
//        }

        if (mBinder == null || mBinder.getADK() == null) {

            UsbAccessory[] accessories = mUsbManager.getAccessoryList();
            UsbAccessory accessory = (accessories == null ? null : accessories[0]);
            if (accessory != null) {
                if (mUsbManager.hasPermission(accessory)) {
                    openAccessory(accessory);
                } else {
                    synchronized (mUsbReceiver) {
                        if (!mPermissionRequestPending) {
                            mUsbManager.requestPermission(accessory,
                                    mPermissionIntent);
                            mPermissionRequestPending = true;
                        }
                    }
                }
            } else {
                Log.d(TAG, "mAccessory is null");
            }
        }

    }

    private void openAccessory(UsbAccessory accessory) {
        if (mAccessory != null || mMasterUri == null) {
            return;
        }
//        mFileDescriptor = mUsbManager.openAccessory(accessory);
//        if (mFileDescriptor != null) {
        mAccessory = accessory;
//            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
//            mInputStream = new FileInputStream(fd);
//            mOutputStream = new FileOutputStream(fd);
//            Log.d(TAG, "accessory opened");
//        } else {
//            Log.d(TAG, "accessory open fail");
//        }


        Intent i = new Intent(this, ROSSerialADKService.class);

        i.putExtra("ROS_MASTER_URI", mMasterUri);
//        i.putExtra("name", "name");

        startService(i);
        bindService(i, mConnection, 0);

    }

    
//    private void closeAccessory() {
//        try {
//            if (mFileDescriptor != null) {
//                mFileDescriptor.close();
//            }
//
//            if (mOutputStream != null) {
//                mOutputStream.close();
//            }
//
//            if (mInputStream != null) {
//                mInputStream.close();
//            }
//        } catch (IOException e) {
//        } finally {
//            mFileDescriptor = null;
//            mInputStream = null;
//            mOutputStream = null;
//            mAccessory = null;
//        }
//    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mAccessory != null) {
            return mAccessory;
        } else {
            return super.onRetainNonConfigurationInstance();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        if (getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }

    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }


        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "Binder service connected");
            mBinder = (ROSSerialADKService.LocalBinder) service;
        }
    };

    public void setAdk(ROSSerialADK adk) {
        this.adk = adk;
//        StringBuilder sb = new StringBuilder();
//        for (TopicInfo t : adk.getPublications()) {
//            sb.append(t.getTopicName());
//            sb.append("||");
//        }
//
//        Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
    }
}