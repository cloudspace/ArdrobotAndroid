package com.cloudspace.ardrobot.util;

import com.cloudspace.rosjava_audio.AudioPublisher;
import com.cloudspace.rosjava_audio.AudioSubscriber;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import std_msgs.Int8;

/**
 * Created by r2DoesInc (r2doesinc@futurehax.com) on 4/24/15.
 */
public class AudioStateWatcher extends AbstractNodeMain {

    public static final String AUDIO_STATE_NODE = "audio_state";
    public static final String ROBOT_PREFIX = "robot_";
    public static final String CONTROLLER_PREFIX = "controller_";
    public static final String NODE_NAME_SUFFIX = "audio_watcher";
    public static final String AUDIO_FROM_CONTROLLER = "audio_from_controller";
    public static final String AUDIO_FROM_ROBOT = "audio_from_robot";
    AudioPublisher audioPublisher;
    AudioSubscriber audioSubscriber;

    boolean isRobotHost;

    AudioState state;
    Publisher<std_msgs.Int8> statePublisher;

    public MessageListener stateMessageListener = new MessageListener<Int8>() {
        @Override
        public void onNewMessage(Int8 int8) {
            state = AudioState.getState(new Byte(int8.getData()).intValue());
            if (isRobotHost) {
                handleRobot();
            } else {
                handleController();
            }
        }
    };

    /**
     * Watches the audio state node for commands to enable/disable mic/speaker
     *
     * @param audioPublisher
     * @param audioSubscriber
     * @param isRobotHost
     */
    public AudioStateWatcher(AudioPublisher audioPublisher, AudioSubscriber audioSubscriber, boolean isRobotHost) {
        this.audioPublisher = audioPublisher;
        this.audioSubscriber = audioSubscriber;
        this.isRobotHost = isRobotHost;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        statePublisher = connectedNode.newPublisher(AUDIO_STATE_NODE, Int8._TYPE);


        Subscriber subscriber = connectedNode.newSubscriber(AUDIO_STATE_NODE, Int8._TYPE);
        subscriber.addMessageListener(stateMessageListener);

        Int8 msg = statePublisher.newMessage();
        msg.setData((byte) AudioState.ROBOT.state);
        stateMessageListener.onNewMessage(msg);
    }

    private void handleRobot() {
        switch (state) {
            case NO_AUDIO:
                audioPublisher.pause();
                audioSubscriber.pause();
                break;
            case CONTROLLER:
                audioPublisher.pause();
                audioSubscriber.play();
                break;
            case ROBOT:
                audioPublisher.play();
                audioSubscriber.pause();
                break;
        }
    }

    private void handleController() {
        switch (state) {
            case NO_AUDIO:
                audioPublisher.pause();
                audioSubscriber.pause();
                break;
            case CONTROLLER:
                audioPublisher.play();
                audioSubscriber.pause();
                break;
            case ROBOT:
                audioPublisher.pause();
                audioSubscriber.play();
                break;
        }
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of((isRobotHost ? ROBOT_PREFIX : CONTROLLER_PREFIX) + NODE_NAME_SUFFIX);
    }

    /**
     * Set the new state for all connected devices.
     *
     * @param newState New state of communication
     */
    public void setState(AudioState newState) {
        if (newState != null && statePublisher != null) {
            Int8 msg = statePublisher.newMessage();
            msg.setData((byte) newState.state);
            statePublisher.publish(msg);
        }
    }

    public enum AudioState {
        NO_AUDIO(0, null), CONTROLLER(1, AUDIO_FROM_CONTROLLER), ROBOT(2, AUDIO_FROM_ROBOT);

        public int state;
        public String topicName;

        AudioState(int state, String topicName) {
            this.state = state;
            this.topicName = topicName;
        }

        public static AudioState getState(int state) {
            switch (state) {
                case 0:
                    return NO_AUDIO;
                case 1:
                    return CONTROLLER;
                case 2:
                    return ROBOT;
                default:
                    return NO_AUDIO;
            }
        }
    }
}



