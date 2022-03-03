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
        #self.pcObject = PcComm()
        #self.androidObject = Android()
        self.stm = STM()

        # Establish connection with other subsystems
        #self.pcObject.connect()
        #self.androidObject.connect()
        self.stm.connect()
        print("Connecting to other devices...")

        time.sleep(2)

        self.imgCount = 0
        self.camera = None

        #temp variables
        self.time = 0.05
        self.x1 = []
        self.y1 = []
        self.y2 = []

    def startThread(self):
        # Read threads created
        #receiveFromImgThread = threading.Thread(target=self.receiveFromImg, args=(), name="read_Img_Thread")
        receiveFromAlgoThread = threading.Thread(target=self.receiveFromAlgo, args=(), name="read_Algo_Thread")
        receiveFromAndroidThread = threading.Thread(target=self.receiveFromAndroid, args=(), name="read_Android_Thread")
        receiveFromSTMThread = threading.Thread(target=self.receiveFromSTM, args=(), name="read_STM_Thread")

        # Makes Threads run in the background
        #receiveFromImgThread.daemon = True
        #receiveFromAlgoThread.daemon = True
        receiveFromAndroidThread.daemon = True
        receiveFromSTMThread.daemon = True

        #receiveFromImgThread.start()
        #receiveFromAlgoThread.start()
        #receiveFromAndroidThread.start()
        #self.sendToSTM("F 100 ")            #Hardcoded msg to be sent to STM
        #self.sendToSTM("S     ")
        receiveFromSTMThread.start()

        count = 0
        while True:
            self.sendToSTM("{:<3} F {:<4}".format(count, 100))
            #A = input("hhi")
            count += 1
            time.sleep(0.2)
            self.sendToSTM("{:<3} B {:<4}".format(count, 100))
            count += 1
            #time.sleep(2)
            b = input("hhi")

        #self.sendToSTM(str(0) + "   LF 360")

        '''for i in range(0, 10, 2):
            self.sendToSTM(str(i) + "   F 10  ")
            time.sleep(0.5)
            self.sendToSTM(str(i+1) + "   B 10  ")
            time.sleep(0.5)'''
        '''self.sendToSTM("{:<3} F {:<4}".format(0, 35))
        time.sleep(0.5)
        #A = input("hihi")
        self.sendToSTM("1   RF 90 ")
        time.sleep(0.5)
        #A = input("hihi")
        self.sendToSTM("2   F 12  ")
        time.sleep(0.5)
        #A = input("hihi")
        self.sendToSTM("3   LF 190")
        time.sleep(0.5)
        #A = input("hihi")
        self.sendToSTM("4   C     ")
        time.sleep(0.5)'''

        # self.sendToSTM("5   C     ")
        # '''time.sleep(20)
        # self.sendToSTM("5   F 1000")
        # time.sleep(0.05)
        # self.sendToSTM("6   RF 360")
        # time.sleep(0.05)
        # self.sendToSTM("7   B 900 ")'''

    def receiveFromImg(self):
        while True:
            imgMsg = self.pcObject.receiveMsgFromImg()
            if imgMsg:
                print("Message received from Image: " + str(imgMsg))
                if imgMsg[:4] == "IMG":
                    self.sendToAndroid(imgMsg[4:])

    def receiveFromAlgo(self):
        while True:
            algoMsg = self.pcObject.receiveMsgFromAlgo()
            if algoMsg:
                print("Message received from Algo: " + str(algoMsg))
                if algoMsg[:7] == "TAKEPIC":
                    self.snapPic()
                    self.sendToImg(algoMsg[7:]) #Send the obstacle: XX YY
                elif algoMsg[0:4] == "STM":
                    self.sendToSTM(algoMsg[5:])

    def sendToImg(self, msgToImg="DEFAULT_MESSAGE"):
        if msgToImg:
            self.pcObject.sendMsgToImg(msgToImg)
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
                print("Message from android: " + androidMsg + "|||")
                if androidMsg.upper() == "W":
                    tmp = input("Message please: ")
                    self.sendToAndroid(tmp)
                elif androidMsg[0:3] == "STM":
                    self.sendToSTM(androidMsg[4:])

    '''
    w s - control motor: drive rear wheel, value from 1000 to 3500 (speed)
    a d - servo(front) : left 60, right 86, center is 73
    
    letter(CAPS) value 
    '''
    def sendToSTM(self, msgToSTM):
        if (msgToSTM):
            self.stm.sendMsg(msgToSTM)
            print("Message send to STM is " + msgToSTM)
        '''while True:
            msgToSTM = input("Enter your command (6 chars): ")
            self.stm.sendMsg(str(msgToSTM))'''

    def receiveFromSTM(self):
        # Enclose in while loop
        while True:
            stmMsg = self.stm.receiveMsg()
            if stmMsg is not None and ord(stmMsg[0]) != 0:
                print("Message from STM: " + stmMsg)       #Comment for debug

                # if "Done C" in stmMsg:
                #     self.sendToSTM("0   F 40  ")
                #     time.sleep(0.5)
                #     self.sendToSTM("1   LF 90 ")
                #     time.sleep(0.5)
                #     self.sendToSTM("2   B 100 ")

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


    def snapPic(self):
        try:
            self.camera.start_preview()
            self.camera.capture('a.jpeg')
            print("Captured image for sending")

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
                print("Error with Ultra: %s" % str(e))

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
        #self.pcObject.disconnect()
        #self.androidObject.disconnect()
        '''with open("test1.csv", 'w') as f:
            f.write(str(rpi.y1[20:-10])[1:-1] + '\n')
            f.write(str(rpi.y2[20:-10])[1:-1])
            f.close()
        print("in closing....")'''
        self.stm.disconnect()
        #self.camera.close()


if __name__ == "__main__":
    rpi = RPI()
    try:
        #rpi.camera = PiCamera()
        #rpi.camera.resolution = (640, 480)
        '''ultrasonic = Ultrasonic()
        dist = ultrasonic.distance()
        print("Measured Distance = %.1f cm" % dist)
        count = 0
        time.sleep(2)
        while dist > 20:
            if (dist-20) >= 100:
                rpi.sendToSTM(str(count) + "   F " + str(int(dist-20 +1)) + " ")
                time.sleep(4)
                dist = ultrasonic.distance()
                print("Measured Distance = %.1f cm" % dist)
                count += 1
            else:
                rpi.sendToSTM(str(count) + "   F " + str(int(dist-20+1)) + "  ")
                time.sleep(4)
                dist = ultrasonic.distance()
                print("Measured Distance = %.1f cm" % dist)
                count += 1

        rpi.sendToSTM(str(count) + "   C     ")

        GPIO.cleanup()'''
        rpi.startThread()
        while True:
            pass
    except KeyboardInterrupt:
        #rpi.sendToSTM(str(count) + "   C     ")
        rpi.closeAll()
        GPIO.cleanup()
