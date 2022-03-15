import socket
from time import sleep
from predictImage import predict
import threading

IP_ADDRESS = "192.168.9.9"
PORT = 1234

class Client(threading.Thread):
    is_connected = False
    def __init__(self):
        threading.Thread.__init__(self)
        self.s = socket.socket()
        self.s.connect((IP_ADDRESS, PORT))
        self.is_connected = True

        while True: # Only start when everything is ready
            status = self.send_message_to_rpi("PC READY")
            if len(status) > 0:
                print(status)
                break
        
        receiveFromRpiThread = threading.Thread(target=self.receive_message_from_rpi_thread, args=(), name="receive_image_thread", daemon=True)
        receiveFromRpiThread.start()

    def send_message_to_rpi(self, msg, wait_for_reply = True):
        if not self.is_connected:
            print("Not connected")
            return

        self.s.send(bytes(msg, "utf-8"))
        sleep(0.1)
        print("Sent msg:", msg)
        while wait_for_reply:
            msg = self.receive_message_from_rpi()
            if len(msg) > 0:
                return msg

        return ""

    def send_message_to_stm(self, msg):
        """
        Send a command to the STM to execute
        Returns the reply message from the STM
        If there is no additional information to provide, returns `Done <CommandNumber>`
        If there is information to provide, returns the information
        For example, `U` returns the reading on the ultra sensor
        """
        return self.send_message_to_rpi("STM {}".format(msg))

    def move(self, direction: str, distance: int):
        """
        Make robot move either forwards or backwards straight for distance cm
        `direction`: Direction of movement, one of F, B
        `distance`: Distance in centimeters to move
        """
        if direction not in ["F" , "B"]:
            print("Direction must be F or B")
            return
        
        self.send_message_to_rpi("STM {} {}".format(direction, distance))

    def turn(self, direction: str, angle: int):
        """
        Turn the robot in `direction` for `angle`
        """
        if direction not in ["RF", "RB", "LF", "LB"]:
            print("Invalid direction, must be one of", ["RF", "RB", "LF", "LB"])
            return

        self.send_message_to_rpi("STM {} {}".format(direction, angle))

    def get_ultra_reading(self):
        """
        Get the readings from the ultrasonic sensor
        """
        return self.send_message_to_stm("U")

    def disconnect(self):
        self.send_message_to_rpi("disconnect")
        self.s.close()

    def receive_message_from_rpi(self):
        try:
            msg = self.s.recv(1024).decode('utf-8')

            if len(msg) == 0:
                return
            
            print("Received msg from RPI:", msg)
            if msg == "RPI RECEIVE_IMAGE":
                self.receive_image_for_prediction()

            return msg
        except Exception as e:
            print("Receiving message went wrong:", e)
            return ""

    def receive_message_from_rpi_thread(self):
        while True:
            self.receive_message_from_rpi()


    def receive_image_for_prediction(self):
        # Receive image from picture
        data = self.s.recv(1024)
        filesize = int(data.decode('utf-8'))
        print("Filesize", filesize)

        # Get image from RPI and save locally
        SAVE_FILE = "RPI/images/to_predict.jpeg"
        total_data_received = len(data)
        with open(SAVE_FILE, "wb") as f:
            while True:
                data = self.s.recv(1024)
                if not data:
                    break

                f.write(data)
                total_data_received += len(data)
                
                if total_data_received >= filesize:
                    break
            print("File done")

        predictions = predict(SAVE_FILE, save=False, return_smallest=True)
        sleep(0.05)
        if len(predictions) == 0:
            return self.send_message_to_rpi("PREDICTION NOIMAGE", wait_for_reply=False)
        
        self.send_message_to_rpi("PREDICTION NUMBER_OF_PREDICTIONS {}".format(len(predictions)), wait_for_reply=False)
        for prediction in predictions:
            self.send_message_to_rpi(prediction, wait_for_reply=False)

if __name__ == "__main__":
    try:
        c = Client()
        c.send_message_to_rpi("WEEK9", wait_for_reply=False)
        while True:
            pass
        #     c.send_message_to_rpi(input("Enter message to send: "))
    except KeyboardInterrupt:
        c.disconnect()
