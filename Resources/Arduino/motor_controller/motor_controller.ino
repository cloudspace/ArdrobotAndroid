#include <ros.h>
#include <geometry_msgs/Twist.h>
#include <std_msgs/Empty.h>
#include <sensor_msgs/Imu.h>
#include <adk.h>
#include <Servo.h>

Servo servoFront, servoRear;
ros::NodeHandle nh;

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

void imuCb( const sensor_msgs::Imu& msg) {
  geometry_msgs::Vector3 linearAcceleration = msg.linear_acceleration;
  setTurn(linearAcceleration.y * 10);
  setSpeed(linearAcceleration.x * 10);
}

void killswitchCB( const std_msgs::Empty& msg) {

}

ros::Subscriber<geometry_msgs::Twist> joystickSub("virtual_joystick/cmd_vel", joystickCb );
ros::Subscriber<sensor_msgs::Imu> imuSub("android/imu", imuCb );
ros::Subscriber<std_msgs::Empty> stopSub("killswitch", killswitchCB );

USB Usb;
ADK adk(&Usb, "ArdroBot",
        "ArdroBot",
        "ArdroBot",
        "1.0",
        "http://ArdroBot.com",
        "0000000012345678");

boolean connected;

void setup()
{
  Serial.begin(57600);
  servoRear.attach(9);
  servoFront.attach(10);
  arm(servoRear);

  while (!Serial);
  if (Usb.Init() == -1) {
    while (1); // halt
  } else {
    Serial.print(F("\r\nArduino Blink LED Started"));
  }
}


void loop()
{

  Usb.Task();

  if (adk.isReady()) {
    if (!connected) {
      connected = true;
      Serial.print(F("\r\nArduino Blink LED Ready"));
      nh.initNode(adk);
      nh.subscribe(joystickSub);
      //      nh.subscribe(imuSub);
      //      nh.subscribe(stopSub);
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
  Serial.print(speed);
  //-100 - 100
  //-100 - full throttle reverse
  //100 - full throttle forward
  //0 - nuetral
  int min = 80, max = 100;
  servoRear.write(map(speed, -100, 100, min, max));
}

void setTurn(int turn) {
  Serial.print(turn);
  //-100 - 100
  //-100 - full left
  //100 - full right
  //0 - nuetral
  int min = 0, max = 180;
  servoFront.write(map(turn, -100, 100, min, max));
}
