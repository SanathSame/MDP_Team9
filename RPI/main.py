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
        self.commandCounter = 0         # Tracks the current command_ID that has been COMPLETED by the STM
        self.curObstacle = 0            # Stores the current obstacle ID robot is heading towards
        self.imgLocation = -1           # Stores the img Location of the current obstacle, set to -1 to no location
        self.adjustingFlag = False
        self.initialAdjust = True
        self.initialLocation = -1

        #temp variables
        self.time = 0.05
        self.x1 = []
        self.y1 = []
        self.y2 = []

        self.seen_ids = []

        '''
        ANDROID (AND):
        "ALG ROBOT XX YY D"  
        "ALG OBS 1 XX YY D || OBS 2 X Y D" (OBSTACLE ID, X-COORDINDATE, Y-COORDINATE, DIRECTION)
        "ALG STARTPF" (WEEK 8)
        "ALG STARTPARKING" (WEEK 9)
        
        RPI:
        Send to IMG: "ADJUST 1" (OBSTACLE NUMBER)
        Send to IMG: "TAKEPICTURE 1" (OBSTACLE NUMBER)
        
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

    def get_command_for_adjustment(self, img_location, adjustment_counter):
        should_move_forward = adjustment_counter % 2 == 0

        if img_location < 2:
            direction = "LF" if should_move_forward else "RB"
            angle = 20 if img_location == 0 else 10
            return "{:<3} {} {:<3}".format(self.commandCounter, direction, angle)
        elif img_location == 2:
            return "{:<3} A {:<4}".format(self.commandCounter, self.initialLocation)
        else:
            direction = "RF" if should_move_forward else "LB"
            angle = 20 if img_location == 4 else 10
            return "{:<3} {} {:<3}".format(self.commandCounter, direction, angle)

    def receiveFromImg(self):
        times_to_adjust = 0
        while True:
            imgMsg = self.pcObject.receiveMsgFromImg()

            if imgMsg is None:
                print("Empty prediction array")
                continue

            print("Message received from Image: " + str(imgMsg))
            predictions = imgMsg.split()        ## returns IMG ImageID classname location ObstacleID or ADJUST ImageID classname location ObstacleID

            if len(predictions) == 1 and predictions[0] == "NOIMAGE":
                self.sendToAlgo("NOIMAGE")
                print("Incorrect positioning of car...")
                continue

            '''if predictions[0] == "ADJUST" and predictions[1] == "NOIMAGE":
                #self.commandCounter += 1
                self.sendToSTM("{:<3} B {:<4}".format(self.commandCounter, 10))  # Send -1 if there is no image?
                #self.adjustingFlag = True
                #self.centraliseImage()
                # TODO: check with stm the data to send for no image'''
            if predictions[0] == "IMG":
                # times_to_adjust = 0
                # if int(predictions[1]) == 10:       ## ImageID of bullseye is 10
                #     self.sendToAlgo("STOP")
                #     # TODO: robot recovery from detecting bullseye
                # else:
                prediction_id = int(predictions[1])

                if prediction_id in self.seen_ids or prediction_id == 10:
                    self.sendToAlgo("NOIMAGE")
                    print("Seen the image already")
                    continue

                self.seen_ids.append(prediction_id)
                self.sendToAlgo("NEXT")
                self.sendToAndroid("TARGET " + predictions[4] + "," + predictions[1]) # "TARGET ObstacleID, ImageID"
                continue
            '''elif predictions[0] == "ADJUST":
                #self.commandCounter += 1
                if self.initialAdjust:
                    self.initialLocation = str(predictions[3])
                    self.initialAdjust = False
                # self.sendToSTM("{:<3} A {:<4}".format(self.commandCounter, predictions[3]))

                self.imgLocation = int(predictions[3])
                #self.sendToSTM(self.get_command_for_adjustment(self.imgLocation, times_to_adjust))
                times_to_adjust += 1
                self.centraliseImage()'''

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
                            self.sendToImg(c[6:])            #Send the obstacle: XX YY
                            #self.curObstacle = int(c[6:].split()[1])
                        if c[2:5] == "STM":
                            print("Message sending to STM: " + str(c[6:] + "||"))
                            self.sendToSTM(c[6:])
                        time.sleep(0.1)

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
                time.sleep(0.1)
                '''elif str(stmMsg[:6]) == "Done A":
                    self.sendToImg("TAKEPICTURE " + str(self.curObstacle))'''

                #self.commandCounter = stmMsg.split()[1]     #Update the command counter

                '''if int(stmMsg[5:8]) == self.commandCounter:
                    print("Centralise the image for ultra...")
                    self.adjustingFlag = True
                    self.centraliseImage()'''

    def centraliseImage(self):
        if self.imgLocation != 2 and self.adjustingFlag:
            print("CENTRALISING IMAGE")
            self.sendToImg("ADJUST " + str(self.curObstacle))
            self.adjustingFlag = False
        else:
            self.sendToImg("TAKEPICTURE " + str(self.curObstacle))

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
