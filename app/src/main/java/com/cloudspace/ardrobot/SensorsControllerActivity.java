package com.cloudspace.ardrobot;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ViewFlipper;

import com.cloudspace.ardrobot.imu.ImuPublisher;
import com.cloudspace.ardrobot.util.BaseController;
import com.cloudspace.ardrobot.util.Constants;
import com.cloudspace.ardrobot.util.PublicationNode;
import com.cloudspace.ardrobot.util.StateConsciousTouchListener;

import org.ros.address.InetAddressFactory;
import org.ros.internal.message.Message;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import std_msgs.Empty;

/**
 * Created by FutureHax on 3/30/15.
 */
public class SensorsControllerActivity extends BaseController {

    SensorManager sensorManager;
    ImuPublisher sensorPublisher;

    StateConsciousTouchListener touchListener = new StateConsciousTouchListener() {
        @Override
        public void onRelease() {
            stopPublisher.publish((Message) stopPublisher.newMessage());
        }
    };

    PublicationNode stopPublisher = new PublicationNode(Constants.NODE_SENSOR_KILLSWITCH, Empty._TYPE) {
        @Override
        public void onShutdownComplete(Node node) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        findViewById(android.R.id.content).setOnTouchListener(touchListener);

        new AlertDialog.Builder(this).setTitle(R.string.to_begin).setMessage(R.string.to_begin_message)
                .setPositiveButton(R.string.got_it, null).show();
    }

    @Override
    protected void init(final NodeMainExecutor nodeMainExecutor) {
        super.init(nodeMainExecutor);
        NodeConfiguration config = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        sensorPublisher = new ImuPublisher(sensorManager, 20000, touchListener, Constants.NODE_IMU_CONTROLLER);
        nodeMainExecutor.execute(sensorPublisher, config);


        NodeConfiguration stopConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        nodeMainExecutor.execute(stopPublisher, stopConfig);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ViewFlipper) findViewById(R.id.flipper)).setDisplayedChild(1);
            }
        });
    }
}
