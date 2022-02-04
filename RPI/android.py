import bluetooth

class Android():
    def __init__(self):
        self.rfCommChannel = 3
        self.clientSock = None
        self.serverSock = None
        self.uuid = "00001101-0000-1000-8000-00805F9B34FB" #"00001101-0000-1000-8000-00805F9B34FB"
        self.clientSockAddr = None
        self.BUFFER_SIZE = 2048


    def connect(self):
        try:
            self.serverSock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
            print("Bluetooth socket created!")

            self.serverSock.bind(("", self.rfCommChannel)) #bluetooth.PORT_ANY))
            print("Bluetooth binding completed!")

            port = self.serverSock.getsockname()[1]

            self.serverSock.listen(1)

            bluetooth.advertise_service(self.serverSock, "MDPGrp9", service_id = self.uuid,
                              service_classes = [self.uuid, bluetooth.SERIAL_PORT_CLASS],
                              profiles = [bluetooth.SERIAL_PORT_PROFILE])
            print("Waiting for connection at RFCOMM channel " + str(self.rfCommChannel))

            self.clientSock, self.clientSockAddr = self.serverSock.accept()
            print("Client accepted: " + str(self.client))
        except Exception as e:
            print("Error in connecting bluetooth: " + str(e))
            self.serverSock.close()

    def disconnect(self):
        try:
            if self.clientSock:
                self.clientSock.close()
            if self.serverSock:
                self.serverSock.close()
            print("Bluetooth connection is closed...")
        except Exception as e:
            print("Error in closing bluetooth socket: " + str(e))

    def sendMsg(self, msg):
        try:
            # Send message
            self.clientSock.send(msg)
        except Exception as e:
            errorMsg = 'Error in sending msg via bluetooth: ' + str(e) + '\nRetrying Connection...'
            print(errorMsg)
            self.connect()

    def receiveMsg(self):
        try:
            message = self.clientSock.recv(self.BUFFER_SIZE).decode('utf-8')
            print("Message received: " + str(message))

            if len(message) > 0:
                return message
        except Exception as e:
            print("Error in receving msg: " + str(e))






