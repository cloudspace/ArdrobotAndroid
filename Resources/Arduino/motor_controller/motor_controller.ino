#include <ros.h>
#include <geometry_msgs/Twist.h>
#include <std_msgs/Empty.h>
#include <sensor_msgs/Imu.h>
#include <Servo.h>
#include <adk.h>
#include <SPI.h>
#include <SabertoothSimplified.h>
#include <SoftwareSerial.h>
#include <USBSabertooth.h>

Servo servoFront, servoRear, servoPan, servoTilt;
ros::NodeHandle nh;
boolean lock = false;
long lockAt = 0;

#include <SoftwareSerial.h>
#include <SabertoothSimplified.h>

SoftwareSerial SWSerial(NOT_A_PIN, 11); // RX on no pin (unused), TX on pin 11 (to S1).
SabertoothSimplified ST(SWSerial); // Use SWSerial as the serial port.

// DRIVETRAINS (Based on Gordon McComb's Constructing Robot Bases book)
// NONHOLONOMIC
// 0 = Front Two Wheel Coaxial Drive (Most common)
// 1 = Mid Two Wheel Coaxial Drive (Roomba)
// 2 = Rear Two Wheel Coaxial Drive
// 3 = Four Wheel Coaxial Drive
// 4 = Six Wheel Coaxial Drive
// 5 = Eight Wheel Coaxial Drive
// 6 = Car-Type Steering Drive
// 7 = Car-Type Steering Drive, Independent Steering Wheels (e.g. Ackerman Steering)
// 8 = Three-Wheel Tricycle Drive
// 9 = Tracked Drive
// HONLONOMIC
// 10 = Three-Wheel Omnidirectional Drive
// 11 = Four-Wheel Omnidirectional Drive
// 12 = Synchronized Omnidirectional Drive
const int DRIVETRAIN = 6;

void arm(Servo targetServo) {
  targetServo.write(map(100, -100, 100, 0, 180));
  delay(1000);
  targetServo.write(map(-100, -100, 100, 0, 180));
  delay(1000);
  targetServo.write(map(0, -100, 100, 0, 180));
  delay(1000);
}

void joystickCb( const geometry_msgs::Twist& msg) {
  setTurn(msg.angular.z * 100);
  setSpeed(msg.linear.x * 100);
}

void tiltControllerCb( const sensor_msgs::Imu& msg) {
  if (lock && millis() - lockAt < 1000) {
    return;
  }
  geometry_msgs::Vector3 linearAcceleration = msg.linear_acceleration;
  setTurn(linearAcceleration.y * -10);
  setSpeed(linearAcceleration.x * -10);
}

void headTiltCb( const sensor_msgs::Imu& msg) {
  geometry_msgs::Vector3 linearAcceleration = msg.linear_acceleration;
  servoPan.write(map(linearAcceleration.y * -10, -100, 100, 0, 180));
  servoTilt.write(map(linearAcceleration.x * -10, -100, 100, 0, 180));
}

void killswitchCB( const std_msgs::Empty& msg) {
  setTurn(0);
  setSpeed(0);
  lock = true;
  lockAt = millis();
}
int power;


ros::Subscriber<geometry_msgs::Twist> joystickSub("controller/joystick/cmd_vel", joystickCb );
ros::Subscriber<sensor_msgs::Imu> headTiltSub("imu/head", headTiltCb );
ros::Subscriber<sensor_msgs::Imu> tiltControllerSub("imu/controller", tiltControllerCb );
ros::Subscriber<std_msgs::Empty> stopSub("sensor_killswitch", killswitchCB );

USB Usb;
ADK adk(&Usb, "ArdroBot", // Manufacturer Name
        "ArdroBotMegaADK", // Model Name
        "Open Source Robotics", // Description (user-visible string)
        "1.0", // Version
        "http://www.ardrobot.com", // URL (web page to visit if no installed apps support the accessory)
        "123456789");

