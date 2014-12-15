package com.cloudspace.ardrobot;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ViewFlipper;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.BitmapFromImage;
import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosImageView;
import org.ros.android.view.VirtualJoystickView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.net.URISyntaxException;

import sensor_msgs.CompressedImage;
import sensor_msgs.Image;

/**
 * Created by cloudspace on 12/15/14.
 */
public class ControllerActivity extends RosActivity {
    Button connectButton;
    EditText masterUriInput;
    private RosImageView<CompressedImage> rosImageView;
    boolean startedByUser = false;
    private VirtualJoystickView virtualJoystickView;

    public ControllerActivity() {
        super("Controller", "Controller");
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        if (getMasterUri() != null && startedByUser) {
            NodeConfiguration imageViewConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName());
            imageViewConfig.setMasterUri(getMasterUri());
            nodeMainExecutor.execute(rosImageView, imageViewConfig);

            NodeConfiguration controllerViewConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName());
            controllerViewConfig.setMasterUri(getMasterUri());
            controllerViewConfig.setNodeName("virtual_joystick");
            nodeMainExecutor
                    .execute(virtualJoystickView, controllerViewConfig);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((ViewFlipper) findViewById(R.id.flipper)).setDisplayedChild(1);
                    virtualJoystickView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller);
        virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtual_joystick);

        rosImageView = (RosImageView<sensor_msgs.CompressedImage>)findViewById(R.id.camera_output);
        rosImageView.setTopicName("/camera/image/compressed");
        rosImageView.setMessageType(CompressedImage._TYPE);
        rosImageView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        masterUriInput = (EditText) findViewById(R.id.master_uri);
        masterUriInput.setText("http://10.0.1.40:11311");

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
            nodeMainExecutorService.setMasterUri(new URI((masterUriInput.getText().toString())));
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    ControllerActivity.this.init(nodeMainExecutorService);
                    return null;
                }
            }.execute();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }
}
