package com.cloudspace.ardrobot;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.ros.address.InetAddressFactory;
import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosTextView;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.message.MessageListener;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.net.URISyntaxException;

import geometry_msgs.Vector3;


public class ClientActivity extends RosActivity implements MessageListener<geometry_msgs.Twist> {

    private static final String TAG = "Ardrobot";
    Button connectButton;

    private Listener listener;
    private int cameraId;
    private RosCameraPreviewView rosCameraPreviewView;

    EditText mMasterUriInput;
    ServoCommand[] lastCommand;

    private RosTextView<geometry_msgs.Twist> rosTextView;

    public ClientActivity() {
        super("ArdroBot", "ArdroBot");
    }

    boolean startedByUser = false;
    TextView masterUriOutput;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        masterUriOutput = (TextView) findViewById(R.id.master_uri_out);
        
        mMasterUriInput = (EditText) findViewById(R.id.master_uri);
        mMasterUriInput.setText("http://10.100.4.153:11311");

        rosCameraPreviewView = (RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);
        rosTextView = (RosTextView<geometry_msgs.Twist>) findViewById(R.id.text);
        rosTextView.setTopicName("/virtual_joystick/cmd_vel");
        rosTextView.setMessageType(geometry_msgs.Twist._TYPE);

        connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startedByUser = true;
                startMasterChooser();
            }
        });
    }

    @Override
    public void startMasterChooser() {

        try {
            nodeMainExecutorService.setMasterUri(new URI(mMasterUriInput.getText().toString()));
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    ClientActivity.this.init(nodeMainExecutorService);
                    return null;
                }
            }.execute();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        rosTextView.setMessageToStringCallable(new MessageCallable<String, geometry_msgs.Twist>() {
            @Override
            public String call(geometry_msgs.Twist message) {
                Vector3 linearVector = message.getLinear();
                Vector3 angularVector = message.getAngular();

                double x = linearVector.getX();
                double y = angularVector.getZ();

                StringBuilder sb = new StringBuilder();
                sb.append(x).append(":").append(y);
                return sb.toString();
            }
        });

    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        if (getMasterUri() != null && startedByUser) {
            masterUriOutput.setText("Child URI - " + getMasterUri().toString());

            listener = new Listener(this, "/virtual_joystick/cmd_vel");
            NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName());
            nodeConfiguration.setMasterUri(getMasterUri());

            nodeMainExecutor.execute(listener, nodeConfiguration);
            nodeMainExecutor.execute(rosTextView, nodeConfiguration);

            rosCameraPreviewView.setCamera(Camera.open(cameraId));
            nodeMainExecutor.execute(rosCameraPreviewView, nodeConfiguration);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((ViewFlipper) findViewById(R.id.flipper)).setDisplayedChild(1);
                }
            });
        }
    }

    public void writeToBoard(ServoCommand[] direction) {
        ServoCommand d1 = direction[0];
        ServoCommand d2 = direction[1];
        byte[] buffer = new byte[]{d1.targetServoByte, (byte) d1.getSpeed(), d2.targetServoByte, (byte) d2.getSpeed()};
//
//        if (mOutputStream != null) {
//            try {
//                mOutputStream.write(buffer);
//            } catch (IOException e) {
//                Toast.makeText(this, "Command failed", Toast.LENGTH_SHORT).show();
//            }
//        }
    }

    @Override
    public void onNewMessage(final geometry_msgs.Twist message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Vector3 linearVector = message.getLinear();
                Vector3 angularVector = message.getAngular();

                double x = linearVector.getX();
                double y = angularVector.getZ();

                ServoCommand[] command = new ServoCommand[2];

                command[0] = ServoCommand.FRONT.withSpeed(y);
                command[1] = ServoCommand.REAR.withSpeed(x);

                if (lastCommand != null) {
                    if (command[0] != lastCommand[0] ||
                            command[1] != lastCommand[1]) {
                    }
                }

                writeToBoard(command);
                lastCommand = command;

                StringBuilder sb = new StringBuilder();
                sb.append(x).append(":").append(y);
                String coords = sb.toString();
                Log.wtf(TAG, "Coordinates: " + coords);
            }
        });
    }
}
