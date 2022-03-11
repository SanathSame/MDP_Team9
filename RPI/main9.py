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

        self.pcObject = PcComm()
        self.stm = STM()
        self.android = Android()

        self.stm.connect()
        print("STM connected")
        self.pcObject.connect()
        print("PC connected")
        # self.androidObject.connect()
        # print("Android connected")
        print("Connection to devices completed...")

        time.sleep(2)
        self.camera = PiCamera()
        self.camera.resolution = (640, 480)
        self.commandCount = 0

        self.startThreads()

    def startThreads(self):
        # Threads of objects we want to be continuously listening to
        receiveFromPcThread = threading.Thread(target=self.process_pc_commands, args=(), name="read_Img_Thread", daemon=True)
        # receiveFromAndroidThread = threading.Thread(target=self.process_android_commands, args=(), name="read_STM_Thread", daemon=True)

        # Makes Threads start in the background
        receiveFromPcThread.start()
        # receiveFromAndroidThread.start()
        print("Started threads")

    def process_pc_commands(self):
        """
        Continuously listen for messages from PC, and process those messages
        READY - Client checking if main is ready
        STM <STMCOMMAND> - Command to send to the STM
        """
        while True:
            msg = self.pcObject.receive_command()

            if len(msg) == 0: # No message received
                return

            print("Message received from PC:", msg)

            command, message = msg.split()[0], " ".join(msg.split()[1:])
            if command == "STM":
                msg = self.stm.execute(message)
                self.pcObject.reply(msg)
            elif command == "PC": # If client checks that pc is ready, we reply ok
                msg = self.pcObject.receive_command(message)
                self.pcObject.reply(msg)
            elif command == "TAKEPICTURE":
                msg = self.request_prediction()
                print("After take picture, we received", msg)

            if msg == "disconnect": # If client wants to disconnect
                self.close_all()

    def process_stm_commands(self):
        """
        Continuously receive messages from STM, and process messages
        """
        while True:
            stmMsg = self.stm.receive_message()

            if len(stmMsg) == 0:
                return

            print("Message received from STM:", stmMsg)

    def process_android_commands(self):
        """
        Continuously listen to android, and process commands from android
        """
        while True:
            androidMsg = self.androidObject.receiveMsg()

            if len(androidMsg) == 0: # No message received
                return

            print("Message received from android:", androidMsg)

            if androidMsg == "START":
                self.execute_week9()

    def capture_image(self, location = "a.jpeg"):
        """
        Take an image from the RPi camera, and saves it
        """
        try:
            self.camera.capture(location)
            return location
        except Exception as e:
            print("Error in taking picture..." + str(e))

    def request_prediction(self, image_path = "a.jpeg"):
        self.capture_image()
        self.send_image()

        while True:
            msg = self.pcObject.receive_command()
            if len(msg) > 0:
                return msg

    def close_all(self):
        """
        Close all connections to other objects
        """
        self.camera.close()
        self.pcObject.disconnect()
        self.stm.disconnect()
        self.android.disconnect

    def predict_image(self):
        pass

    def execute_week9(self):
        print("Starting week 9 task")


if __name__ == "__main__":
    try:
        rpi = RPI()

        input()
    except Exception as e:
        print(e)
        rpi.close_all()