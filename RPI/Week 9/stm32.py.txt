import serial
import time

class STM():
    def __init__(self):
        self.BAUD_RATE = 115200
        self.SERIAL_PORT = "/dev/ttyUSB0"
        self.service = None
        self.STM_MSG_LENGTH = 19
        self.commandCount = 0

    def connect(self):
        """
        Initialise and connect RPi and STM
        """
        try:
            self.service = serial.Serial(self.SERIAL_PORT, self.BAUD_RATE, parity=serial.PARITY_NONE,
                stopbits=serial.STOPBITS_ONE,bytesize=serial.EIGHTBITS,timeout=5)
            time.sleep(1)

            if not self.service:
                print("Connected to STM...")

        except Exception as e:
            print("Error in connecting to STM: " + str(e))
            time.sleep(0.5)
            self.connect()

    def send_message(self, command: str):
        """
        Sends a command to the STM to execute

        Returns the message STM sends back after executing the command

        Will only stop once `Done` is received from STM
        Message content will either be `Done <commandNumber>`, or some required sensor value, 
        based on command
        """
        if len(command) == 0:
            return

        try:
            formatted_command = "{:<3} {:<6}".format(self.commandCount, command)
            self.service.write(formatted_command.encode('utf-8'))
            print("Write command", formatted_command.strip())
            self.commandCount += 1

            messages_to_receive = []

            while True:
                message = self.read_message()

                if len(message) == 0:
                    continue
                    
                if message.startswith("I") or message.startswith("E") or message.startswith("R"):
                    print("EMERGENCY MESSAGE:", message)
                    continue

                messages_to_receive.append(message)

                if "Done {}".format(self.commandCount - 1) in message:
                    break
                # self.commandCount += 1
                
                        
            print("Command sent to STM:", formatted_command.strip())
            print("Received reply from STM:", [m.strip() for m in messages_to_receive])
            return messages_to_receive[0]
        except Exception as e:
            print('Error in sending message to STM: ' + str(e))
            self.connect()
            time.sleep(0.5)
            self.send_message(command)

    def read_message(self):
        """
        Receive message from STM
        """
        try:
            msg = self.service.read(self.STM_MSG_LENGTH)
            return msg.decode('utf-8')

        except Exception as e:
            print('Error receiving message from STM: ' + str(e))
            self.connect()
            time.sleep(0.5)
            self.receive_message()

    def get_ultra_reading(self, number_of_readings = 5):
        result = 0
        for i in range(number_of_readings):
            result += int(self.send_message("U"))

        return result // number_of_readings

    def disconnect(self):
        """
        Disconnect from STM
        """
        try:
            if self.service:
                self.service.close()
                print('Serial connection closed...')
        except Exception as e:
            print("Error in disconnecting from STM..." + str(e))