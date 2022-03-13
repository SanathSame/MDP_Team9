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
        #self.pcObject = PcComm()
        #self.androidObject = Android()
        self.stm = STM()

        # Establish connection with other subsystems
        #self.pcObject.connect()
        #self.androidObject.connect()
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
        #receiveFromImgThread.start()
        #receiveFromAlgoThread.start()
        receiveFromAndroidThread.start()
        receiveFromSTMThread.start()

        # self.hardcode_parking()


    def fastest_car(self):
        # constants
        MAX_CAMERA_DIST = 130  # max distance at which image rec can work
        CARPARK_WIDTH = 50 
        ULTRA_HEADER = "U"
        DIST_HEADER = "D"  # indicates distance moved while IR is active
        TURN_RADIUS = 20

        # read ultra value first to determine roughly distance to obstacle
        self.sendCommandToSTM("U")
        stm_message = self.receiveFromSTM()
        if not stm_message.startswith(ULTRA_HEADER):
            raise Exception("Expected to receive ultra value")
        ultra_dist = int(stm_message.strip()[2:])  # format: "U xxxx<9 spaces>"

        # determine distance to move until we are near wall
        dist_to_wall = ultra_dist - 40  # ensure we are 40cm away from wall TODO: test 
        self.sendCommandToSTM(f"F {dist_to_wall}")

        # turn left - move forward until IR gives large readings (store this as left_dist_moved)
        self.sendCommandToSTM("LF 90")
        self.sendCommandToSTM("F IR")
        stm_message = self.receiveFromSTM()
        if not stm_message.startswith(DIST_HEADER):
            raise Exception("Expected to receive left distance moved")
        left_dist_moved = int(stm_message.strip()[2:])  # format: "D xxxx<9 spaces>"

        # navigate around wall: turn right, move forward ~20cm (TODO: test), turn right again
        self.sendCommandToSTM("RF 90")
        self.sendCommandToSTM("F 20")
        self.sendCommandToSTM("RF 90")

        # move a bit forward to ensure car is beside wall, then use IR to move forward until car is past wall
        # store distance moved as right_dist_moved
        min_dist_along_wall = 50  # TODO: test
        self.sendCommandToSTM(f"F {min_dist_along_wall}") 
        self.sendCommandToSTM("F IR")
        stm_message = self.receiveFromSTM()
        if not stm_message.startswith(DIST_HEADER):
            raise Exception("Expected to receive left distance moved")
        right_dist_moved = int(stm_message.strip()[3:]) + min_dist_along_wall  # format: "D xxxx<9 spaces>"

        # navigate around wall: turn right, move forward ~20cm (TODO: test), turn right again
        self.sendCommandToSTM("RF 90")
        self.sendCommandToSTM("F 20")
        self.sendCommandToSTM("RF 90")

        # move forward by right_dist_moved - left_dist_moved - 2*TURN_RADIUS (1 for initial turn left, and 1 more for final turn right towards parking lot)
        self.sendCommandToSTM(f"F {right_dist_moved - left_dist_moved - 2*TURN_RADIUS}")
        
        # turn left towards carpark, move forward by initialY. take pic, ensure only 1 bullseye
        self.sendCommandToSTM("LF 90")
        self.sendCommandToSTM(f"F {dist_to_wall}")


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
                return str(stmMsg)


    def sendCommandToSTM(self, command):
        # ensure command is 6 chars exactly
        self.stm.sendMsg("{:<3} {:<6}".format(self.commandCounter, command))
        self.increment_Command_and_sleep()


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
                    # self.sendToSTM("{:<3} U {:<4}".format(self.commandCounter, " "))        ## Get initial ultrasonic reading
                    self.fastest_car()


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