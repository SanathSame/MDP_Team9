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
        #self.androidObject = Android()
        self.stm = STM()

        # Establish connection with other subsystems
        self.pcObject.connect()
        #self.androidObject.connect()
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
        "ALG ROBOT XX YY D"  (X-COORDINDATE, Y-COORDINATE, DIRECTION)
        "ALG OBS 1 XX YY D || OBS 2 X Y D" (OBSTACLE ID, X-COORDINDATE, Y-COORDINATE, DIRECTION)
        "ALG STARTPF" (WEEK 8)
        "ALG STARTPARKING" (WEEK 9)
        
        
        ALG:
        "IMG TAKEPICTURE 1" (OBSTACLE NUMBER)
        "STM XXX F 10  " (XXX - COUNTER, DIRECTION, DIST/ANGLE) 
        
        STM:
        "AND XXX DONE" (XXX - COUNTER)
        
        IMG: 
        "RPI TARGET 1 22" (OBSTACLE ID, IMAGE ID)      
        '''

    def startThread(self):
        # Read threads created
        receiveFromImgThread = threading.Thread(target=self.receiveFromImg, args=(), name="read_Img_Thread")
        receiveFromAlgoThread = threading.Thread(target=self.receiveFromAlgo, args=(), name="read_Algo_Thread")
        receiveFromAndroidThread = threading.Thread(target=self.receiveFromAndroid, args=(), name="read_Android_Thread")
        receiveFromSTMThread = threading.Thread(target=self.receiveFromSTM, args=(), name="read_STM_Thread")

        # Makes Threads run in the background
        receiveFromImgThread.daemon = True
        receiveFromAlgoThread.daemon = True
        receiveFromAndroidThread.daemon = True
        receiveFromSTMThread.daemon = True

        receiveFromImgThread.start()
        receiveFromAlgoThread.start()
        #receiveFromAndroidThread.start()
        receiveFromSTMThread.start()
        self.A5_TASK()

    def receiveFromImg(self):
        while True:
            imgMsg = self.pcObject.receiveMsgFromImg()
            if imgMsg is not None:
                print("Message received from Image: " + str(imgMsg))
                predictions = imgMsg.split()
                if not predictions:
                    continue
                if len(predictions) == 3:
                    if int(predictions[1]) == 12:
                        self.sendToAlgo("CONTINUE")
                    else:
                        self.sendToAlgo("STOP")
                elif len(predictions) == 1:
                    if predictions[0] == "NOIMAGE":
                        print("Incorrect positioning of car...")

                '''if imgMsg[:3] == "AND":
                    self.sendToAndroid(imgMsg[4:])'''

    def receiveFromAlgo(self):
        while True:
            algoMsg = self.pcObject.receiveMsgFromAlgo()
            if algoMsg is not None:
                print("Message received from Algo: " + str(algoMsg[2:]))  ## Start from index 2 to remove unknown symbol
                commands = algoMsg.split("\n")
                for c in commands:
                    if c[2:5] == "IMG":
                        print("Message sending to img server: " + str(c[6:] + "||"))
                        self.sendToImg()            #Send the obstacle: XX YY
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

    def sendToSTM(self, msgToSTM):
        if (msgToSTM):
            self.stm.sendMsg(msgToSTM)
            print("Message send to STM is " + msgToSTM)

    def receiveFromSTM(self):
        # Enclose in while loop
        while True:
            stmMsg = self.stm.receiveMsg()
            if stmMsg is not None and ord(stmMsg[0]) != 0:
                '''time.sleep(2)
                msg = str(input("Message for STM: "))
                if len(msg) < 6:
                    msg += " " * (6 - len(msg))
                    self.sendToSTM(msg)'''
                '''y1, y2 = int(stmMsg[:3]), int(stmMsg[4:])
                rpi.y1.append(int(y1))
                rpi.y2.append(int(y2))
                rpi.x1.append(self.time)
                self.time += 0.05'''

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

        except Exception as e:
            print("Error in taking picture...")

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

    '''def plot_values(self, x1, y1, y2):
        plt.figure(1)
        plt.plot(x1, y1, label="line 1")
        plt.plot(x1, y2, label="line 2")
        plt.xlabel('time/s')
        plt.ylabel('distance')
        plt.title('STM test')
        plt.legend()
        plt.show()'''

    def closeAll(self):
        GPIO.cleanup()
        self.camera.close()
        self.pcObject.disconnect()
        #self.androidObject.disconnect()
        '''with open("test1.csv", 'w') as f:
            f.write(str(rpi.y1[20:-10])[1:-1] + '\n')
            f.write(str(rpi.y2[20:-10])[1:-1])
            f.close()
        print("in closing....")'''
        self.stm.disconnect()

    def A5_TASK(self):
        ultrasonic = Ultrasonic()
        dist = ultrasonic.distance()
        print("Measured Distance = {:.1f} cm".format(dist))
        count = 0
        if dist > 20:
            self.sendToSTM("{:<3} F {:<4}".format(count, dist-19))
        elif dist < 10:
            self.sendToSTM("{:<3} B {:<4}".format(count, 20-dist))
        time.sleep(4)
        dist = ultrasonic.distance()
        print("Measured Distance = {:.1f} cm".format(dist))
        count += 1
        '''if (dist - 20) >= 100:
            self.sendToSTM(str(count) + "   F " + str(int(dist - 20 + 1)) + " ")
        else:
            self.sendToSTM(str(count) + "   F " + str(int(dist - 20 + 1)) + "  ")
            time.sleep(4)
            dist = ultrasonic.distance()
            print("Measured Distance = %.1f cm" % dist)
            count += 1'''

        time.sleep(0.05)
        self.sendToSTM("{:<3} C {:<4}".format(count, ""))

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
