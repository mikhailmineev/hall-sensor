void printHex(int num) {
  char tmp[3];
  sprintf(tmp, "%03x", num);
  Serial.print(tmp);
}

void setup() {
  analogReference(DEFAULT);
  Serial.begin(115200);
}

int s1;
int s2;
int s3;

String set;
String code = "";
int data = 0;
String sdata = "";

boolean highsignal = false;

void loop() {
  while (Serial.available() > 0)
  {
    int inChar = Serial.read();
    set += (char)inChar;
    if (inChar == '\n') {
      if (set.length() == 7) {
        Serial.print(set);
        code += (char)set[0];
        code += (char)set[1];
        sdata += (char)set[2];
        sdata += (char)set[3];
        sdata += (char)set[4];
        sdata += (char)set[5];
        data = sdata.toInt();
        if (code == "hs") { //high signal
          if (data == 1)
            highsignal = true;
          else
            highsignal = false;
        }
        code = "";
        sdata = "";
      }
      set = "";
    }
  }

  if (highsignal) {
    s1 = analogRead(A4);
    s2 = analogRead(A5);
    s3 = analogRead(A6);
  } else {
    s1 = analogRead(A1);
    s2 = analogRead(A2);
    s3 = analogRead(A3);
  }

  printHex(s1);
  Serial.print(':');
  printHex(s2);
  Serial.print(':');
  printHex(s3);
  Serial.print(':');
  Serial.print('\n');
}
