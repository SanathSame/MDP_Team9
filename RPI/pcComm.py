import socket
import time
import os
from constants import *

class PcComm():
    def __init__(self):
        self.IP_ADDDRESS = "192.168.9.9"
        self.serverSocket = None # Socket connection with RPi
        self.nPort = 1234
        self.client = None
        self.client_ip = None

        self.isConnected = False
        self.BUFFER_SIZE = 1024

        self.connect()

    def connect(self):
        try:
            if self.isConnected:
                return

            self.serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            # Ensures address can immediately be reused even after multiple iterations
            self.serverSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.serverSocket.bind((self.IP_ADDDRESS, 1234))
            self.serverSocket.listen(2)

            print("RPI waiting for client to connect")
            self.client, self.client_ip = self.serverSocket.accept()
            print("RPi is connected to " + str(self.client))

            self.isConnected = True

        except Exception as e:
            print("Connection Error: " + str(e))
            self.connect()

    def disconnect(self):
        try:
            if self.serverSocket:
                self.serverSocket.close()
                print("RPi socket closed...")

            if self.client:
                self.client.close()
                print("Image Rec client socket closed...")

            self.isConnected = False
        except Exception as e:
            print("Error in disconnecting from PC: " + str(e))

    def receive_command(self):
        """
        Receive a command to execute
        """
        try:
            msg = self.client.recv(self.BUFFER_SIZE).decode("utf-8")
            print("Receive command", msg)
            if msg == "READY":
                return "ok"

            return msg
        except Exception as e:
            print("Error in receiving: " + str(e))
            self.connect()
            self.receive_command()

    def reply(self, msg):
        """
        Send a message to the client
        """
        self.client.send(bytes(msg, "utf-8"))
        time.sleep(0.1)

    def send_image(self):
        """
        Send image to client
        """
        try:
            if not self.isConnected:
                print("Client and RPi not connected")
                self.connect()
                return False

            self.reply("RPI RECEIVE_IMAGE") # Signal to client that he is going to receive image
            FILE_TO_READ = "a.jpeg"
            filesize = os.path.getsize(FILE_TO_READ)
            self.client.send(bytes(str(filesize), "utf-8"))
            time.sleep(0.5)

            with open(FILE_TO_READ, "rb") as img_file:
                img_bytes = img_file.read()
                self.client.send(img_bytes)
            print("Successfully sent image to server...")

        except Exception as e:
            print("Error in sending: " + str(e))
            self.connect()

    def receiveMsgFromImg(self):
        """
        Receive predictions from client
        """
        try:
            msgReceived = self.client.recv(self.BUFFER_SIZE).decode("utf-8")
            print("In receiveMsgFromImg() function, Message received is: " + str(msgReceived))
            return msgReceived

        except Exception as e:
            print("Error in receiving: " + str(e))
            self.connect()
            self.receiveMsgFromImg()