package com.cloudspace.ardrobot.imu;



import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Looper;

public class ImuThread extends Thread {
    private final SensorManager sensorManager;
    private SensorListener sensorListener;
    private Looper threadLooper;

    private final Sensor accelSensor;
    private final Sensor gyroSensor;
    private final Sensor quatSensor;

    private int sensorDelay;

    public ImuThread(SensorManager sensorManager, SensorListener sensorListener, int sensorDelay) {
        this.sensorManager = sensorManager;
        this.sensorListener = sensorListener;

        this.sensorDelay = sensorDelay;

        this.accelSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.gyroSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        this.quatSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }


    public void run() {
        Looper.prepare();
        this.threadLooper = Looper.myLooper();
        this.sensorManager.registerListener(this.sensorListener, this.accelSensor, sensorDelay);
        this.sensorManager.registerListener(this.sensorListener, this.gyroSensor, sensorDelay);
        this.sensorManager.registerListener(this.sensorListener, this.quatSensor, sensorDelay);
        Looper.loop();
    }


    public void shutdown() {
        this.sensorManager.unregisterListener(this.sensorListener);
        if (this.threadLooper != null) {
            this.threadLooper.quit();
        }
    }
}
