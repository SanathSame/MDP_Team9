import threading
import time
import os
from pcComm import *
from android import *
from stm32 import *
from picamera import PiCamera
from ultrasonic import *
import RPi.GPIO as GPIO

class RPI(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

        # Creating subsystem objects
        self.pcObject = PcComm()
        self.androidObject = Android()
        self.stm = STM()

        # Establish connection with other subsystems
        self.pcObject.connect()
        self.androidObject.connect()
        self.stm.connect()
        print("Connection to devices completed...")

        time.sleep(2)

        self.imgCount = 0
        self.camera = None

        #temp variables
        self.time = 0.05
        self.x1 = []
        self.y1 = []
        self.y2 = []

        '''
        ANDROID (AND):
        "ALG ROBOT XX YY D"  
        "ALG OBS 1 XX YY D || OBS 2 X Y D" (OBSTACLE ID, X-COORDINDATE, Y-COORDINATE, DIRECTION)
        "ALG STARTPF" (WEEK 8)
        "ALG STARTPARKING" (WEEK 9)
        
        
        ALG:
        "IMG TAKEPICTURE 1" (OBSTACLE NUMBER)
        "STM XXX F 10  " (XXX - COUNTER, DIRECTION, DIST/ANGLE) 
        "AND ROBOT XX YY D" (X-COORDINDATE, Y-COORDINATE, DIRECTION)
        
        STM:
        "AND XXX DONE" (XXX - COUNTER)
        
        IMG: 
        "IMG 12 Bulleyes 0 1" (IMAGE ID, CLASS_NAME, LOCATION, obstacle ID)
        "AND TARGET 1,30" (OBSTACLE ID, IMAGE ID)      
        '''

    def startThread(self):
        # Read threads created
        receiveFromImgThread = threading.Thread(target=self.receiveFromImg, args=(), name="read_Img_Thread", daemon=True)
        receiveFromAlgoThread = threading.Thread(target=self.receiveFromAlgo, args=(), name="read_Algo_Thread", daemon=True)
        receiveFromAndroidThread = threading.Thread(target=self.receiveFromAndroid, args=(), name="read_Android_Thread", daemon=True)
        receiveFromSTMThread = threading.Thread(target=self.receiveFromSTM, args=(), name="read_STM_Thread", daemon=True)

        # Makes Threads start in the background
        receiveFromImgThread.start()
        receiveFromAlgoThread.start()
        receiveFromAndroidThread.start()
        receiveFromSTMThread.start()

    def receiveFromImg(self):
        while True:
            imgMsg = self.pcObject.receiveMsgFromImg()
            if imgMsg is not None:
                print("Message received from Image: " + str(imgMsg))
                predictions = imgMsg.split()        ## IMG ImageID classname location ObstacleID
                if len(predictions) > 1:
                    if int(predictions[1]) == 12:
                        self.sendToAlgo("STOP")
                        # TODO: robot recovery from detecting bullseye
                    else:
                        self.sendToAlgo("NEXT")
                        self.sendToAndroid("TARGET " + predictions[4] + "," + predictions[1]) # "TARGET ObstacleID, ImageID"

                elif len(predictions) == 1:
                    if predictions[0] == "NOIMAGE":
                        print("Incorrect positioning of car...")
                else:
                    print("Empty prediction array...")

    def receiveFromAlgo(self):
        while True:
            algoMsg = self.pcObject.receiveMsgFromAlgo()
            if algoMsg is not None:
                print("Message received from Algo: " + str(algoMsg[2:]))  ## Start from index 2 to remove unknown symbol
                commands = algoMsg.split("\n")
                for c in commands:
                    if len(c) > 0:
                        if c[2:5] == "IMG":
                            print("Message sending to img server: " + str(c[6:] + "||"))
                            #self.useUltra()
                            self.sendToImg(c[6:])            #Send the obstacle: XX YY
                        if c[2:5] == "STM":
                            print("Message sending to STM: " + str(c[6:] + "||"))
                            self.sendToSTM(c[6:])
                        time.sleep(0.5)
                commands = None

    def sendToImg(self, msgToImg="DEFAULT_MESSAGE"):
        if msgToImg:
            self.snapPic()
            self.pcObject.sendMsgToImg(msgToImg)
            print("Message is sent to IMG server: " + str(msgToImg))

    def sendToAlgo(self, msgToAlgo):
        if msgToAlgo:
            self.pcObject.sendMsgToAlgo(msgToAlgo)
            print("Message is sent to Algo server: " + str(msgToAlgo))

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
                if androidMsg[:3] == "ALG":
                    self.sendToAlgo(androidMsg[4:])
                elif androidMsg[:3] == "IMG":
                    self.sendToImg(androidMsg[4:])

    def sendToSTM(self, msgToSTM):
        if (msgToSTM):
            self.stm.sendMsg(msgToSTM)
            print("Message send to STM is " + msgToSTM)

    def receiveFromSTM(self):
        # Enclose in while loop
        while True:
            stmMsg = self.stm.receiveMsg()
            if stmMsg is not None and ord(stmMsg[0]) != 0:
                print("Message from STM: " + str(stmMsg))
                if str(stmMsg[:6]) == "Done C":
                    self.sendToImg()
                elif str(stmMsg[:4]) == "Done":
                    self.sendToAlgo(stmMsg)

    def snapPic(self):
        try:
            self.camera.start_preview()
            self.camera.capture('a.jpeg')
            print("Captured image for sending")
            self.camera.stop_preview()

        except Exception as e:
            print("Error in taking picture..." + str(e))

    # Un-used function
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
                print("Error with Ultra: " + str(e))

    def closeAll(self):

        self.camera.close()
        self.pcObject.disconnect()
        self.androidObject.disconnect()
        self.stm.disconnect()

    def useUltra(self, count=98):
        ultrasonic = Ultrasonic()
        dist = ultrasonic.distance()
        print("Measured Distance = {:.1f} cm".format(dist))
        if dist > 20:
            self.sendToSTM("{:<3} F {:<4}".format(count, int(dist - 19)))
        time.sleep(4)
        dist = ultrasonic.distance()
        print("Measured Distance = {:.1f} cm".format(dist))
        count += 1
        self.sendToSTM("{:<3} C {:<4}".format(count, ""))
        GPIO.cleanup()

    def A5_TASK(self):
        ultrasonic = Ultrasonic()
        dist = ultrasonic.distance()
        print("Measured Distance = {:.1f} cm".format(dist))
        count = 0
        if dist > 20:
            self.sendToSTM("{:<3} F {:<4}".format(count, int(dist-19)))
        elif dist < 10:
            self.sendToSTM("{:<3} B {:<4}".format(count, int(20-dist)))
        time.sleep(4)
        dist = ultrasonic.distance()
        print("Measured Distance = {:.1f} cm".format(dist))
        count += 1

        time.sleep(0.05)
        self.sendToSTM("{:<3} C {:<4}".format(count, ""))
        GPIO.cleanup()

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
