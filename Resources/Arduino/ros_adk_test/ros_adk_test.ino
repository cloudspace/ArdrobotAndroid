/*
 * rosserial Publisher Example
 * Prints "hello world!"
 */

#include <ros.h>
#include <std_msgs/String.h>

#include <adk.h>

USB Usb;
ADK adk(&Usb, "ArdroBot",
        "ArdroBot",
        "ArdroBot",
        "1.0",
        "http://ArdroBot.com",
        "0000000012345678");

ros::NodeHandle  nh;

std_msgs::String str_msg;
ros::Publisher chatter("chatter", &str_msg);

char hello[13] = "hello world!";
boolean connected;

void setup()
{
  Serial.begin(115200);
  Serial.print("\r\nArduino Blink LED Started");
  while (!Serial); // Wait for serial port to connect - used on Leonardo, Teensy and other boards with built-in USB CDC serial connection
  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }
  nh.initNode();
  nh.advertise(chatter);
}

void loop()
{
  Usb.Task();

  if (adk.isReady()) {
    if (!connected) {
      connected = true;
    }

    str_msg.data = hello;
    chatter.publish( &str_msg );
    nh.spinOnce();
    delay(1000);
  }
}
