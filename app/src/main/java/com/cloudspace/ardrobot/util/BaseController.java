package com.cloudspace.ardrobot.util;

import android.os.Bundle;

import com.cloudspace.rosjava_audio.AudioPublisher;

import org.ros.address.InetAddressFactory;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

/**
 * Created by FutureHax on 4/9/15.
 */
public class BaseController extends BaseActivity {
    public AudioPublisher audioPublisher;

    public BaseController() {
        super("Ardrobot is running.", "Ardrobot");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioPublisher = new AudioPublisher("audio_from_controller");
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration audioPubConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        nodeMainExecutor
                .execute(audioPublisher, audioPubConfig);
    }
}
