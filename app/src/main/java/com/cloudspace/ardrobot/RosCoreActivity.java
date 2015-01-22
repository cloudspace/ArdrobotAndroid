package com.cloudspace.ardrobot;

import android.hardware.Camera;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Surface;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.apache.commons.lang.ArrayUtils;
import org.ros.RosCore;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import geometry_msgs.Vector3;


public class RosCoreActivity extends RosActivity implements MessageListener<geometry_msgs.Twist> {

    private static final String TAG = "Ardrobot";
    private static final int MASTER_PORT = 11311;
    private static final long NODE_LIST_UPDATE_INTERVAL = 10000;

    private Listener listener;
    private int cameraId = 0;
    private RosCameraPreviewView rosCameraPreviewView;

    TextView mMasterUriOutput, rosTextView;
    ServoCommand[] lastCommand;

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
            mAccessory = getIntent().getParcelableExtra("accessory");
            openAccessory();
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
                        mMasterUriOutput.setText("Master URI - " + mMasterUri.toString());
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

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        try {            
            listener = new Listener(this, "/virtual_joystick/cmd_vel");
            NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName()).setMasterUri(mMasterUri);
            NodeConfiguration cameraConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName()).setMasterUri(mMasterUri);

            Camera camera = Camera.open(cameraId);
            setCameraDisplayOrientation(camera);
            rosCameraPreviewView.setCamera(camera);
            
            nodeMainExecutor.execute(listener, nodeConfiguration);
            nodeMainExecutor.execute(rosCameraPreviewView, cameraConfiguration);
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

    //    public void writeToBoard(ServoCommand[] direction) {
    //        ServoCommand d1 = direction[0];
    //        ServoCommand d2 = direction[1];
    //        byte[] buffer = new byte[]{d1.getSpeed(), (byte) d2.getSpeed()};
    //
    //        if (mOutputStream != null) {
    //            try {
    //                mOutputStream.write(buffer);
    //            } catch (IOException e) {
    //                Toast.makeText(this, "Command failed", Toast.LENGTH_SHORT).show();
    //            }
    //        }
    //    }

    public void writeToBoard(int x, int y) {
        byte[] buffer = new byte[]{(byte) (x < 0 ? 1 : 0), (byte) x, (byte) (y < 0 ? 1 : 0), (byte) y};
        Log.wtf("WROTE BUFFER", (x > 0 ? 1 : 0) + " : " + x + " : " + (y > 0 ? 1 : 0) + " : " + y);

        if (mOutputStream != null) {
            try {
                mOutputStream.write(buffer);
            } catch (IOException e) {
                Toast.makeText(this, "Command failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onNewMessage(final geometry_msgs.Twist message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Vector3 linearVector = message.getLinear();
                Vector3 angularVector = message.getAngular();

                double linear = linearVector.getX();
                double angular = angularVector.getZ();
                
                int adjustedLinear = Double.valueOf(linear * 100).intValue();
                int adjustedAngular = Double.valueOf(angular * 100).intValue();
                
                ServoCommand[] command = new ServoCommand[2];

                command[0] = ServoCommand.REAR.withSpeed(linear);
                command[1] = ServoCommand.FRONT.withSpeed(angular);

//                if (lastCommand != null) {
//                    if (command[0] != lastCommand[0] ||
//                            command[1] != lastCommand[1]) {
//                    }
//                }

                writeToBoard(adjustedLinear, adjustedAngular);
                lastCommand = command;

                StringBuilder sb = new StringBuilder();
                sb.append(linear).append(":").append(angular);
                String coords = sb.toString();
                rosTextView.setText(coords);
//                Log.wtf(TAG, "Coordinates: " + adjustedAngular + " : " + adjustedLinear);
            }
        });
    }

    private void openAccessory() {
        
        mFileDescriptor = mUsbManager.openAccessory(mAccessory);
        if (mFileDescriptor != null) {
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            Log.d(TAG, "accessory opened");
        } else {
            Log.d(TAG, "accessory open fail");
        }
    }
}
