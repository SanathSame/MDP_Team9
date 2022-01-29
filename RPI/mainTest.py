from pcComm import *
import socket
import os
import tqdm

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


if __name__ == "__main__":
    TestTCP()
