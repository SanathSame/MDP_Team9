import cv2
import socket
import torch
import os

def predict():
    # model = torch.hub.load('ultralytics/yolov5', 'custom', path='Image Recognition/weights/best.pt')
    s = socket.socket()        
    host = '192.168.9.9'# ip of raspberry pi 
    port = 9999          
    s.connect((host, port))

    # signal to rpi that we want an image

    with open("RPI/images/to_predict.jpeg", "wb") as f:
        while True:
            data = s.recv(1024)
            if not data:
                break

            f.write(data)
        print("File done")

    print("Received")
    s.close()

    os.system('python3 "Image Recognition/detect.py" --weights "Image Recognition/weights/best.pt" --source "RPI/images/to_predict.jpeg" --name "../../../RPI/predictions/run"')
    # img = cv2.imread("to_predict.jpeg")
    # img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    # results = model(img)
    # predicted_classnames = results.pandas().xyxy[0][['name']].values.flatten()

    # for box in results.xyxy[0]: 
    #     if box[5]==0:
    #         xB = int(box[2])
    #         xA = int(box[0])
    #         yB = int(box[3])
    #         yA = int(box[1])
    #         img = cv2.rectangle(img, (xA, yA), (xB, yB), (0, 255, 0), 2)

    # if len(predicted_classnames) == 0:
    #     print("No objects detected")
    # else:
    #     print(", ".join(predicted_classnames))

    # cv2.imwrite("predicted.jpeg", img)

if __name__ == "__main__":
    predict()