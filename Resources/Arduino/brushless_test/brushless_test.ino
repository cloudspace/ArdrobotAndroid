// this uses the Arduino servo library included with version 0012 

// caution, this code sweeps the motor up to maximum speed !
// make sure the motor is mounted securily before running.

#include <Servo.h> 

Servo myServo;

void arm(){
  setSpeed(100);
  delay(1000); 
  setSpeed(-100);
  delay(1000); 
  setSpeed(0);
  delay(1000);   
}

void setSpeed(int speed) {
  
  //-100 - 100 
  //-100 - fullx throttle reverse 
  //100 - full throttle forward
  //0 - nuetral
  Serial.println("Speed " + String(speed));
  int angle = map(speed, -100, 100, 0, 180);
  Serial.println("Adjusted to angle " + String(angle));

  myServo.write(angle);    
}

void setup()
{
  Serial.begin(9600);
  myServo.attach(9);
  arm();  
}

void loop()
{
  if (Serial.available()) {
    int input = Serial.parseInt();
    setSpeed(input);
  }
} 
