#include <AFMotor.h>

#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define DRIVETRAIN 6
#define TURNSPEED 225
#define DRIVESPEED 255
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

AF_DCMotor motor_front(1, MOTOR12_64KHZ);
AF_DCMotor motor_rear(3);

void setup()
{
  // set communiation speed
  Serial.begin(115200);
  pinMode(ledPin, OUTPUT);
  delay(700);
  acc.powerOn();

  if (DRIVETRAIN == 6) {
    motor_front.setSpeed(TURNSPEED);
    motor_rear.setSpeed(DRIVESPEED);
  }
}

void loop()
{
  byte msg[0];
  if (acc.isConnected()) {
    int len = acc.read(msg, sizeof(msg), 1); // read data into msg variable

    if (len > 0) {      
      delay(100);
      switch (len) {
        case LEFT : turnLeft();
          break;
        case RIGHT : turnRight();
          break;
        case STRAIGHT : goForward();
          break;
        case BACK : goBackward();
          break;
        default : doStop();
      }
    }
  } else {
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

void goForward() {

  Serial.println("Forward");

  if (DRIVETRAIN == 6) {
    motor_front.run(RELEASE);
    motor_rear.run(FORWARD);
  }
}
void goBackward() {
  Serial.println("Backward");

  if (DRIVETRAIN == 6) {
    motor_front.run(RELEASE);
    motor_rear.run(BACKWARD);
  }
}
void turnRight() {
  Serial.println("Right");

  if (DRIVETRAIN == 6) {
    motor_front.run(BACKWARD);
    // motor_rear.run(FORWARD);
  }
}
void turnLeft() {
  Serial.println("Left");

  if (DRIVETRAIN == 6) {
    motor_front.run(FORWARD);
    // motor_rear.run(FORWARD);
  }
}
void doStop() {
  Serial.println("Stop");

  if (DRIVETRAIN == 6) {
    motor_front.run(RELEASE);
    motor_rear.run(RELEASE);
  }
}
