import threading
import time
import os
import math
from turtle import distance

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
        # constants
        TURN_RADIUS = 20

        self.stm.send_message(f"F 10")

        # read ultra value first to determine roughly distance to obstacle
        distance_from_wall = int(self.stm.send_message("U"))
        target_distance_from_wall = 35
        print("Distance from wall", distance_from_wall)

        # determine distance to move until we are near wall
        while distance_from_wall > target_distance_from_wall:
            # dist_to_wall = distance_from_wall - 40  # ensure we are 40cm away from wall TODO: test 
            # self.stm.send_message(f"F {dist_to_wall}")
            distance_to_move = int((distance_from_wall - target_distance_from_wall))
            self.stm.send_message("F {}".format(distance_to_move))
            distance_from_wall = int(self.stm.send_message("U"))

        # turn left - move forward until IR gives large readings (store this as left_dist_moved)
        self.stm.send_message("LF 90")
        left_distance_moved = int(self.stm.send_message("F IR"))

        # navigate around wall: turn right, move forward (TODO: test), turn right again
        self.stm.send_message("RF 210")
        self.stm.send_message(f"F 10")
        # self.stm.send_message("RF 90")

        # move a bit forward to ensure car is beside wall, then use IR to move forward until car is past wall
        # store distance moved as right_dist_moved
        # min_dist_along_wall = 50  # TODO: test
        # self.stm.send_message(f"F {min_dist_along_wall}") 
        # right_distance_moved = int(self.stm.send_message("F IR")) + min_dist_along_wall
        right_distance_moved = int(self.stm.send_message("F IR"))

        # navigate around wall: turn right, move forward (TODO: test), turn right again
        self.stm.send_message("RF 200")
        # self.stm.send_message(f"F {right_distance_moved}")
        # self.stm.send_message("RF 90")

        # move forward by right_dist_moved - left_dist_moved - 2*TURN_RADIUS (1 for initial turn left, and 1 more for final turn right towards parking lot)
        self.stm.send_message(f"F {right_distance_moved - left_distance_moved - 2*TURN_RADIUS}")
        self.stm.send_message(f"LF 90")

        ### second part ###
        times_to_adjust = 1
        previous_direction = "F"
        average_location = self.get_average_location(previous_direction)
        distance_from_wall = int(self.stm.send_message("U"))
        degrees_to_turn = 45
        target_average_location = 4.5
        average_location_threshold = 0.3
        target_distance_from_back_of_start_wall = 10
        decay = 0.05

        print("average_location", average_location)
        print("distance_from_wall", distance_from_wall)

        while distance_from_wall > 30 and times_to_adjust < 4:
            to_turn = int(degrees_to_turn * abs(average_location - target_average_location) / target_average_location * ((1 - decay) ** (times_to_adjust - 1)))
            print("to turn", to_turn)
            print("average_location", average_location)
            print("distance_from_wall", distance_from_wall)

            if to_turn < 5 and times_to_adjust > 3:
                break

            if average_location < target_average_location - average_location_threshold:
                self.stm.send_message("{} {}".format("LF", to_turn))
                previous_direction = "L"
            elif average_location > target_average_location + average_location_threshold:
                self.stm.send_message("{} {}".format("RF", to_turn))
                previous_direction = "R"

            self.stm.send_message("F {}".format((distance_from_wall - target_distance_from_back_of_start_wall) // 2))
            
            # if average_location < target_average_location - average_location_threshold:
            #     self.stm.send_message("{} {}".format("RF", to_turn))
            #     previous_direction = "L"
            # elif average_location > target_average_location + average_location_threshold:
            #     self.stm.send_message("{} {}".format("LF", to_turn))
            #     previous_direction = "R"

            
            times_to_adjust += 1
            distance_from_wall = int(self.stm.send_message("U"))
            average_location = self.get_average_location(previous_direction)
        
        distance_from_wall = int(self.stm.send_message("U"))
        minimum_distance_to_move = 5
        while distance_from_wall > target_distance_from_back_of_start_wall:
            to_move = distance_from_wall - target_distance_from_back_of_start_wall
            to_move = minimum_distance_to_move if to_move < minimum_distance_to_move else to_move
            self.stm.send_message("F {}".format(to_move))
            distance_from_wall = int(self.stm.send_message("U"))

        print("Done")

    def get_average_location(self, previous_direction):
        predictions = self.request_prediction()

        if len(predictions) == 0:
            return 4.5

        predictions = [p for p in predictions if "bullseye" in p]
        # Get only smallest guy
        location = int(predictions[0].split()[-1])
        return location
        # locations = [int(p.split()[-1]) for p in predictions]

        # if len(locations) == 0:
        #     if previous_direction == "L":
        #         average_location = 11
        #     elif previous_direction == "R":
        #         average_location = -1
        #     else:
        #         average_location = 4.5
        # else:
        #     average_location = sum(locations) / len(locations)

        # return average_location, len(predictions)

if __name__ == "__main__":
    try:
        rpi = RPI()

        input()
    except Exception as e:
        print(e)
        rpi.close_all()