package com.cloudspace.ardrobot;

import android.hardware.Camera;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.view.Surface;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.cloudspace.rosserial_android.ROSSerialADK;
import com.cloudspace.rosserial_java.NodeConnectionUtils;

import org.apache.commons.lang.ArrayUtils;
import org.ros.RosCore;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class RosCoreActivity extends RosActivity {

    private static final String TAG = "Ardrobot";
    private static final int MASTER_PORT = 11311;
    private static final long NODE_LIST_UPDATE_INTERVAL = 10000;

    private int cameraId = 0;
    private RosCameraPreviewView rosCameraPreviewView;

    TextView mMasterUriOutput, rosTextView;

    private URI mMasterUri;
    RosCore core;
    private WifiManager wifiManager;

    ArrayAdapter<String> listAdapter;
    ListView nodeList;
    List<String> childNodes;

    UsbAccessory mAccessory;
    private ParcelFileDescriptor mFileDescriptor;
    private UsbManager mUsbManager;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;

    ConnectedNode connectedNode;
    ROSSerialADK adk;

    public RosCoreActivity() {
        super("ArdroBot", "ArdroBot");
    }

    final Handler nodeListUpdateHandler = new Handler();

    final Runnable nodeListRunnable = new Runnable() {
        public void run() {
            updateNodeList();
            nodeListUpdateHandler.postDelayed(this, NODE_LIST_UPDATE_INTERVAL);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);

        if (getIntent().hasExtra("accessory")) {
            openAccessory((UsbAccessory) getIntent().getParcelableExtra("accessory"));
        }
        
        mMasterUriOutput = (TextView) findViewById(R.id.master_uri);

        rosCameraPreviewView = (RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);
        rosTextView = (TextView) findViewById(R.id.text);


        if (core == null) {
            new AsyncTask<Void, Void, URI>() {
                @Override
                protected void onPostExecute(URI uri) {
                    super.onPostExecute(uri);
                    if (uri != null) {
                        mMasterUri = uri;
                        mMasterUriOutput.setText("Master URI - " + "http://" + AccessoryActivity.getIpFromVPN(RosCoreActivity.this) + ":" + MASTER_PORT);
                        ((ViewFlipper) findViewById(R.id.flipper)).setDisplayedChild(1);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                prepareListData();
                            }
                        }, 3000);
                    }
                }

                @Override
                protected URI doInBackground(Void... params) {
                    try {
                        byte[] ipByte = BigInteger.valueOf(wifiManager.getConnectionInfo().getIpAddress()).toByteArray();
                        ArrayUtils.reverse(ipByte);
                        core = RosCore.newPublic(InetAddress.getByAddress(ipByte).getHostAddress(), MASTER_PORT);
                        core.start();
                        core.awaitStart();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        return core != null ? core.getUri() : null;
                    }
                }
            }.execute();
        } else {
            ((ViewFlipper) findViewById(R.id.flipper)).setDisplayedChild(1);
        }

        nodeList = (ListView) findViewById(R.id.child_nodes);
    }

    void updateNodeList() {
        try {
            List<Object> topics = core.getMasterServer().getPublishedTopics(GraphName.of("android_core"), GraphName.of(("lister")));

            childNodes.clear();
            for (Object topic : topics) {
                childNodes.add(topic.toString());
            }

            listAdapter.notifyDataSetChanged();
        } catch (Exception e) {
        };
    }

    private void prepareListData() {
        List<Object> topics = core.getMasterServer().getPublishedTopics(GraphName.of("android_core"), GraphName.of(("lister")));
        childNodes = new ArrayList<>();

        for (Object topic : topics) {
            childNodes.add(topic.toString());
        }

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, childNodes);
        nodeList.setAdapter(listAdapter);
        nodeListRunnable.run();
    }

    @Override
    public void startMasterChooser() {
        nodeMainExecutorService.setMasterUri(mMasterUri);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                RosCoreActivity.this.init(nodeMainExecutorService);
                return null;
            }
        }.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        core.shutdown();
        core = null;
    }

    private void attemptToSetAdk() {
        if (connectedNode != null && mAccessory != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adk = new ROSSerialADK(RosCoreActivity.this, connectedNode, mAccessory, mFileDescriptor, mInputStream, mOutputStream);
                }
            });
        }
    }

    NodeConnectionUtils connectionUtils = new NodeConnectionUtils("node", new NodeConnectionUtils.OnNodeConnectedListener() {
        @Override
        public void onNodeConnected(ConnectedNode node) {
            connectedNode = node;
            attemptToSetAdk();
        }
    });

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        try {            
            NodeConfiguration config = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName()).setMasterUri(mMasterUri);
            NodeConfiguration cameraConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName()).setMasterUri(mMasterUri);

//            Camera camera = Camera.open(cameraId);
//            setCameraDisplayOrientation(camera);
//            rosCameraPreviewView.setCamera(camera);

            nodeMainExecutor.execute(connectionUtils, config);
//            nodeMainExecutor.execute(rosCameraPreviewView, cameraConfiguration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCameraDisplayOrientation(Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        camera.setDisplayOrientation((info.orientation - degrees + 360) % 360);
    }

    private void openAccessory(UsbAccessory accessory) {

        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

            attemptToSetAdk();
        }
    }
}
