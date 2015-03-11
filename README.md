# ArdrobotAndroid
Android component for Openbots

The goal for this project was to build an Android app to control and view live streaming footage from an Arduino based robot from anywhere in the world over a VPN.

This project uses [Ros](http://www.ros.org/about-ros/), [Ros](https://github.com/rosjava/rosjava_core), and [ROS Serial](https://github.com/cloudspace/rosserial_arduino).
This implementation puts roscore on the Android device attached to the robot, which forwards the messages from your controller, via ros serial to an Arduino MegaADK with the motor controller shield to drive the motors.

To use this project, see [ROS Serial](https://github.com/cloudspace/rosserial_arduino) for the ros serial library. 
After that has been imported, open the motor_controller.ino sketch and upload it to your Arduino.

Install OpenVPN on your computer, and run the [server.conf](Resources/VPN/server.conf). 
Import this project into Android Studio, compile and run the app. You may need to update the ip in [client.conf](app/src/main/res/raw/client.conf) to match the ip of the computer you started the VPN on.

Upon first install, the app will prompt you to install the OpenVPN client bundled with the app. 
Once installed, the app will launch the sample VPN [client.conf](app/src/main/res/raw/client.conf) and attempt to connect. Once connected to the VPN, the app will display options for controller and master. 

For the device you wish to be attached to the Arduino, after the VPN is connected, simply plug the device into the Arduino, and when the app opens, select the roscore master option. You should see the app begin to display the cameras view, as well as a Master URI.


For the controller device, once connected to the VPN, open the app, select the controller option, input the Master URI value from the device attached to the Arduino, hit connect and you should see the output from the other devices camera, and a joystick. The joystick commands are parsed and passed to the servo pins on the motor controller shield.
