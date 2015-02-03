package com.cloudspace.ardrobot;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.apache.commons.lang.ArrayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.InetAddress;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;


public class AccessoryActivity extends Activity implements Handler.Callback {
    private static final String TAG = "Ardrobot";

    private static final String ACTION_USB_PERMISSION = "com.cloudspace.ardrobot.action.USB_PERMISSION";

    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
    UsbAccessory mAccessory;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;

    TextView statusUpdate;

    ViewFlipper flippy;

    private boolean didAttemptStart = false;

    public static String getIpFromVPN(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getString(
                "ip", "");
    }

    public static void setIpFromVPN(Context c, String ipFromVPN) {
        PreferenceManager.getDefaultSharedPreferences(c).edit().putString("ip", ipFromVPN).commit();
    }

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
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
                updateAccessoryView();
            }
        }
    };

    private View.OnClickListener masterButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(v.getContext(), RosCoreActivity.class);
            if (mAccessory != null) {
                i.putExtra("accessory", mAccessory);
            }
            startActivity(i);
        }
    };

    private View.OnClickListener clientButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(v.getContext(), ClientActivity.class);
            if (mAccessory != null) {
                i.putExtra("accessory", mAccessory);
            }
            startActivity(i);
        }
    };

    private View.OnClickListener controllerButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(v.getContext(), ControllerActivity.class));
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        closeAccessory();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mInputStream != null && mOutputStream != null) {
            return;
        }

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

        updateAccessoryView();
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            Log.d(TAG, "accessory opened");
        } else {
            Log.d(TAG, "accessory open fail");
        }

        updateAccessoryView();

    }

    private void updateAccessoryView() {
        try {
            isConnectedToVPN(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    boolean result = msg.getData().getBoolean("result");
                    if (!result) {
                        flippy.setDisplayedChild(0);
                    } else {
                        flippy.setDisplayedChild((mOutputStream == null || mAccessory == null) ? 1 : 2);
                    }
                    return false;
                }
            });

        } catch (IOException e) {
            flippy.setDisplayedChild(0);
            e.printStackTrace();
        } catch (RemoteException e) {
            flippy.setDisplayedChild(0);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            flippy.setDisplayedChild(0);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            flippy.setDisplayedChild(0);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            flippy.setDisplayedChild(0);
            e.printStackTrace();
        }
    }

    private void closeAccessory() {
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }

            if (mOutputStream != null) {
                mOutputStream.close();
            }

            if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mInputStream = null;
            mOutputStream = null;
            mAccessory = null;
            updateAccessoryView();
        }
    }

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
        setContentView(R.layout.activity_accessory);
        flippy = (ViewFlipper) findViewById(R.id.viewSwitcher);

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

        findViewById(R.id.button_master_1).setOnClickListener(masterButtonListener);
        findViewById(R.id.button_client_1).setOnClickListener(clientButtonListener);
        findViewById(R.id.button_controller_1).setOnClickListener(controllerButtonListener);
        findViewById(R.id.button_master_2).setOnClickListener(masterButtonListener);
        findViewById(R.id.button_client_2).setOnClickListener(clientButtonListener);
        statusUpdate = (TextView) findViewById(R.id.status);

        if (!isVPNClientAvailable()) {
            try {
                copyApkToExternal();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        updateAccessoryView();
    }


    private void copyApkToExternal() throws IOException {
        InputStream in = getResources().openRawResource(R.raw.open_vpn);
        FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory() + "/open_vpn.apk");
        byte[] buff = new byte[1024];
        int read = 0;
        try {
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
        } finally {
            in.close();
            out.close();

            Intent promptInstall = new Intent(Intent.ACTION_VIEW);
            promptInstall.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/open_vpn.apk")),
                    "application/vnd.android.package-archive");

            startActivity(promptInstall);
            copyConfigToExternal();
        }
    }

    private void copyConfigToExternal() throws IOException {
        InputStream in = getResources().openRawResource(R.raw.client);
        FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory() + "/client.ovpn");
        byte[] buff = new byte[1024];
        int read = 0;
        try {
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
        } finally {
            in.close();
            out.close();
        }
    }

    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_UPDATE_MYIP = 1;
    private static final int START_PROFILE_EMBEDDED = 2;
    private static final int START_PROFILE_BYUUID = 3;
    private static final int ICS_OPENVPN_PERMISSION = 7;

    protected IOpenVPNAPIService mService = null;
    private Handler mHandler;


    private void startEmbeddedProfile() {
        try {
            isConnectedToVPN(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    if (!msg.getData().getBoolean("result")) {
                        try {
                            InputStream conf = getResources().openRawResource(R.raw.client);
                            InputStreamReader isr = new InputStreamReader(conf);
                            BufferedReader br = new BufferedReader(isr);
                            String config = "";
                            String line;
                            while (true) {
                                line = br.readLine();
                                if (line == null)
                                    break;
                                config += line + "\n";
                            }
                            br.readLine();
                            mService.addVPNProfile("client", config);
                            mService.startVPN(config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    return false;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler = new Handler(this);
        bindService();
    }


    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */

        @Override
        public void newStatus(String uuid, String state, String message, String level)
                throws RemoteException {
            Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
            msg.sendToTarget();

        }

    };


    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.

            mService = IOpenVPNAPIService.Stub.asInterface(service);

            try {
                // Request permission to use the API
                Intent i = mService.prepare(getPackageName());
                if (i != null) {
                    startActivityForResult(i, ICS_OPENVPN_PERMISSION);
                } else {
                    onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK, null);
                }

            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;

        }
    };
    private String mStartUUID = null;

    private void bindService() {
        Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
        icsopenvpnService.setPackage("de.blinkt.openvpn");

        bindService(icsopenvpnService, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        unbindService(mConnection);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService();
    }

    private void prepareStartProfile(int requestCode) throws RemoteException {
        if (mService == null || didAttemptStart) {
            return;
        }
        didAttemptStart = true;
        Intent requestpermission = mService.prepareVPNService();
        if (requestpermission == null) {
            onActivityResult(requestCode, Activity.RESULT_OK, null);
        } else {
            // Have to call an external Activity since services cannot used onActivityResult
            startActivityForResult(requestpermission, requestCode);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == START_PROFILE_EMBEDDED)
                startEmbeddedProfile();
            if (requestCode == START_PROFILE_BYUUID)
                try {
                    mService.startProfile(mStartUUID);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            if (requestCode == ICS_OPENVPN_PERMISSION) {
                try {
                    prepareStartProfile(START_PROFILE_EMBEDDED);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                try {
                    mService.registerStatusCallback(mCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        String data = (String) msg.obj;
        if (data.contains("CONNECTED|SUCCESS")) {
            String numberChunk = data.split(",", 2)[1];
            String ipChunk = numberChunk.split(",")[0];
            setIpFromVPN(this, ipChunk);
        } else if (data.contains("NOPROCESS")) {
            setIpFromVPN(this, "");
        }

        updateAccessoryView();

        return true;
    }

    protected boolean isVPNClientAvailable() {
        Intent mIntent = getPackageManager().getLaunchIntentForPackage("de.blinkt.openvpn");
        if (mIntent != null) {
            return true;
        } else {
            return false;
        }
    }

    public void isConnectedToVPN(final Handler.Callback c) throws IOException, RemoteException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        byte[] ipByte = BigInteger.valueOf(wifiManager.getConnectionInfo().getIpAddress()).toByteArray();
        ArrayUtils.reverse(ipByte);
        final String ip = InetAddress.getByAddress(ipByte).getHostAddress();
        Bundle b = new Bundle();
        String vpnIp = getIpFromVPN(this);
        boolean result = !vpnIp.isEmpty() && !ip.equals(vpnIp);
        b.putBoolean("result", result);
        Message m = new Message();
        m.setData(b);
        c.handleMessage(m);
    }
}
