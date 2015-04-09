package com.cloudspace.ardrobot;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.cloudspace.ardrobot.imu.ImuPublisher;
import com.cloudspace.ardrobot.util.BaseController;
import com.cloudspace.ardrobot.util.PublicationNode;
import com.cloudspace.ardrobot.util.StateConsciousTouchListener;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.RosImageView;
import org.ros.internal.message.Message;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import sensor_msgs.CompressedImage;
import std_msgs.Empty;

/**
 * Created by FutureHax on 3/30/15.
 */
public class SensorsControllerActivity extends BaseController {
    SensorManager sensorManager;
    ImuPublisher sensorPublisher;
    private RosImageView<CompressedImage> rosImageView;

    StateConsciousTouchListener touchListener = new StateConsciousTouchListener() {
        @Override
        public void onRelease() {
            stopPublisher.publish((Message) stopPublisher.newMessage());
        }
    };

    PublicationNode stopPublisher = new PublicationNode("sensor_killswitch", Empty._TYPE) {
        @Override
        public void onShutdownComplete(Node node) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors_controller);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rosImageView = (RosImageView<CompressedImage>) findViewById(R.id.camera_output);
        rosImageView.setTopicName("/camera/image/compressed");
        rosImageView.setMessageType(CompressedImage._TYPE);
        rosImageView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        findViewById(android.R.id.content).setOnTouchListener(touchListener);

        new AlertDialog.Builder(this).setTitle("To begin...").setMessage("Touch and hold the screen to send tilt based commands " +
                "to the robot. Remove your finger from the screen to instantly bring it to a halt.")
                .setPositiveButton("Got it", null).show();
    }

    @Override
    protected void init(final NodeMainExecutor nodeMainExecutor) {
        super.init(nodeMainExecutor);
        NodeConfiguration config = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        sensorPublisher = new ImuPublisher(sensorManager, 20000, touchListener);
        nodeMainExecutor.execute(sensorPublisher, config);

        NodeConfiguration imageViewConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        nodeMainExecutor.execute(rosImageView, imageViewConfig);

        NodeConfiguration stopConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        nodeMainExecutor.execute(stopPublisher, stopConfig);

    }
}
