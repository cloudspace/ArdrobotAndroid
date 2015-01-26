package com.cloudspace.ardrobot;

import android.hardware.Camera;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.message.MessageListener;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import geometry_msgs.Vector3;


public class RobotActivity extends RosActivity implements MessageListener<geometry_msgs.Twist> {

    private static final String TAG = "Ardrobot";

    private Listener listener;
    private int cameraId = 0;
    private RosCameraPreviewView rosCameraPreviewView;
    boolean startedByUser = false;

    TextView mMasterUriOutput, rosTextView;
    ServoCommand[] lastCommand;

    private WifiManager wifiManager;

    ListView nodeList;

    UsbAccessory mAccessory;
    private ParcelFileDescriptor mFileDescriptor;
    private UsbManager mUsbManager;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;
    private EditText mMasterUriInput;

    public RobotActivity() {
        super("ArdroBot", "ArdroBot");
    }

    Button connectButton;

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
        mMasterUriInput = (EditText) findViewById(R.id.master_uri_input);
        mMasterUriInput.setText("http://10.100.4.210:11311");

        rosCameraPreviewView = (RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);
        rosTextView = (TextView) findViewById(R.id.text);

        connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startedByUser = true;
                startMasterChooser();
            }
        });

        nodeList = (ListView) findViewById(R.id.child_nodes);
    }



    @Override
    public void startMasterChooser() {
        try {
            nodeMainExecutorService.setMasterUri(new URI(mMasterUriInput.getText().toString()));
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    RobotActivity.this.init(nodeMainExecutorService);
                    return null;
                }
            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        if (getMasterUri() != null && startedByUser) {

            try {
                listener = new Listener(this, "/virtual_joystick/cmd_vel");
                NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName());
                nodeConfiguration.setMasterUri(getMasterUri());

                NodeConfiguration cameraConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName());
                cameraConfiguration.setMasterUri(getMasterUri());

                Camera camera = Camera.open(cameraId);
                setCameraDisplayOrientation(camera);
                rosCameraPreviewView.setCamera(camera);

                nodeMainExecutor.execute(listener, nodeConfiguration);
                nodeMainExecutor.execute(rosCameraPreviewView, cameraConfiguration);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((ViewFlipper) findViewById(R.id.flipper)).setDisplayedChild(1);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
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
