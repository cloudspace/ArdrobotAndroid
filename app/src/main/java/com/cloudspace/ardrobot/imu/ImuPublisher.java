/*
 * Copyright (c) 2011, Chad Rockey
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Android Sensors Driver nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.cloudspace.ardrobot.imu;


import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.cloudspace.ardrobot.util.StateConsciousTouchListener;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import sensor_msgs.Imu;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 * @author axelfurlan@gmail.com (Axel Furlan)
 */
public class ImuPublisher implements NodeMain {

    private ImuThread imuThread;
    private SensorListener sensorListener;
    private SensorManager sensorManager;
    private Publisher<Imu> publisher;
    private int sensorDelay;
    StateConsciousTouchListener touchListener;
    private final String nodeName;

    public ImuPublisher(SensorManager manager, int sensorDelay, String nodeName) {
        this.sensorManager = manager;
        this.sensorDelay = sensorDelay;
        this.nodeName = nodeName;
    }

    public ImuPublisher(SensorManager manager, int sensorDelay, StateConsciousTouchListener touchListener, String nodeName) {
        this(manager, sensorDelay, nodeName);
        this.touchListener = touchListener;
    }

    public GraphName getDefaultNodeName() {
        return GraphName.newAnonymous();
    }

    public void onError(Node node, Throwable throwable) {
    }

    public void onStart(ConnectedNode node) {
        try {
            this.publisher = node.newPublisher("android/imu/" + nodeName, "sensor_msgs/Imu");

            this.sensorListener = new SensorListener(publisher,
                    !sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).isEmpty(),
                    !sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).isEmpty(),
                    !sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).isEmpty(), touchListener);
            this.imuThread = new ImuThread(this.sensorManager, sensorListener, sensorDelay);
            this.imuThread.start();
        } catch (Exception e) {
            if (node != null) {
                node.getLog().fatal(e);
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onShutdown(Node arg0) {
        if (this.imuThread == null) {
            return;
        }
        this.imuThread.shutdown();

        try {
            this.imuThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onShutdownComplete(Node arg0) {
    }

    public Imu getImu() {
        return sensorListener.getImu();
    }
}
