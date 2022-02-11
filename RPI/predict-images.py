# Send string 
# IMG <ID> <CLASS>

import torch
import cv2
import os

WEIGHTS_PATH = 'Image Recognition/weights/best.pt' # Path to weights being used
MODEL_PATH = 'Image Recognition' # Path to yolov5 repo locally
PREDICTIONS_DIR = 'RPI/predictions' # Path to save predictions

def predict(img_path):
    model = torch.hub.load(MODEL_PATH, 'custom', path=WEIGHTS_PATH, source='local')

    img = cv2.imread(img_path)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    results = model(img)
    results.render()

    # Save images with prediction labels
    counter = len(os.listdir(PREDICTIONS_DIR)) # To prevent name collision
    for img in results.imgs:
        cv2.imwrite('{}/{}.jpeg'.format(PREDICTIONS_DIR, counter), img[...,::-1]) # Img in rgb, but cv2 expects bgr
        counter += 1

    # Extract ids and classnames
    predicted_ids = results.pandas().xyxy[0][['class']].values.flatten()
    predicted_classnames = results.pandas().xyxy[0][['name']].values.flatten()

    id_classname = ["IMG {} {}".format(predicted_id, predicted_classname) for predicted_id, predicted_classname in list(zip(predicted_ids, predicted_classnames))]
    return id_classname

    
if __name__ == "__main__":
    print(predict("Image Recognition/data/images/n.jpeg"))