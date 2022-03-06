import threading
import time
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
        self.androidObject = Android()
        self.stm = STM()

        # Establish connection with other subsystems
        self.pcObject.connect()
        self.androidObject.connect()
        self.stm.connect()
        print("Connection to devices completed...")

        time.sleep(1)

        self.camera = PiCamera()
        self.camera.resolution = (640, 480)
        self.commandCounter = 0         # Tracks the current command_ID that has been COMPLETED by the STM

        ## hardcoded distances
        self.dist_to_wall = 80
        self.wall_left = 0
        self.wall_right = 60



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

        self.hardcode_parking()

    def hardcode_parking(self):
        dist_moved_to_the_left = 60-self.wall_left+10
        self.stm.sendMsg("{:<3} F {:<4}".format(self.commandCounter, self.dist_to_wall-30))
        self.increment_Command_and_sleep()
        self.stm.sendMsg("{:<3} LF {:<3}".format(self.commandCounter, 90))
        self.increment_Command_and_sleep()
        self.stm.sendMsg("{:<3} F {:<4}".format(self.commandCounter, dist_moved_to_the_left)) ## Exceed the left side of the wall
        self.increment_Command_and_sleep()

        self.stm.sendMsg("{:<3} RF {:<3}".format(self.commandCounter, 90))
        self.increment_Command_and_sleep()
        self.stm.sendMsg("{:<3} F {:<4}".format(self.commandCounter, 10))
        self.increment_Command_and_sleep()
        self.stm.sendMsg("{:<3} RF {:<3}".format(self.commandCounter, 90))
        self.increment_Command_and_sleep()
        self.stm.sendMsg("{:<3} F {:<4}".format(self.commandCounter, 65)) ## Travelling behind the wall
        self.increment_Command_and_sleep()
        self.stm.sendMsg("{:<3} RF {:<3}".format(self.commandCounter, 180))
        self.increment_Command_and_sleep()
        self.stm.sendMsg("{:<3} F {:<4}".format(self.commandCounter, self.wall_right-self.wall_left -dist_moved_to_the_left)) ## Car facing west on the right edge of wall
        self.increment_Command_and_sleep()

        self.stm.sendMsg("{:<3} LF {:<3}".format(self.commandCounter, 90)) ## Turn left to the carpark
        self.increment_Command_and_sleep()
        self.stm.sendMsg("{:<3} F {:<4}".format(self.commandCounter, self.dist_to_wall))
        self.increment_Command_and_sleep()


    def increment_Command_and_sleep(self):
        self.commandCounter += 1
        time.sleep(0.1)


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
                if androidMsg == "STARTPARKING":
                    self.sendToSTM("{:<3} U {:<4}".format(self.commandCounter, " "))        ## Get initial ultrasonic reading

    def sendToImg(self, msgToImg="DEFAULT_MESSAGE"):
        if msgToImg:
            self.snapPic()
            self.pcObject.sendMsgToImg(msgToImg)
            print("Message is sent to IMG server: " + str(msgToImg))

    def receiveFromImg(self):
        while True:
            pass


    def snapPic(self):
        try:
            self.camera.start_preview()
            self.camera.capture('a.jpeg')
            print("Captured image for sending")
            self.camera.stop_preview()

        except Exception as e:
            print("Error in taking picture..." + str(e))

    def closeAll(self):
        self.camera.close()
        self.pcObject.disconnect()
        self.androidObject.disconnect()
        self.stm.disconnect()

if __name__ == "__main__":
    rpi = RPI()
    try:
        rpi.startThread()
        while True:
            pass

    except KeyboardInterrupt:
        rpi.closeAll()