#include <AFMotor.h>

#include <Max3421e.h>
#include <Usb.h>
#include <Servo.h>
#include <AndroidAccessory.h>

#define DRIVETRAIN 6
AndroidAccessory acc("ArdroBot",
"ArdroBot",
"ArdroBot",
"1.0",
"http://ArdroBot.com",
"0000000012345678");

int ledState = LOW;
const int ledPin =  13;
const int servoLeftPin =  9;
const int servoRightPin =  10;
unsigned long previousMillis = 0;
const long longInterval = 3000;
const long shortInterval = 100;

const int LEFT = 1;
const int RIGHT = 2;
const int STRAIGHT = 3;
const int BACK = 4;
const int STOP = 5;

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

void setSpeed(int speed, Servo targetServo) {

  //-100 - 100 
  //-100 - fullx throttle reverse 
  //100 - full throttle forward
  //0 - nuetral
  Serial.println("Speed " + String(speed));
  int angle = map(speed, -100, 100, 0, 180);
  Serial.println("Adjusted to angle " + String(angle));

  targetServo.write(angle);    
}

void setup()
{
  // set communiation speed
  Serial.begin(115200);
  pinMode(ledPin, OUTPUT);
  servoRear.attach(servoFrontPin);
  servoFront.attach(servoRearPin);
  arm(servoRear); 
  delay(700);
  acc.powerOn();
}

void loop()
{
  byte msg[512];
  if (acc.isConnected()) {
    int len = acc.read(msg, sizeof(msg), 3500); // read data into msg variable
   
    for (int i=0;i<len;i++) {
      Serial.println(msg[i]);
    }

    if (len == 4) {
      int driveSpeed = msg[1];
      int turnSpeed = msg[3];

      boolean driveIsPositive = msg[0] == 1; 
      boolean turnIsPositive = msg[2] == 1; 

      if (!driveIsPositive) {
        driveSpeed = driveSpeed * -1;
      }

      if (!turnIsPositive) {
        turnSpeed = turnSpeed * -1;
      }

              setSpeed(turnSpeed, servoFront);
              setSpeed(driveSpeed, servoRear);


      //        switch (msg[0]) {
      //          case STRAIGHT : goForward(driveSpeed);
      //            break;
      //          case BACK : goBackward(driveSpeed);
      //            break;
      //          default : doStop();
      //        }
      //        switch (msg[2]) {
      //          case LEFT : turnLeft(turnSpeed);
      //            break;
      //          case RIGHT : turnRight(turnSpeed);
      //            break;        
      //          default : doStop();
      //        }
    }
  } 
  else {
    unsigned long currentMillis = millis();
    if (currentMillis - previousMillis >= shortInterval) {
      previousMillis = currentMillis;
      if (ledState == LOW)
        ledState = HIGH;
      else
        ledState = LOW;
      digitalWrite(ledPin, ledState);
    }
  }
}

