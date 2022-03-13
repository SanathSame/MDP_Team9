import threading
import time
import os
import math

from numpy import number
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
            if command == "STM": # Client sends to RPI, RPI sends to STM. STM returns a message, we return msg back to client
                msg = self.stm.send_message(message)
                self.pcObject.reply(msg)
            elif command == "TAKEPICTURE":
                msg = self.request_prediction()
                print("After take picture, we received", msg)
            elif command == "WEEK9":
                self.execute_week9()
            elif command == "disconnect": # If client wants to disconnect
                self.close_all()
            else:
                self.pcObject.reply("ok")

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

    def request_prediction(self):
        print("Request prediction")
        self.capture_image()
        self.pcObject.send_image()

        predictions = []
        msg = ""
        while len(msg) == 0:
            msg = self.pcObject.receive_command()

        if "NOIMAGE" in msg:
            return predictions

        number_of_predictions = int(msg.split()[-1])

        for i in range(number_of_predictions):
            prediction = self.pcObject.receive_command()
            predictions.append(prediction)

        return predictions

    def close_all(self):
        """
        Close all connections to other objects
        """
        self.camera.close()
        self.pcObject.disconnect()
        self.stm.disconnect()
        self.android.disconnect

    def execute_week9(self):
        print("Starting week 9 task")
        distance_from_wall = int(self.stm.send_message("U"))

        while distance_from_wall > 30:
            print(distance_from_wall)
            predictions = self.request_prediction()
            predictions = [p for p in predictions if "bullseye" in p]
            locations = [int(p.split()[-1]) for p in predictions]

            average_location = sum(locations) / len(locations)

            degrees_to_turn = 30
            if average_location < 4:
                # Turn left
                self.stm.send_message("LF {}".format(degrees_to_turn))
            elif average_location > 5:
                # Turn right
                self.stm.send_message("RF {}".format(degrees_to_turn))
            else:
                self.stm.send_message("F 20")
            distance_from_wall = int(self.stm.send_message("U"))

        print("Done")


if __name__ == "__main__":
    try:
        rpi = RPI()

        input()
    except Exception as e:
        print(e)
        rpi.close_all()