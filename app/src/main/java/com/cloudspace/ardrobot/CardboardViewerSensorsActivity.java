package com.cloudspace.ardrobot;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;

import com.cloudspace.ardrobot.imu.ImuPublisher;
import com.cloudspace.cardboard.CardboardViewerActivity;

import org.ros.address.InetAddressFactory;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

/**
 * Created by FutureHax on 3/30/15.
 */
public class CardboardViewerSensorsActivity extends CardboardViewerActivity {
    SensorManager sensorManager;
    ImuPublisher sensorPublisher;
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void init(final NodeMainExecutor nodeMainExecutor) {
        if (nodeMainExecutor != null) {
            super.init(nodeMainExecutor);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    NodeConfiguration config = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                            .setMasterUri(getMasterUri());
                    sensorPublisher = new ImuPublisher(sensorManager, 20000);
                    nodeMainExecutor.execute(sensorPublisher, config);
                    return null;
                }
            }.execute();
        }
    }
}
