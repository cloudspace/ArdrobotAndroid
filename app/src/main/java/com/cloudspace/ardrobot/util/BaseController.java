package com.cloudspace.ardrobot.util;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.cloudspace.ardrobot.R;
import com.cloudspace.rosjava_video.AudioPublisher;
import com.cloudspace.rosjava_video.AudioSubscriber;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.RosImageView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import sensor_msgs.CompressedImage;

/**
 * Created by FutureHax on 4/9/15.
 */
public class BaseController extends BaseActivity implements CompoundButton.OnCheckedChangeListener {
    public AudioPublisher audioPublisher;
    public AudioSubscriber audioSubscriber;
    public AudioStateWatcher audioWatcher;

    Button connectButton;
    EditText masterUriInput;
    public boolean startedByUser = true;
    public RosImageView<CompressedImage> rosImageView;

    CheckBox muteState, speakState;
    AudioStateWatcher.AudioState lastFromMute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        audioPublisher = new AudioPublisher(AudioStateWatcher.AudioState.CONTROLLER.topicName);
        audioSubscriber = new AudioSubscriber(AudioStateWatcher.AudioState.ROBOT.topicName, audioManager);
        audioWatcher = new AudioStateWatcher(audioPublisher, audioSubscriber, false);

        rosImageView = (RosImageView<CompressedImage>) findViewById(R.id.camera_output);
        rosImageView.setTopicName(Constants.NODE_IMAGE_COMPRESSED);
        rosImageView.setMessageType(CompressedImage._TYPE);
        rosImageView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        masterUriInput = (EditText) findViewById(R.id.master_uri);

        connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startedByUser = true;
                startMasterChooser();
            }
        });

        muteState = (CheckBox) findViewById(R.id.mute_state);
        speakState = (CheckBox) findViewById(R.id.speak_state);
        muteState.setOnCheckedChangeListener(this);
        speakState.setOnCheckedChangeListener(this);
    }

    /**
     * Create audio pub/pub and state watcher, as well as video stream watcher, create unique configs for each element.
     *
     * @param nodeMainExecutor
     */
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

        NodeConfiguration imageViewConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());
        nodeMainExecutor.execute(rosImageView, imageViewConfig);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        int id = compoundButton.getId();
        if (id == R.id.mute_state) {
            //Checked == mute
            if (checked) {
                //Store last audio state to restore after unmute
                lastFromMute = audioWatcher.state;
                audioWatcher.setState(AudioStateWatcher.AudioState.NO_AUDIO);
            } else {
                //Restore previous state and reset it
                audioWatcher.setState(lastFromMute);
                lastFromMute = null;
            }
        } else if (id == R.id.speak_state) {
            //Checked == audio from controller
            audioWatcher.setState(checked ? AudioStateWatcher.AudioState.CONTROLLER : AudioStateWatcher.AudioState.ROBOT);
        }
    }
}
