import serial
import time


class STM():
    def __inti__(self):
        self.BAUD_RATE = 115200
        self.SERIAL_PORT = "/dev/ttyUSB0"
        self.service = None
        self.STM_MSG_LENGTH = 10

    def connect(self):
        try:
            self.service = serial.Serial(self.SERIAL_PORT, self.BAUD_RATE, parity=serial.PARITY_NONE,
                stopbits=serial.STOPBITS_ONE,bytesize=serial.EIGHTBITS,timeout=5)
            time.sleep(1)

            if not self.service:
                print("Connected to STM...")

        except Exception as e:
            print("Error in connecting to STM: " + str(e))

    def sendMsg(self, msg):
        try:
            print('Sending message: ' + str(msg))
            self.service.write(msg.encode('utf-8'))
        except Exception as err:
            print('Error in sending message...')

    def receiveMsg(self):
        try:
            msg = self.service.read(5)
            if len(msg) > 0:
                return msg.decode('utf-8')
        except Exception as e:
            print('Error met in reading ' + str(e))

    def disconnect(self):
        try:
            if self.service:
                self.service.close()
                print('Serial connection closed...')
        except Exception as e:
            print("Error in disconnecting from STM..." + str(e))