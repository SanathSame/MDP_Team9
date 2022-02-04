import cv2
import socket
import torch

def predict():
    model = torch.hub.load('ultralytics/yolov5', 'custom', path='weights/best.pt')
    s = socket.socket()        
    host = '192.168.9.9'# ip of raspberry pi 
    port = 9999          
    s.connect((host, port))

    with open("to_predict.jpeg", "wb") as f:
        while True:
            data = s.recv(1024)
            if not data:
                break

            f.write(data)
        print("File done")

    print("Received")
    s.close()

    img = cv2.imread("to_predict.jpeg")
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    results = model(img)
    predicted_classnames = results.pandas().xyxy[0][['name']].values.flatten()

    if len(predicted_classnames) == 0:
        print("No objects detected")
    else:
        print(", ".join(predicted_classnames))

if __name__ == "__main__":
    predict()