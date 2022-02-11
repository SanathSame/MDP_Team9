from pcComm import *
import socket
import os
import tqdm
# from android import *
#from stm32 import *
from pcComm import *
# import bluetooth
import sys

BUFFER_SIZE = 1024
SEPARATOR = "@.@"

# Test TCP codes
def TestTCP():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(("192.168.9.9", 1234))

    # Sending msg
    msg = input("Enter message: ")
    #msg = msg + "\n"
    sock.send(bytes(msg, "utf-8"))

    # Receiving msg
    #pcMsg = sock.recv(1024)
    #print("\n" + str(pcMsg))


    # Receiving image
    received = sock.recv(BUFFER_SIZE).decode()
    filename, filesize = received.split(SEPARATOR)
    # remove absolute path if there is
    filename = os.path.basename(filename)
    # convert to integer
    filesize = int(filesize)

    print("ready to receive " + filename + str(filesize))
    progress = tqdm.tqdm(range(filesize), f"Receiving {filename}", unit="B", unit_scale=True, unit_divisor=1024)
    with open(filename, "wb") as f:
        while True:
            # read 1024 bytes from the socket (receive)
            bytes_read = sock.recv(BUFFER_SIZE)
            if not bytes_read:
                # file transmitting is done
                break
            # write to the file the bytes we just received
            f.write(bytes_read)
            # update the progress bar
            progress.update(len(bytes_read))

    sock.close()

def TestBluetooth():
    btObj = Android()
    btObj.connect()

    msg = input("Enter message: ")
    btObj.sendMsg(msg)

    msg1 = btObj.receiveMsg()
    print("Message 1: " + str(msg1))

    msg2 = btObj.receiveMsg()
    print("Message 2: " + str(msg2))

    msg3 = btObj.receiveMsg()
    print("Message 3: " + str(msg3))

    btObj.disconnect()

def Testtest():
    uuid = "00001101-0000-1000-8000-00805F9B34FB" #"94f39d29-7d6d-437d-973b-fba39e49d4ee"
    service_matches = bluetooth.find_service(uuid=uuid)

    if len(service_matches) == 0:
        print("couldn't find the SampleServer service =(")
        sys.exit(0)

    first_match = service_matches[0]
    port = first_match["port"]
    name = first_match["name"]
    host = first_match["host"]

    print("connecting to \"%s\" on %s" % (name, host))

    # Create the client socket
    sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    sock.connect((host, 3))

    print("connected.  type stuff")
    while True:
        data = input()
        if len(data) == 0: break
        sock.send(data)

    sock.close()

def TestSTM():
    stm = STM()
    stm.connect()
    print("attempt connect")
    try:
        print("attempting")

        stm.sendMsg("w50")
    except KeyboardInterrupt:
        print("Terminating the program now...")

def TestImagePrediction():
    # model = torch.hub.load('ultralytics/yolov5', 'custom', path='Image Recognition/weights/best.pt')
    s = socket.socket()        
    host = '192.168.9.9'# ip of raspberry pi 
    port = 1234          
    s.connect((host, port))
    print("Connected")

    # signal to rpi that we want an image

    with open("RPI/images/to_predict.jpeg", "wb") as f:
        while True:
            data = s.recv(1024)
            print(data)
            if not data:
                break

            f.write(data)
        print("File done")

    print("Received")
    s.close()

    os.system('python3 "Image Recognition/detect.py" --weights "Image Recognition/weights/best.pt" --source "RPI/images/to_predict.jpeg" --name "../../../RPI/predictions/run"')
  

if __name__ == "__main__":
    TestImagePrediction()
    #TestBluetooth()
    #Testtest()
    #TestSTM()
