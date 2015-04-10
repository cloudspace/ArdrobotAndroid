# ArdrobotAndroid
Android component for Openbots

The goal for this project was to build an Android app to control and view live streaming footage from an Arduino based robot from anywhere in the world over a VPN.

This project uses [ROS](http://www.ros.org/about-ros/), [ROS Java](https://github.com/rosjava/rosjava_core), and [ROS Serial](https://github.com/cloudspace/rosserial_arduino).
This implementation puts roscore on the Android device attached to the robot, which forwards the messages from your controller, via ros serial to an Arduino MegaADK with the motor controller shield to drive the motors.

To use this project, see [ROS Serial](https://github.com/cloudspace/rosserial_arduino) for the ros serial library. 
After that has been imported, open the motor_controller.ino sketch and upload it to your Arduino.

Install OpenVPN on your computer, and run the [server.conf](Resources/VPN/server.conf). 
Import this project into Android Studio, compile and run the app. You may need to update the ip in [client.conf](app/src/main/res/raw/client.conf) to match the ip of the computer you started the VPN on.

Upon first install, the app will prompt you to install the OpenVPN client bundled with the app. 
Once installed, the app will launch the sample VPN [client.conf](app/src/main/res/raw/client.conf) and attempt to connect. Once connected to the VPN, the app will display options for controller and master. 

For the device you wish to be attached to the Arduino, after the VPN is connected, simply plug the device into the Arduino, and when the app opens, select the roscore master option. You should see the app begin to display the cameras view, as well as a Master URI.


For the controller device, once connected to the VPN, open the app, select the controller option, input the Master URI value from the device attached to the Arduino, hit connect and you should see the output from the other devices camera, and a joystick. The joystick commands are parsed and passed to the servo pins on the motor controller shield.

=====
# ROS Cardboard
ROS enabled Cardboard components for Android.

This module provides a RosCardboardActivity, an activity enabled with both Ros and Cardboard components, and a sample CardboardViewerActivity, showing how to take a video stream from Ros and convert it to a Cardboard enabled split view.

=====
#ROSJava Audio

=====

# ROSSerial Android
Android specific components of ROSSerial.

ROSSerial is used to set up a bridge between the non network enabled Arduino, and the Ros connected Android device.

Usage is as follows.
`
try {
    adk = new ROSSerialADK(errorHandler, ExternalCoreActivity.this, connectedNode, mAccessory);
} catch (Exception e) {
    ROSSerialADK.sendError(errorHandler, ROSSerialADK.ERROR_ACCESSORY_CANT_CONNECT, e.getMessage());
    return;
}
adk.setOnSubscriptionCB(topicRegisteredListener);
adk.setOnSubscriptionCB(topicRegisteredListener);
`

Where errorHandler is null, or a Handler object setup to receive errors as follows
`
    Handler errorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (isActive()) {
                if (msg.getData() != null && msg.getData().get("error") != null) {
                    String title;
                    String message = (String) msg.getData().get("error");
                    switch (msg.what) {
                        case ROSSerialADK.ERROR_ACCESSORY_CANT_CONNECT:
                            title = "Unable to connect";
                            break;
                        case ROSSerialADK.ERROR_ACCESSORY_NOT_CONNECTED:
                            title = "Unable to communicate";
                            break;
                        default:
                        case ROSSerialADK.ERROR_UNKNOWN:
                            title = "Unknown error";
                            break;
                    }
                    if (errorDialog == null) {
                        errorDialog = new AlertDialog.Builder(ExternalCoreActivity.this).setTitle(title).setMessage(message).create();
                    } else {
                        errorDialog.setTitle(title);
                        errorDialog.setMessage(message);
                    }
                    errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                            startActivity(new Intent(ExternalCoreActivity.this, AccessoryActivity.class));
                        }
                    });
                    errorDialog.show();
                }
            }
        }
    };
    `

=====

# ROSSerial Java
ROSSerial implemented in pure java.

Originally found https://github.com/ros-drivers/rosserial-experimental/tree/master/rosserial_java, this repo brings ROSSerial up to Hydro compatibility.

=====

