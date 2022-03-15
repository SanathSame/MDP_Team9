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
        self.target_average_location = 4.5

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
        MIN_OBSTACLE_DISTANCE = 50
        MAX_OBSTACLE_DISTANCE = 200
        MIN_STRAIGHT_DISTANCE_MOVE = 5

        # Get out of spawn
        self.stm.send_message("F 30")

        TARGET_DISTANCE = 30
        while True:
            distance_from_wall = self.stm.get_ultra_reading()

            if distance_from_wall < TARGET_DISTANCE:
                break

            distance_to_move = distance_from_wall - TARGET_DISTANCE

            if distance_to_move < MIN_STRAIGHT_DISTANCE_MOVE:
                break

            self.stm.send_message(f"F {distance_to_move}")

        if distance_from_wall < TARGET_DISTANCE // 2:
            self.stm.send_message(f"B {TARGET_DISTANCE - distance_from_wall}")

        # First turn left
        self.stm.send_message("LF 80")
        left_distance_moved = int(self.stm.send_message("F IR"))
        print("Left distance moved", left_distance_moved)
        print(self.stm.get_ultra_reading())
        # self.stm.send_message("RF 180")
        self.stm.send_message("RF 90")
        time.sleep(1)
        self.stm.send_message("RF 90")

        # Go along back of obstacle
        distance_to_move_before_starting = 60
        self.stm.send_message(f"F {distance_to_move_before_starting}")
        right_distance_moved = int(self.stm.send_message("F IR"))
        print("Right distnace moved", right_distance_moved)
        # self.stm.send_message("RF 180")
        self.stm.send_message("RF 90")
        time.sleep(1)
        self.stm.send_message("RF 90")

        remaining_to_move_back = right_distance_moved + distance_to_move_before_starting - left_distance_moved - 2 * TURN_RADIUS
        print("remaining", remaining_to_move_back)

        if remaining_to_move_back > 0:
            self.stm.send_message(f"F {remaining_to_move_back}")
        elif remaining_to_move_back < 0:
            self.stm.send_message(f"B {remaining_to_move_back * -1}")

        self.stm.send_message("LF 80")
        self.centralise_bot_to_bullseye(10)
        print("Done")

    def limit_value_between(self, value, min_value, max_value):
        """
        Returns value if min_value <= value <= max_value
        Returns min_value if value < min_value
        Returns max_value if value > max_value
        """

        if value < min_value:
            return min_value
        if value > max_value:
            return max_value
        return value

    def get_average_location(self):
        predictions = self.request_prediction()
        print("Getting average location predictions", predictions)
        # predictions = [p for p in predictions if "bullseye" in p]

        if len(predictions) == 0:
            return -1
        # Get only smallest guy
        locations = [int(p.split()[-1]) for p in predictions]
        
        # Return weighted avg
        if len(locations) == 1:
            return locations[0]
        elif len(locations) == 2:
            return locations[0] * 0.8 + locations[1] * 0.2
        elif len(locations) == 3:
            return locations[0] * 0.75 + locations[1] * 0.25 + locations[2] * 0.25
        else:
            return sum(locations) / len(locations)

    def centralise_bot_to_bullseye(self, target_distance_from_wall, max_distance = 99999):
        print("Centralise bot to bullseye")

        times_to_adjust = 1
        distance_from_wall = self.stm.get_ultra_reading()
        degrees_to_turn = 45
        target_average_location = self.target_average_location
        average_location_threshold = 0.5
        target_distance_from_back_of_start_wall = target_distance_from_wall
        decay = 0.05

        while distance_from_wall > target_distance_from_back_of_start_wall and times_to_adjust < 4:
            distance_from_wall = self.stm.get_ultra_reading()
            if distance_from_wall < target_distance_from_wall:
                break
            average_location = self.get_average_location()

            print("average location", average_location)

            if average_location < 0:
                self.stm.send_message("F {}".format(distance_from_wall // 2))
                distance_from_wall = self.stm.get_ultra_reading()
                continue

            print("Got distance from wall and average location", distance_from_wall, average_location)

            if distance_from_wall < target_distance_from_back_of_start_wall:
                print("We already close enough to go in")
                break

            to_turn = int(degrees_to_turn * abs(average_location - target_average_location) / target_average_location * ((1 - decay) ** (times_to_adjust - 1)))
            print("to turn", to_turn)
            print("average_location", average_location)
            print("distance_from_wall", distance_from_wall)

            if to_turn < 5 or times_to_adjust > 4:
                break

            if average_location < target_average_location - average_location_threshold:
                self.stm.send_message("{} {}".format("LF", to_turn))
            elif average_location > target_average_location + average_location_threshold:
                self.stm.send_message("{} {}".format("RF", to_turn))

            distance_from_wall = self.stm.get_ultra_reading()
            to_move_forward = int((distance_from_wall - target_distance_from_back_of_start_wall) * 0.5)
            to_move_forward = max_distance if distance_from_wall > max_distance else distance_from_wall
            self.stm.send_message("F {}".format(to_move_forward // 2))

            if average_location < target_average_location - average_location_threshold:
                self.stm.send_message("{} {}".format("RF", to_turn // 2))
            elif average_location > target_average_location + average_location_threshold:
                self.stm.send_message("{} {}".format("LF", to_turn // 2))

            distance_from_wall = self.stm.get_ultra_reading()
            to_move_forward = int((distance_from_wall - target_distance_from_back_of_start_wall) * 0.5)
            self.stm.send_message("F {}".format(to_move_forward // 2))

            times_to_adjust += 1
            if distance_from_wall < target_distance_from_back_of_start_wall:
                print("We already close enough to go in")
                break
        
        distance_from_wall = self.stm.get_ultra_reading()
        minimum_distance_to_move = 5
        target_distance_from_back_of_start_wall = target_distance_from_wall

        while distance_from_wall > target_distance_from_back_of_start_wall:
            to_move = distance_from_wall - target_distance_from_back_of_start_wall
            to_move = minimum_distance_to_move if to_move < minimum_distance_to_move else to_move
            self.stm.send_message("F {}".format(to_move))
            distance_from_wall = self.stm.get_ultra_reading()

if __name__ == "__main__":
    try:
        rpi = RPI()

        input()
    except Exception as e:
        print(e)
        rpi.close_all()