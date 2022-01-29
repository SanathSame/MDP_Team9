import bluetooth

class android():
    def __int__(self):

        self.rfCommChannel = 4
        self.clientSock = None
        self.serverSock = None
        self.uuid = 0
        self.clientSockAddr = "192.168.9.9"

    def connect(self):
        self.serverSock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        print("Bluetooth socket created!")

        self.socket.bind(("", self.rfCommChannel))
        print("Bluetooth binding completed!")

        self.serverSock.listen(1)

        bluetooth.advertise_service(self.serverSock, "MDPGrp9", service_id = self.uuid,
                                    service_classes = [self.uuid, bluetooth.SERIAL_PORT_CLASS],
                                    profiles = [bluetooth.SERIAL_PORT_PROFILE])
        print("Waiting for connection at channel " + str(self.rfcommchannel))



