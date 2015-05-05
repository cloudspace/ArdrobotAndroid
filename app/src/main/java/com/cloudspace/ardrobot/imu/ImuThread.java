package com.cloudspace.ardrobot.imu;



import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Looper;

class ImuThread extends Thread {
    private final SensorManager sensorManager;
    private SensorListener sensorListener;
    private Looper threadLooper;

    private final Sensor accelSensor;
    private final Sensor gyroSensor;
    private final Sensor quatSensor;

    /**
     *
     * @param sensorManager SensorManager created with activity context
     * @param sensorListener SensorListener created to translate raw sensor data into ROS Imu messages
     */
    public ImuThread(SensorManager sensorManager, SensorListener sensorListener) {
        this.sensorManager = sensorManager;
        this.sensorListener = sensorListener;

        this.accelSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.gyroSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        this.quatSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }


    public void run() {
        Looper.prepare();
        this.threadLooper = Looper.myLooper();
        this.sensorManager.registerListener(this.sensorListener, this.accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        this.sensorManager.registerListener(this.sensorListener, this.gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        this.sensorManager.registerListener(this.sensorListener, this.quatSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Looper.loop();
    }


    public void shutdown() {
        this.sensorManager.unregisterListener(this.sensorListener);
        if (this.threadLooper != null) {
            this.threadLooper.quit();
        }
    }
}
