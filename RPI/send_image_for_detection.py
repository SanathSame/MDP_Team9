import socket
from picamera import PiCamera
import time

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

host = '192.168.9.9' #ip of raspberry pi
port = 9999
s.bind((host, port))

s.listen(5)
# c, addr = s.accept()

try:
    camera = PiCamera()
    print("Camera enabled")
    camera.resolution = (640, 640)
    camera.start_preview()
    time.sleep(2) # Camera warmup

    # for i in range(3):
    c, addr = s.accept()
    print('Got connection from',addr)
    camera.capture('a.jpeg')
    print("Captured")
    with open("a.jpeg", "rb") as img_file:
        img_bytes = img_file.read()
        c.send(img_bytes)
    c.close()
    
    camera.stop_preview()
    print("Done with capturing, closed camera")

except Exception as e:
    print("Exception has occurred", e)
finally:
    # c.close()
    s.close()