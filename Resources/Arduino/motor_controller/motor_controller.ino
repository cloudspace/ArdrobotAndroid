/*
 * rosserial Publisher Example
 * Prints "hello world!"
 */

#include <ros.h>
#include <geometry_msgs/Twist.h>
#include <adk.h>
#include <Servo.h>

ros::NodeHandle  nh;git
Servo servoFront, servoRear;

const int servoFrontPin =  9;
const int servoRearPin =  10;

void arm(Servo targetServo){
  setSpeed(100, targetServo);
  delay(1000); 
  setSpeed(-100, targetServo);
  delay(1000); 
  setSpeed(0, targetServo);
  delay(1000);   
}

void messageCb( const geometry_msgs::Twist& msg) {
  Serial.print("\r\nLineaer :");
  Serial.print(msg.linear.x);
  Serial.print(" : ");
  Serial.print(msg.linear.y);
  Serial.print(" : ");
  Serial.print(msg.linear.z);
  
  Serial.print("\r\nAngular :");
  Serial.print(msg.angular.x);
  Serial.print(" : ");
  Serial.print(msg.angular.y);
  Serial.print(" : ");
  Serial.print(msg.angular.z);
  
  setSpeed(msg.angular.z * 100, servoFront);
  setSpeed(msg.linear.x * 100, servoRear);
}

ros::Subscriber<geometry_msgs::Twist> sub("virtual_joystick/cmd_vel", messageCb );

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
  pinMode(13, OUTPUT);
  servoRear.attach(servoFrontPin);
  servoFront.attach(servoRearPin);
  arm(servoRear); 
  
  while (!Serial); // Wait for serial port to connect - used on Leonardo, Teensy and other boards with built-in USB CDC serial connection
  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  } else {
    Serial.print("\r\nArduino Blink LED Started");
  }
}


void loop()
{

  Usb.Task();

  if (adk.isReady()) {
    if (!connected) {
      connected = true;
      Serial.print("\r\nConnected to accessory");
      nh.initNode(adk);
      nh.subscribe(sub);
    } else {
      nh.spinOnce();
      delay(1000);
    }
  } else {
    if (connected) {
      connected = false;
      Serial.print("\r\nDisconnected from accessory");
    }
  }
}

void setSpeed(int speed, Servo targetServo) {

  //-100 - 100 
  //-100 - full throttle reverse
  //100 - full throttle forward
  //0 - nuetral
  Serial.println("Speed " + String(speed));
  int angle = map(speed, -100, 100, 0, 180);
  Serial.println("Adjusted to angle " + String(angle));

  targetServo.write(angle);    
}