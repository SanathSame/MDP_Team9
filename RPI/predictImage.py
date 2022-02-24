# Send string 
# IMG <ID> <CLASS>

import torch
import cv2
import os

WEIGHTS_PATH = 'Image Recognition/weights/best.pt' # Path to weights being used
MODEL_PATH = 'Image Recognition' # Path to yolov5 repo locally
PREDICTIONS_DIR = 'RPI/predictions' # Path to save predictions

def predict(img_path: str):
    """
    Given a path to an image file, predict what classes are in the image
    Returns a list of strings with the format "IMG <ID> <CLASS>", where 
    - ID is the ID of the class
    - CLASS is the name of the class
    """
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

    # Extract ids, classnames and locations of the image
    # Note that results are sorted from highest confidence to lowest confidence
    df = results.pandas().xyxyn[0]
    df['centerx'] = df[['xmin','xmax']].mean(axis=1)
    df['location'] = df.apply(lambda x: get_location_of_center(x['centerx']), axis=1)

    predicted_ids = df[['class']].values.flatten()
    predicted_classnames = df[['name']].values.flatten()
    predicted_locations = df[['location']].values.flatten()

    result = ["IMG {} {} {}".format(predicted_id, predicted_classname, predicted_location) for predicted_id, predicted_classname, predicted_location in list(zip(predicted_ids, predicted_classnames, predicted_locations))]
    return result

def get_location_of_center(center_location):
    """
    Returns where the center is located in the image
    0 <= center_location <= 1
    """

    if center_location <= 0.2:
        return "FAR_LEFT"
    elif center_location <= 0.4:
        return "LEFT"
    elif center_location <= 0.6:
        return "CENTER"
    elif center_location <= 0.8:
        return "RIGHT"
    else:
        return "FAR_RIGHT"

    
if __name__ == "__main__":
    print(predict("Image Recognition/data/images/n.jpeg"))