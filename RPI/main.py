import threading
import time
import os
from pcComm import *
from android import *
from stm32 import *
from picamera import PiCamera
# from picamera.array import PiRGBArray

class RPI(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

        # Creating subsystem objects
        self.pcObject = PcComm()
        self.androidObject = Android()
        self.stm = STM()

        # Establish connection with other subsystems
        #self.pcObject.connect()
        self.androidObject.connect()
        self.stm.connect()
        print("Connecting to other devices...")

        time.sleep(3)

        self.imgCount = 0
        self.camera = None
        self.SEPARATOR = "@.@"

    def startThread(self):
        # pc send thread created
        #sendToPcThread = threading.Thread(target=self.sendToPc, args=(), name="send_Pc_Thread")

        # pc read thread created
        receiveFromPcThread = threading.Thread(target=self.receiveFromPc, args=(), name="read_Pc_Thread")
        receiveFromAndroidThread = threading.Thread(target=self.receiveFromAndroid, args=(), name="read_Android_thread")
        receiveFromSTMThread = threading.Thread(target=self.receiveFromSTM, args=(), name="read_STM_thread")

        # Makes Threads run in the background
        #sendToPcThread.daemon = True
        receiveFromPcThread.daemon = True
        receiveFromAndroidThread.daemon = True
        receiveFromSTMThread.daemon = True

        receiveFromPcThread.start()
        receiveFromAndroidThread.start()
        receiveFromSTMThread.start()

    def sendToPc(self, msgToPc):
        if msgToPc:
            self.pcObject.sendMsg(msgToPc)
            print("Message is sent to PC: " + str(msgToPc))

    def receiveFromPc(self):
        while True:
            self.pcObject.receiveMsg()
            print("Message received from PC: " + str(self.pcObject.msgQueue))
            while len(self.pcObject.msgQueue):
                command = self.pcObject.dequeue()
                print("Current command: " + command)
                if command != "":
                    if command.upper() == "P":
                        self.snapPic()
                        self.imgCount += 1
                        print("Image taken!")
                    if command.upper() == "S":
                        #for i in range(1, 11):
                        #fileName = "image" + "{:02d}".format(i) + ".jpg"
                        fileName = "image01.jpg"
                        fileSize = os.path.getsize(fileName)
                        print("filename: " + str(fileName) +"\n, file size: "+ str(fileSize))
                        #self.pcObject.serverSocket.send(f"{fileName}{self.SEPARATOR}{fileSize}".encode())

                        time.sleep(1)
                        print("after sleep")
                        sendToPcThread = threading.Thread(target=self.pcObject.sendImage, args=(fileName, fileSize), name="send_Pc_Thread")
                        sendToPcThread.daemon = True
                        sendToPcThread.start()

            break

    def sendToAndroid(self, msgToAndroid):
        if (msgToAndroid):
            self.androidObject.send(msgToAndroid)
            print("Message send to Android is " + msgToAndroid)

    def receiveFromAndroid(self):
        androidMsg = str(self.androidObject.receiveMsg())
        print("Message from android: " + androidMsg)
        if androidMsg.upper() == "W":
            print("Move forward")

    def sendToSTM(self, msgToSTM):
        if (msgToSTM):
            self.arduino_obj.send(msgToSTM)
            print("Message send to Arduino is " + msgToSTM)

    def receiveFromSTM(self):
        stmMsg = str(self.stm.receiveMsg())
        print("Message from STM: " + stmMsg)

    def snapPic(self):
        try:
            self.camera.start_preview()
            for i, filename in enumerate(self.camera.capture_continuous('image{counter:02d}.jpg')):
                print(filename)
                time.sleep(1)
                if i == 1:
                    break
        except Exception as e:
            print("Error in taking picture...")

    def closeAll(self):
        self.pcObject.disconnect()
        self.androidObject.disconnect()
        self.stm.disconnect()


if __name__ == "__main__":
    rpi = RPI()
    try:
        rpi.camera = PiCamera()
        rpi.camera.resolution = (640, 480)
        rpi.startThread()
        while True:
            pass
    except KeyboardInterrupt:
        rpi.closeAll()
