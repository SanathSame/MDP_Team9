import socket
import time
import tqdm

class PcComm():
    def __init__(self):
        self.IP_ADDDRESS = "192.168.9.9"
        self.serverSocket = None
        self.nPort = 1234
        self.client = None
        self.clientIP = None
        self.isConnected = False
        self.msgQueue = []
        self.BUFFER_SIZE = 1024

    # Enqueue the commands
    def enqueue(self, msg):
        self.msgQueue.append(msg)

    # Dequeue the commands stored inside the queue
    def dequeue(self):
        return self.msgQueue.pop(0)

    def connect(self):
        try:
            if not self.isConnected:
                self.serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                # Ensures address can immediately be reused even after multiple iterations
                self.serverSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                self.serverSocket.bind((self.IP_ADDDRESS, 1234))
                self.serverSocket.listen(2)

                print("RPi is listening for PC communication...")
                self.client, self.clientIP = self.serverSocket.accept()
                print("RPi is connected to " + str(self.client))

                self.isConnected = True

        except Exception as e:
            print("Connection Error: " + str(e))

    def disconnect(self):
        try:
            if self.serverSocket:
                self.serverSocket.close()
                print("RPi socket closed...")

            if self.client:
                self.client.close()
                print("Client closed...")
                self.isConnected = False

        except Exception as e:
            print("Error in disconnecting from PC: " + str(e))

    def sendMsg(self, msg=""):
        try:
            if msg != "":
                if self.isConnected:
                    #msg = msg + '\n'
                    self.client.sendall(msg.encode())
                    print("In send(msg) function, Message has been sent to PC: " + str(msg))
                    return True

                else:
                    print("In send(msg) function, RPI is not connected properly...")
                    self.connect()
                    return False
        except Exception as e:
            print("Error in sending: " + str(e))
            self.connect()
            self.send(msg)

    def receiveMsg(self):
        try:
            msgReceived = self.client.recv(10).decode("utf-8")
            print("In send(msg) function, Message received is: " + str(msgReceived))
            commands = msgReceived.split(' ')
            for i in range(len(commands)):
                if commands != '':
                    self.enqueue(commands[i])
            return self.msgQueue

        except Exception as e:
            print("Error in receiving: " + str(e))
            self.connect()
            self.receive()

    def sendImage(self, fileName, fileSize):
        # start sending the file
        progress = tqdm.tqdm(range(fileSize), f"Sending {fileName}", unit="B", unit_scale=True, unit_divisor=1024)
        with open(fileName, "rb") as f:
            while True:
                # read the bytes from the file
                bytes_read = f.read(self.BUFFER_SIZE)
                if not bytes_read:
                    # file transmitting is done
                    break
                self.serverSocket.sendall(bytes_read)
                # update the progress bar
                progress.update(len(bytes_read))


