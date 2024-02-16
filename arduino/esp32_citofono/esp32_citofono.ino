#include <WiFi.h>
#include <HTTPClient.h>
#include <ESPAsyncWebSrv.h>
#include <esp_camera.h>
#include <ArduinoOTA.h>

#define LED_BUILTIN 4
struct WiFiCredentials {
  const char* ssid;
  const char* password;
};

WiFiCredentials wifiNetworks[] = {
  {"WifiChianeseSUDCucina_2.4ghz", "canepufoente()1"},
  {"WifiChianeseSUD", "canepufoente()1"},
  {"gausscam_2.4ghz", "canepufoente()1"},
  {"WifiChianese", "canepufoente()1"},
  {"WifiChianeseDL", "canepufoente()1"}
  
};

// Create an instance of the server
AsyncWebServer server(80);
void ledblinker(int delayms, int times){
  for(int i = 0; i < times; i++){
    digitalWrite(LED_BUILTIN, LOW);
    delay(delayms);
    digitalWrite(LED_BUILTIN, HIGH);
    delay(delayms);
    digitalWrite(LED_BUILTIN, LOW);
    
  }
}
void checkNetwork() {
  int n = WiFi.scanNetworks();
  Serial.println("scan done");
  if (n == 0) {
    Serial.println("no networks found");
  } else {
    Serial.print(n);
    Serial.println(" networks found");

    int maxSignalStrength = -100;
    String bestNetwork;
    String bestPassword;

    for (int i = 0; i < n; ++i) {
      String ssid = WiFi.SSID(i);
      for (int j = 0; j < sizeof(wifiNetworks)/sizeof(wifiNetworks[0]); j++) {
        if (ssid == wifiNetworks[j].ssid) {
          int rssi = WiFi.RSSI(i);
          if (rssi > maxSignalStrength) {
            maxSignalStrength = rssi;
            bestNetwork = ssid;
            bestPassword = wifiNetworks[j].password;
          }
        }
      }
    }

    if (bestNetwork != "") {
      WiFi.begin(bestNetwork.c_str(), bestPassword.c_str());
      Serial.println("Connecting to WiFi: " + bestNetwork);
      unsigned long startTime = millis();
      while (WiFi.status() != WL_CONNECTED) {
        if (millis() - startTime > 10000) {
          Serial.println("Failed to connect to " + bestNetwork);
          break;
        }
        delay(1000);
      }
    } else {
      Serial.println("No known networks found");
    }
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("Connected to WiFi");
    ledblinker(50, 10);
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
    String message = "esp32 citofono, boot-up";
    message = message + " IP address: " + WiFi.localIP().toString();
    message = message + " MAC address: " + WiFi.macAddress();
    message = message + " RSSI: " + WiFi.RSSI();
    message = message + " SSID: " + WiFi.SSID();
    message = message + " BSSID: " + WiFi.BSSIDstr();
    message = message + " Gateway: " + WiFi.gatewayIP().toString();
    message = message + " Subnet: " + WiFi.subnetMask().toString();
    message = message + " DNS: " + WiFi.dnsIP().toString();
    message = message + " Hostname: " + WiFi.getHostname();
    message = message + " AutoConnect: " + WiFi.getAutoConnect();

    sendEmail(message);
  } else {
    Serial.println("Failed to connect to any WiFi");
  }
}
void setup() {
  // Initialize Serial
  //ArduinoOTA.begin();
  Serial.begin(9600);
  pinMode (LED_BUILTIN, OUTPUT);
  ledblinker(100, 5);
  
  // WiFi network scan
  checkNetwork();

// Camera configuration
camera_config_t config;
config.ledc_channel = LEDC_CHANNEL_0;
config.ledc_timer = LEDC_TIMER_0;
config.pin_d0 = 5;
config.pin_d1 = 18;
config.pin_d2 = 19;
config.pin_d3 = 21;
config.pin_d4 = 36;
config.pin_d5 = 39;
config.pin_d6 = 34;
config.pin_d7 = 35;
config.pin_xclk = 0;
config.pin_pclk = 22;
config.pin_vsync = 25;
config.pin_href = 23;
config.pin_sscb_sda = 26;
config.pin_sscb_scl = 27;
config.pin_pwdn = 32;
config.pin_reset = -1;
config.xclk_freq_hz = 20000000;
config.pixel_format = PIXFORMAT_JPEG;
config.frame_size = FRAMESIZE_SVGA; // Reduced frame size
config.jpeg_quality = 5; // Reduced JPEG quality
config.fb_count = 1;

// Initialize camera
esp_err_t err = esp_camera_init(&config);
if (err != ESP_OK) {
  Serial.printf("Camera init failed with error 0x%x", err);
  return;
}

server.on("/image", HTTP_GET, [](AsyncWebServerRequest *request){
  camera_fb_t * fb = NULL;
  fb = esp_camera_fb_get();  
  if(!fb) {
    Serial.println("Camera capture failed");
    return;
  }

  // Save image data to a buffer
  uint8_t* buf = new uint8_t[fb->len];
  memcpy(buf, fb->buf, fb->len);

  // Return the frame buffer to the camera
  esp_camera_fb_return(fb);

  // Send the buffered image data
  AsyncWebServerResponse *response = request->beginResponse_P(200, "image/jpeg", buf, fb->len);
  response->addHeader("Content-Disposition", "inline; filename=capture.jpg");
  request->send(response);

  // Delete the buffer
  delete[] buf;
});

  server.on("/ledon", HTTP_GET, [](AsyncWebServerRequest *request){
    digitalWrite(4, HIGH); // Assuming LED is connected to GPIO 4
    request->send(200, "text/plain", "LED is on");
  });

  server.on("/ledoff", HTTP_GET, [](AsyncWebServerRequest *request){
    digitalWrite(4, LOW); // Assuming LED is connected to GPIO 4
    request->send(200, "text/plain", "LED is off");
  });

  // Start the server
  server.begin();
}
unsigned long lastCheck = 0;

void loop() {
  // Check internet connection every 10 seconds
  if(millis() - lastCheck > 10000) {
    lastCheck = millis();
    if(!checkInternetConnection()) {
      connectNetwork();
    }
  }

  // Handle incoming requests
}
void sendEmail(String message) {
  HTTPClient http;
  String serverPath = "http://192.168.1.30/phploggervari/mailto.php?destinatario=domenicanoleo@gmail.com&oggetto=esp32";
  
  http.begin(serverPath.c_str());
  http.addHeader("Content-Type", "application/x-www-form-urlencoded");
  
  String httpRequestData = "corpo=" + message;
  int httpResponseCode = http.POST(httpRequestData);
  
  if (httpResponseCode>0) {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
  }
  else {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
  // Free resources
  http.end();
}
void connectNetwork() {
  int connectionAttempts = 0;
  int networkIndex = 0;

  while (WiFi.status() != WL_CONNECTED) {
    if(connectionAttempts < 10) {
      WiFi.begin(wifiNetworks[networkIndex].ssid, wifiNetworks[networkIndex].password);
      Serial.println("Connecting to WiFi... attempt: " + String(connectionAttempts));
    } else if(networkIndex < sizeof(wifiNetworks)/sizeof(wifiNetworks[0]) - 1) {
      networkIndex++;
      connectionAttempts = 0;
    } else {
      Serial.println("Failed to connect to all available networks.");
      break;
    }
    delay(5000);
    connectionAttempts++;
  }
}

bool checkInternetConnection() {
  HTTPClient http;
  http.begin("http://www.google.com");
  int httpCode = http.GET();
  http.end();
  return httpCode == 200;
}