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
}

void loop()
{
  byte msg[512];
  if (acc.isConnected()) {
    int len = acc.read(msg, sizeof(msg), 1); // read data into msg variable

    if (len > 0) {
      if (len == 1) {
        doStop();
      } else if (len == 2) {
        int speed = (255 * msg[1]) / 100;
        switch (msg[0]) {
          case LEFT : turnLeft(speed);
            break;
          case RIGHT : turnRight(speed);
            break;
          case STRAIGHT : goForward(speed);
            break;
          case BACK : goBackward(speed);
            break;
          default : doStop();
        }
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

void goForward(int speed) {
  if (DRIVETRAIN == 6) {
    motor_rear.setSpeed(speed);
    motor_rear.run(FORWARD);
  }
}
void goBackward(int speed) {
  if (DRIVETRAIN == 6) {
    motor_rear.setSpeed(speed);
    motor_rear.run(BACKWARD);
  }
}
void turnRight(int speed) {
  if (DRIVETRAIN == 6) {
    motor_front.setSpeed(speed);
    motor_front.run(BACKWARD);
  }
}
void turnLeft(int speed) {
  if (DRIVETRAIN == 6) {
    motor_front.setSpeed(speed);
    motor_front.run(FORWARD);
  }
}
void doStop() {
  if (DRIVETRAIN == 6) {
    motor_front.run(RELEASE);
    motor_rear.run(RELEASE);
  }
}
