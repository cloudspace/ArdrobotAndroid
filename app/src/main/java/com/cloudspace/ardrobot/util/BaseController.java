package com.cloudspace.ardrobot.util;

import android.os.Bundle;

import com.cloudspace.rosjava_audio.AudioPublisher;
import com.cloudspace.rosjava_audio.AudioSubscriber;

import org.ros.address.InetAddressFactory;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

/**
 * Created by FutureHax on 4/9/15.
 */
public class BaseController extends BaseActivity {
    public AudioPublisher audioPublisher;
    public AudioSubscriber audioSubscriber;
    public AudioStateWatcher audioWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioPublisher = new AudioPublisher("audio_from_controller");
        audioSubscriber = new AudioSubscriber("audio_from_robot");
        audioWatcher = new AudioStateWatcher(audioPublisher, audioSubscriber, false);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration audioSubConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        nodeMainExecutor.execute(audioSubscriber, audioSubConfig);

        NodeConfiguration audioPubConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        nodeMainExecutor.execute(audioPublisher, audioPubConfig);

        NodeConfiguration audioWatcherConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        nodeMainExecutor.execute(audioWatcher, audioWatcherConfig);
    }
}
