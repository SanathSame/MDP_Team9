import threading
import time
import os
from pcComm import *
from android import *
from stm32 import *
from picamera import PiCamera
from ultrasonic import *
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
        #self.stm.connect()
        print("Connecting to other devices...")

        time.sleep(2)

        self.imgCount = 0
        self.camera = None
        #self.SEPARATOR = "@.@"

    def startThread(self):
        # pc send thread created
        #sendToPcThread = threading.Thread(target=self.sendToPc, args=(), name="send_Pc_Thread")
        #sendToSTMThread = threading.Thread(target=self.sendToSTM, args=(), name="send_STM_Thread")

        # pc read thread created
        receiveFromImgThread = threading.Thread(target=self.receiveFromImg, args=(), name="read_Img_Thread")
        receiveFromAlgoThread = threading.Thread(target=self.receiveFromAlgo, args=(), name="read_Algo_Thread")
        receiveFromAndroidThread = threading.Thread(target=self.receiveFromAndroid, args=(), name="read_Android_Thread")
        receiveFromSTMThread = threading.Thread(target=self.receiveFromSTM, args=(), name="read_STM_Thread")

        # Makes Threads run in the background
        receiveFromImgThread.daemon = True
        receiveFromAlgoThread.daemon = True
        receiveFromAndroidThread.daemon = True
        receiveFromSTMThread.daemon = True

        # receiveFromImgThread.start()
        #receiveFromAlgoThread.start()
        receiveFromAndroidThread.start()
        #receiveFromSTMThread.start()

    def receiveFromImg(self):
        while True:
            imgMsg = self.pcObject.receiveMsgFromImg()
            if imgMsg:
                print("Message received from Image: " + str(imgMsg))

    def receiveFromAlgo(self):
        while True:
            algoMsg = self.pcObject.receiveMsgFromAlgo()
            if algoMsg:
                print("Message received from Algo: " + str(algoMsg))

    def sendToImg(self, msgToImg="DEFAULT_MESSAGE"):
        print("Send to Img in main.py called")
        if msgToImg:
            self.pcObject.sendMsgToImg(self.camera, msgToImg)
            print("Message is sent to PC: " + str(msgToImg))

    def sendToAlgo(self, msgToAlgo):
        if msgToAlgo:
            self.pcObject.sendMsgToAlgo(msgToAlgo)
            print("Message is sent to PC: " + str(msgToAlgo))

    '''def receiveFromPc(self):
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

            break'''

    def sendToAndroid(self, msgToAndroid):
        if (msgToAndroid):
            self.androidObject.sendMsg(msgToAndroid)
            print("Message send to Android is " + msgToAndroid)

    def receiveFromAndroid(self):
        # Enclose in while loop
        while True:
            androidMsg = self.androidObject.receiveMsg()
            if androidMsg:
                print("Message from android: " + androidMsg)
                if androidMsg.upper() == "W":
                    tmp = input("Message please: ")
                    self.sendToAndroid(tmp)

    '''
    w s - control motor: drive rear wheel, value from 1000 to 3500 (speed)
    a d - servo(front) : left 60, right 86, center is 73
    
    letter(CAPS) value 
    '''
    def sendToSTM(self, msgToSTM):
        if (msgToSTM):
            self.stm.sendMsg(msgToSTM)
            print("Message send to Arduino is " + msgToSTM)

    def receiveFromSTM(self):
        # Enclose in while loop
        while True:
            stmMsg = self.stm.receiveMsg()
            if stmMsg:
                print("Message from STM: " + stmMsg)
                time.sleep(2)
                msg = str(input("Message for STM: "))
                if len(msg) < 6:
                    msg += " " * (6 - len(msg))
                    self.sendToSTM(msg)

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
    
    def readUltra(self):
        ultrasonic = Ultrasonic()
        values = [] 
        while True:
            try:
                dist = ultrasonic.distance()
                print(dist)
                values.append(dist)
                time.sleep(1)
            except KeyboardInterrupt:
                ultrasonic.cleanup()
            except Exception as e:
                print("Error with Ultra: %s" %str(e))

    def closeAll(self):
        self.pcObject.disconnect()
        self.androidObject.disconnect()
        self.stm.disconnect()
        self.camera.close()


if __name__ == "__main__":
    rpi = RPI()
    try:
        rpi.camera = PiCamera()
        rpi.camera.resolution = (640, 480)
        # rpi.startThread()
        print("Gonna sleep first")
        
        time.sleep(3)
        rpi.sendToImg()

    except KeyboardInterrupt:
        rpi.closeAll()
