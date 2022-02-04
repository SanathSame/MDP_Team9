import torch
import cv2 
from flask import Flask, request
import numpy as np

# REQUIRES INTERNET
# load trained model
model = torch.hub.load('ultralytics/yolov5', 'custom', path='weights/best.pt')

app = Flask(__name__)

@app.route("/")
def hello():
    return """
    <p>Send a POST request to /predict, with the image as formdata under the key \"image\"</p>
    <p>Example command using cURL</p>
    <pre>curl -X POST http://127.0.0.1:5000/predict --form "image=@2008.jpg"</pre>
    """

# curl -X POST http://127.0.0.1:5000/predict --form "image=@2008.jpg"
@app.route("/predict", methods=["POST"])
def predict():
    print(request.files)
    if 'image' not in request.files:
        return "No file was uploaded", 400 # Bad request

    img = cv2.imdecode(np.frombuffer(request.files['image'].read(), np.uint8), cv2.IMREAD_UNCHANGED)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    results = model(img)
    predicted_classnames = results.pandas().xyxy[0][['name']].values.flatten()

    return ", ".join(predicted_classnames)

if __name__ == "__main__":
    app.run(debug=True)