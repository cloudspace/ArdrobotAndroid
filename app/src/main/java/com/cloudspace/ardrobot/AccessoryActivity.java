package com.cloudspace.ardrobot;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.cloudspace.ardrobot.util.BleService;
import com.cloudspace.ardrobot.util.BootReciever;
import com.cloudspace.ardrobot.util.Constants;
import com.cloudspace.ardrobot.util.SettingsProvider;

import org.apache.commons.lang.ArrayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.InetAddress;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;


public class AccessoryActivity extends AccessoryWatchingActivity implements Handler.Callback {
    private static final String TAG = "Ardrobot";

    TextView statusUpdate;

    ViewFlipper flippy;
    int retryAttempts = 0;

    private boolean didAttemptStart = false;

    boolean SHOULD_OVERRIDE_VPN_REQUIREMENT = true;

    public static String getIpFromVPN(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getString(
                Constants.PREF_IP, "");
    }

    public static void setIpFromVPN(Context c, String ipFromVPN) {
        PreferenceManager.getDefaultSharedPreferences(c).edit().putString(Constants.PREF_IP, ipFromVPN).commit();
    }

    private View.OnClickListener masterButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            Intent i = new Intent(v.getContext(), RosCoreActivity.class);
            Intent i = new Intent(v.getContext(), ExternalCoreActivity.class);
            if (getCurrentAccessory() != null) {
                i.putExtra(Constants.EXTRA_ACCESSORY, getCurrentAccessory());
            }
            startActivity(i);
            finish();
        }
    };

    private View.OnClickListener cardboardButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(v.getContext(), CardboardViewerSensorsActivity.class));
            finish();
        }
    };

    private View.OnClickListener sensorButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(v.getContext(), SensorsControllerActivity.class));
            finish();
        }
    };

    private View.OnClickListener controllerButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(v.getContext(), ControllerActivity.class));
            finish();
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void updateAccessoryView() {
        try {
            isConnectedToVPN(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    boolean result = msg.getData().getBoolean(Constants.EXTRA_RESULT);
                    if (!result) {
                        flippy.setDisplayedChild(0);
                    } else {
                        flippy.setDisplayedChild((getCurrentAccessory() == null) ? 1 : 2);
                    }
                    return false;
                }
            });

        } catch (Exception e) {
            flippy.setDisplayedChild(0);
            e.printStackTrace();
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessory);
        flippy = (ViewFlipper) findViewById(R.id.viewSwitcher);

        findViewById(R.id.video_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(v.getContext(), VideoConferenceTest.class);
                if (getCurrentAccessory() != null) {
                    i.putExtra(Constants.EXTRA_ACCESSORY, getCurrentAccessory());
                }
                startActivity(i);
                finish();
            }
        });
        findViewById(R.id.button_master_1).setOnClickListener(masterButtonListener);
        findViewById(R.id.button_cardboard_1).setOnClickListener(cardboardButtonListener);
        findViewById(R.id.button_controller_1).setOnClickListener(controllerButtonListener);
        findViewById(R.id.button_sensor_1).setOnClickListener(sensorButtonListener);
        findViewById(R.id.button_master_2).setOnClickListener(masterButtonListener);
        findViewById(R.id.button_cardboard_2).setOnClickListener(cardboardButtonListener);
        findViewById(R.id.button_sensor_2).setOnClickListener(sensorButtonListener);
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

    @Override
    public void handleClosedAccessory() {
        updateAccessoryView();
    }

    @Override
    public void handleAccessory() {
        updateAccessoryView();
    }

    private void copyApkToExternal() throws IOException {
        InputStream in = getResources().openRawResource(R.raw.open_vpn);
        FileOutputStream out = new FileOutputStream(Constants.PATH_OVPN_APK);
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
            promptInstall.setDataAndType(Uri.fromFile(new File(Constants.PATH_OVPN_APK)),
                    Constants.APK_DATA_TYPE);

            startActivity(promptInstall);
            copyConfigToExternal();
        }
    }

    private void copyConfigToExternal() throws IOException {
        InputStream in = getResources().openRawResource(R.raw.client);
        FileOutputStream out = new FileOutputStream(Constants.PATH_OVPN_CLIENT_CONFIG);
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

    protected IOpenVPNAPIService mVpnService = null;
    BleService mBtService;

    private Handler mHandler;


    private void startEmbeddedProfile() {
        try {
            isConnectedToVPN(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    if (!msg.getData().getBoolean(Constants.EXTRA_RESULT)) {
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
                            if (!config.equals("\n")) {
                                mVpnService.addVPNProfile("client", config);
                                mVpnService.startVPN(config);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler = new Handler(this);
        bindServices();
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
    private ServiceConnection mVpnConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.

            mVpnService = IOpenVPNAPIService.Stub.asInterface(service);

            try {
                // Request permission to use the API
                Intent i = mVpnService.prepare(getPackageName());
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
            mVpnService = null;

        }
    };

    private ServiceConnection mBtConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mBtService = ((BleService.LocalBinder) service).getService();
            mBtService.initialize();

            Log.d("CONNECTED TO BT SERVCE", "NOW");
            if (!SettingsProvider.getEdisonAddress(AccessoryActivity.this).isEmpty()) {
                mBtService.connect(SettingsProvider.getEdisonAddress(AccessoryActivity.this));
                final Handler h = new Handler();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (mBtService.mConnectionState == mBtService.STATE_CONNECTING) {
                            retryAttempts = retryAttempts + 1;
                            if (retryAttempts >= 60) {
                                mBtService.mConnectionState = mBtService.STATE_DISCONNECTED;
                            }
                            h.postDelayed(this, 1000);
                        } else if (mBtService.mConnectionState == mBtService.STATE_DISCONNECTED) {
                            sendBroadcast(new Intent(AccessoryActivity.this, BootReciever.class));
                        }
                    }
                };

                h.post(r);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mVpnService = null;
        }
    };
    private String mStartUUID = null;

    private void bindServices() {
        Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
        icsopenvpnService.setPackage(Constants.VPN_SERVICE_PACKAGE);

        bindService(icsopenvpnService, mVpnConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, BleService.class), mBtConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        unbindService(mVpnConnection);
        unbindService(mBtConnection);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService();
    }


    private void prepareStartProfile(int requestCode) throws RemoteException {
        if (mVpnService == null || didAttemptStart) {
            return;
        }
        didAttemptStart = true;
        Intent requestpermission = mVpnService.prepareVPNService();
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
                    mVpnService.startProfile(mStartUUID);
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
                    mVpnService.registerStatusCallback(mCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        String data = (String) msg.obj;
        if (data.contains(Constants.VPN_CONNECTED_SUCCESS)) {
            String numberChunk = data.split(",", 2)[1];
            String ipChunk = numberChunk.split(",")[0];
            setIpFromVPN(this, ipChunk);
        } else if (data.contains(Constants.VPN_NOPROCESS)) {
            setIpFromVPN(this, "");
        }

        updateAccessoryView();

        return true;
    }

    protected boolean isVPNClientAvailable() {
        Intent mIntent = getPackageManager().getLaunchIntentForPackage(Constants.VPN_SERVICE_PACKAGE);
        if (mIntent != null) {
            return true;
        } else {
            return false;
        }
    }

    public void isConnectedToVPN(final Handler.Callback c) throws IOException, RemoteException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Bundle b = new Bundle();
        boolean result;
        if (!SHOULD_OVERRIDE_VPN_REQUIREMENT) {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            byte[] ipByte = BigInteger.valueOf(wifiManager.getConnectionInfo().getIpAddress()).toByteArray();
            ArrayUtils.reverse(ipByte);
            final String ip = InetAddress.getByAddress(ipByte).getHostAddress();
            String vpnIp = getIpFromVPN(this);
            result = !vpnIp.isEmpty() && !ip.equals(vpnIp);
        } else {
            result = true;
        }
        b.putBoolean(Constants.EXTRA_RESULT, result);
        Message m = new Message();
        m.setData(b);
        c.handleMessage(m);
    }
}
