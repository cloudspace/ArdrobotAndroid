#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include <Servo.h>

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
const int FORWARD = 3;
const int BACK = 4;
const int STOP = 5;

Servo servoLeft, servoRight;


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
  byte msg[0];
  if (acc.isConnected()) {
    int len = acc.read(msg, sizeof(msg), 1); // read data into msg variable

    if (len > 0) {
      servoRight.attach(servoRightPin);
      servoLeft.attach(servoLeftPin);
      delay(100);
      switch (len) {
        case LEFT : turnLeft();
          break;
        case RIGHT : turnRight();
          break;
        case FORWARD : goForward();
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
    servoLeft.write(180);
    servoRight.write(180);
  }
}
void goBackward() {
  Serial.println("Back");
  if (DRIVETRAIN == 6) {
    servoLeft.write(-180);
    servoRight.write(-180);
  }
}
void turnRight() {
  Serial.println("Right");
  if (DRIVETRAIN == 6) {
    servoLeft.write(180);
    servoRight.write(90);
  }
}
void turnLeft() {
  Serial.println("Left");
  if (DRIVETRAIN == 6) {
    servoLeft.write(90);
    servoRight.write(180);
  }
}
void doStop() {
  Serial.println("Stop");
  if (DRIVETRAIN == 6) {
    servoLeft.detach();
    servoRight.detach();
  }
}