boolean connected;
int leftSpeed = 1;
int rightSpeed = 5;

void setup()
{
  Serial.begin(57600);
  SWSerial.begin(9600);

  while (!Serial);
  if (Usb.Init() == -1) {
    while (1); // halt
  } else {
    Serial.print(F("\r\nArdrobot Started"));
  }

  handleSetupForType();
}

void handleSetupForType() {
  switch (DRIVETRAIN) {
    case 2: {
        ST.motor(1, 0);
        ST.motor(2, 0);
      }
      break;
    case 6: {
        servoRear.attach(3);
        servoFront.attach(2);
        servoPan.attach(4);
        servoTilt.attach(5);
        arm(servoRear);
      }
      break;
  }
}

void loop()
{
  Usb.Task();
  if (adk.isReady()) {
    if (!connected) {
      connected = true;
      Serial.print(F("\r\nArdrobot Ready"));
      nh.initNode(adk);
      nh.subscribe(joystickSub);
      nh.subscribe(headTiltSub);
      nh.subscribe(tiltControllerSub);
      nh.subscribe(stopSub);
      nh.spinOnce();
    } else {
      nh.spinOnce();
      delay(1000);
    }
  } else {
    if (connected) {
      connected = false;
    }
  }
}

void setSpeed(int speed) {
  //-100 - 100
  //-100 - full throttle reverse
  //100 - full throttle forward
  //0 - nuetral
  Serial.print(speed);
  switch (DRIVETRAIN) {
    case 2: {
        int leftSpeed, rightSpeed;
        if (speed > 0) {
          leftSpeed = speed * 1.15;
          rightSpeed = speed;
        } else {
          leftSpeed = speed;
          rightSpeed = speed * 0.7;
        }
        int mod = 31; // 0 - 63 used to artifically limit speeds.

        leftSpeed = map(leftSpeed, -100, 100, -127 + mod, 127 - mod);
        rightSpeed = map(rightSpeed, -100, 100, -127 + mod, 127 - mod);
        //1 is backwards, so is underpwoered
        ST.motor(1, rightSpeed);
        ST.motor(2, leftSpeed);
      }
      break;
    case 6:
      int mod = 60; // 0 - 90 used to artifically limit speeds.
      servoRear.write(map(speed, -100, 100, 0 + mod, 180 - mod));
      break;
  }

}

void setTurn(int turn) {
  //-100 - 100
  //-100 - full left
  //100 - full right
  //0 - nuetral
  switch (DRIVETRAIN) {
    case 2: {
        int mod = 50;
        leftSpeed = map(leftSpeed, -100, 100, -127 + mod, 127 - mod);
        Serial.print(F("\r\nTHE TURN:"));
        Serial.print(turn);
        Serial.print(F("\r\n"));
        int leftTurn, rightTurn;
        //        if (speed > 0) {
        //fwd
        //          leftSpeed = speed * 1.15;
        //          rightSpeed = speed;
        //        } else {
        //rev
        //          leftSpeed = speed;
        //          rightSpeed = speed * 0.7;
        //        }

        if (turn < -50) {
          //        Serial.print(F("\r\n+2"));
        } else if (turn < 0) {
          Serial.print(F("\r\n+1"));
          leftTurn = turn;
          rightTurn = 0;
          ST.motor(1, rightTurn);
          ST.motor(2, leftTurn);
        } else if (turn > 50) {
          //        Serial.print(F("\r\n-1"));

        } else {
          //        Serial.print(F("\r\n-2"));
        }

        //      if (turn > 0) {
        //        ST.motor(1, 0);
        //        ST.motor(2, turn);
        //      } else {
        //        ST.motor(1, turn);
        //        ST.motor(2, 0);
        //      }
        //      int min = 0, max = 180;
        //      servoFront.write(map(turn, -100, 100, 0, 180));
      }
      break;
    case 6:
      int min = 0, max = 180;
      servoFront.write(map(turn, -100, 100, 0, 180));
      break;
  }

}
