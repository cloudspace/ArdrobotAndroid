package com.cloudspace.ardrobot.util;

import com.cloudspace.rosjava_audio.AudioPublisher;
import com.cloudspace.rosjava_audio.AudioSubscriber;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;

import std_msgs.Int8;

/**
 * Created by FutureHax on 4/24/15.
 */
public class AudioStateWatcher extends AbstractNodeMain {

    AudioPublisher audioPublisher;
    AudioSubscriber audioSubscriber;

    boolean isRobotHost;

    AudioState state;

    public AudioStateWatcher(AudioPublisher audioPublisher, AudioSubscriber audioSubscriber, boolean isRobotHost) {
        this.audioPublisher = audioPublisher;
        this.audioSubscriber = audioSubscriber;
        this.isRobotHost = isRobotHost;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        org.ros.node.topic.Subscriber subscriber = connectedNode.newSubscriber("audio_state", Int8._TYPE);
        subscriber.addMessageListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 int8) {
                state = AudioState.getState(new Byte(int8.getData()).intValue());
                if (isRobotHost) {
                    handleRobot();
                } else {
                    handleController();
                }
            }
        });
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
        return GraphName.of((isRobotHost ? "robot_" : "controller_") + "audio_watcher");
    }

    public enum AudioState {
        NO_AUDIO(0), CONTROLLER(1), ROBOT(2);

        public int state;

        AudioState(int state) {
            this.state = state;
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
