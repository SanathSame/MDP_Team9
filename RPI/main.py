import threading
import time
import os
from pcComm import *
from android import *
from stm32 import *
from picamera import PiCamera
from ultrasonic import *

class RPI(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

        # Creating subsystem objects
        self.pcObject = PcComm()
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
        '''self.sendToSTM("0   F 10  ")            #Hardcoded msg to be sent to STM
        time.sleep(1)
        self.sendToSTM("1   LF 90 ")
        time.sleep(1)
        self.sendToSTM("2   F 10  ")
        time.sleep(1)
        self.sendToSTM("3   C     ")'''
        receiveFromSTMThread.start()

        '''self.snapPic()
        print("Picture taken....")
        self.sendToImg()
        print("Sent img to server")'''

    def receiveFromImg(self):
        counter = 0
        while True:
            imgMsg = self.pcObject.receiveMsgFromImg()
            if imgMsg is not None:
                print("Message received from Image: " + str(imgMsg))
                predictions = imgMsg.split()
                if not predictions:
                    continue
                #print(predictions[1])
                print(predictions)
                if len(predictions) == 3:
                    if int(predictions[1]) == 12:
                        counter += 1
                        print("Current counter in receive from image: " + str(counter))
                        self.sendToAlgo("CONTINUE")
                        predictions = []
                    else:
                        self.sendToAlgo("STOP")
                elif len(predictions) == 1:
                    if predictions[0] == "NOIMAGE":
                        pass

                '''if imgMsg[:3] == "AND":
                    self.sendToAndroid(imgMsg[4:])'''



    def receiveFromAlgo(self):
        while True:
            algoMsg = self.pcObject.receiveMsgFromAlgo()

            #print("Message received from Algo: " + str(algoMsg[2:]))        ## Start from index 2 to remove unknown symbol
            if algoMsg is not None:
                commands = algoMsg.split("\n")
                for c in commands:
                    if c[2:5] == "IMG":
                        print("Message sending to img server: " + str(c[6:] + "|||"))
                        #self.snapPic()
                        self.sendToImg()            #Send the obstacle: XX YY
                    if c[2:5] == "STM":
                        print("Message sending to STM: " + str(c[6:] + "|||"))
                        #time.sleep(0.05)
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
            msgToSTM = input("Enter your msg: ")
            self.stm.sendMsg(msgToSTM)'''

    def receiveFromSTM(self):
        # Enclose in while loop
        while True:
            stmMsg = self.stm.receiveMsg()
            if stmMsg is not None and ord(stmMsg[0]) != 0 :
                #print("Message from STM: " + stmMsg)       #Comment for debug

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
                if stmMsg[:4] == "DONE":
                    self.sendToAlgo(stmMsg)


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
        self.pcObject.disconnect()
        self.androidObject.disconnect()
        '''with open("test1.csv", 'w') as f:
            f.write(str(rpi.y1[20:-10])[1:-1] + '\n')
            f.write(str(rpi.y2[20:-10])[1:-1])
            f.close()
        print("in closing....")'''
        self.stm.disconnect()
        self.camera.close()


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
