#include <SoftwareSerial.h>
#include <Servo.h>

SoftwareSerial BTSerial(2, 3);
Servo myservo_x;  //가로 서보
Servo myservo_y;  //세로 서보

union Point {
  long int val;
  char bytes[4];
};

typedef struct packet {
  Point currentX;
  Point currentY;
} Packet;

void changeEndian(Point *ptr) {
  char temp;
  temp = ptr->bytes[0];
  ptr->bytes[0] = ptr->bytes[3];
  ptr->bytes[3] = temp;
  temp = ptr->bytes[1];
  ptr->bytes[1] = ptr->bytes[2];
  ptr->bytes[2] = temp;
}

void readPacket(SoftwareSerial *serial, Packet *packet) {
  serial->readBytes(packet->currentX.bytes, sizeof(long int));
  serial->readBytes(packet->currentY.bytes, sizeof(long int));

  changeEndian(&packet->currentX);
  changeEndian(&packet->currentY);
}

int current_x_Angle = 90;
int current_y_Angle = 80;

void setup() {
  BTSerial.begin(9600);
  Serial.begin(9600);
  myservo_x.attach(9);
  myservo_y.attach(10);
  myservo_x.write(current_x_Angle);
  myservo_y.write(current_y_Angle);
}


int flag = 0;

void loop() {

  
  while (BTSerial.available()) {
    flag = 1;
    Packet packet;
    readPacket(&BTSerial, &packet);

    // 좌표확인
    Serial.print("currentX : "); Serial.println(packet.currentX.val);
    Serial.print("currentY : "); Serial.println(packet.currentY.val);

    // 리셋
    if ((packet.currentX.val == 10000) && (packet.currentY.val == 10000))
    {
      myservo_x.write(90);
      myservo_y.write(80);
      delay(180);
      current_x_Angle = 90;
      current_y_Angle = 80;
      BTSerial.flush();
      break;
    }

    // 좌표 오류 거르기
    if ( packet.currentX.val > 310 || packet.currentX.val < 0 ) break;
    if ( packet.currentY.val > 220 || packet.currentY.val < 0 ) break;

    // x좌표 각도변화량 계산 (300*220 해상도)
    int x_delta_angle = 0.4 * (30 * (150 - packet.currentX.val) / 150);
    int y_delta_angle = 0.4 * (20 * (packet.currentY.val - 110) / 110);

    // 조그만 이동 무시
    if (x_delta_angle < 3 && x_delta_angle > -3) x_delta_angle = 0;
    if (y_delta_angle < 2 && y_delta_angle > -2) y_delta_angle = 0;

    // delta 각도 출력
    Serial.print( "x delta = "); Serial.println(x_delta_angle);
    Serial.print( "y delta = "); Serial.println(y_delta_angle);

    //// x좌표 이동
    current_x_Angle += x_delta_angle;
    if (current_x_Angle > 180) current_x_Angle = 180;
    if (current_x_Angle < 0  ) current_x_Angle = 0;
    myservo_x.write(current_x_Angle);


    //// y좌표 이동
    current_y_Angle += y_delta_angle;
    if (current_y_Angle > 120) current_y_Angle = 120;
    if (current_y_Angle < 60  ) current_y_Angle = 60;
    myservo_y.write(current_y_Angle);


    delay(170);

  }
}

