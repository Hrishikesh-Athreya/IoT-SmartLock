#include "ThingSpeak.h"
#include <ESP8266WiFi.h>
#include <Servo.h>

Servo servo;
const char ssid[] = "";  // your network SSID (name)
const char pass[] = "";   // your network password         
WiFiClient  client;
int statusCode;

//---------Channel Details---------//
unsigned long counterChannelNumber = ;            // Channel ID
const char * myCounterReadAPIKey = ""; // Read API Key
const int FieldNumber1 = 1;  // The field you wish to read  
//-------------------------------//

void setup()
{
  Serial.begin(115200);
  WiFi.mode(WIFI_STA);
  ThingSpeak.begin(client);
  
//servo motor setup
  servo.attach(2); //D4
  servo.write(0);
  delay(2000);
}

void loop()
{
  //----------------- Network -----------------//
  if (WiFi.status() != WL_CONNECTED)
  {
    Serial.print("Connecting to ");
    Serial.print(ssid);
    Serial.println(" ....");
    while (WiFi.status() != WL_CONNECTED)
    {
      WiFi.begin(ssid, pass);
      delay(5000);
    }
    Serial.println("Connected to Wi-Fi Succesfully.");
  }
  //--------- End of Network connection--------//

  //---------------- Channel 1 ----------------//
  long temp = ThingSpeak.readLongField(counterChannelNumber, FieldNumber1, myCounterReadAPIKey);
  statusCode = ThingSpeak.getLastReadStatus();
  if (statusCode == 200)
  {
    Serial.print("Status: ");
    if(temp==0){
      servo.write(0);
      Serial.println("unlocked");
    }

    if(temp==1){
      servo.write(140);
      Serial.println("locked");
    }
    
  }
  else
  {
    Serial.println("Unable to read channel / No internet connection");
  }
  delay(100);

}
