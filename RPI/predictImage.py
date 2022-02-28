# Send string 
# IMG <ID> <CLASS>

import torch
import cv2
from math import ceil
import os

WEIGHTS_PATH = 'Image Recognition/weights/best.pt' # Path to weights being used
MODEL_PATH = 'Image Recognition' # Path to yolov5 repo locally
PREDICTIONS_DIR = 'RPI/predictions' # Path to save predictions
model = torch.hub.load(MODEL_PATH, 'custom', path=WEIGHTS_PATH, source='local')
NO_PREDICTION_PREFIX = "NO_PREDICTION"
STITCHED_IMAGE_PREFIX = "stitched"

def predict(img_path: str):
    """
    Given a path to an image file, predict what classes are in the image
    Returns a list of strings with the format "IMG <ID> <CLASS>", where 
    - ID is the ID of the class
    - CLASS is the name of the class
    """
    model.conf = 0.6

    img = cv2.imread(img_path)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = cv2.GaussianBlur(img, (5, 5), 1)
    results = model(img)
    results.render()

    # Extract ids, classnames and locations of the image
    # Note that results are sorted from highest confidence to lowest confidence
    df = results.pandas().xyxyn[0]

    result = []
    if not df.empty:
        df['centerx'] = df[['xmin','xmax']].mean(axis=1)
        df['location'] = df.apply(lambda x: get_location_of_center(x['centerx']), axis=1)
        df['class'] = df.apply(lambda x: get_id_of_image(x['name']), axis=1)

        predicted_ids = df[['class']].values.flatten()
        predicted_classnames = df[['name']].values.flatten()
        predicted_locations = df[['location']].values.flatten()

        result = ["IMG {} {} {}".format(predicted_id, predicted_classname, predicted_location) for predicted_id, predicted_classname, predicted_location in list(zip(predicted_ids, predicted_classnames, predicted_locations))]
    
    # Save image to PREDICTIONS_DIR
    counter = len(os.listdir(PREDICTIONS_DIR))
    for img in results.imgs:
        if len(result) == 0: 
            # If image has no predictions, we append a NO_PREDICTION_PREFIX to 
            # separate from images with predictions
            image_name = '{}/{}_{}.jpeg'.format(PREDICTIONS_DIR, NO_PREDICTION_PREFIX, counter)
        else:
            image_name = '{}/{}.jpeg'.format(PREDICTIONS_DIR, counter)
        
        cv2.imwrite(image_name, img[...,::-1]) # Img in rgb, but cv2 expects bgr
        counter += 1
    
    # Any image that does not contain NO_PREDICTION_PREFIX or STITCHED_IMAGE_PREFIX and is not hidden (starts with .)
    images_to_stitch = [cv2.imread(os.path.join(PREDICTIONS_DIR, img)) for img in os.listdir(PREDICTIONS_DIR) if 
        (not img.startswith(STITCHED_IMAGE_PREFIX) and 
        not img.startswith(".") and 
        not img.startswith(NO_PREDICTION_PREFIX)
    )]

    print(len(images_to_stitch))

    # Stitch images and save the result
    stitch_images_and_save(images_to_stitch)
    return result

def get_location_of_center(center_location):
    """
    Returns where the center is located in the image
    0 <= center_location <= 1
    FAR_LEFT, LEFT, CENTER, RIGHT, FAR_RIGHT
    """

    if center_location <= 0.2:
        return 0 # "FAR_LEFT"
    elif center_location <= 0.4:
        return 1 # "LEFT"
    elif center_location <= 0.6:
        return 2 # "CENTER"
    elif center_location <= 0.8:
        return 3 # "RIGHT"
    else:
        return 4 # "FAR_RIGHT"

def get_id_of_image(image_class_name):
    """
    Gets the id of the image, based on the classname from the image
    """
    image_class_id_mappings = {
        '1': 11, 
        '2': 12, 
        '3': 13, 
        '4': 14, 
        '5': 15, 
        '6': 16, 
        '7': 17, 
        '8': 18, 
        '9': 19, 
        'a': 20, 
        'b': 21, 
        'blue_arrow': 39, 
        'bullseye': 10, 
        'c': 22, 
        'circle': 40, 
        'd': 23, 
        'e': 24, 
        'f': 25, 
        'g': 26, 
        'green_arrow': 38, 
        'h': 27, 
        'red_arrow': 37, 
        's': 28, 
        't': 29, 
        'u': 30, 
        'v': 31, 
        'w': 32, 
        'white_arrow': 36, 
        'x': 33, 
        'y': 34, 
        'z': 35
    }

    return image_class_id_mappings[image_class_name]

def stitch_images_and_save(images, images_per_row = 4):
    """
    Stitch all images that have a prediction into a single image
    `images`: List of cv2 images to stitch together
    `images_per_row`: Number of images per row (maximum)
    """
    
    # Separate images into individual rows
    rows = []
    for i in range(ceil(len(images) / images_per_row)):
        initial_index = images_per_row * i
        final_index = images_per_row * (i + 1)

        if final_index > len(images):
            final_index = len(images)

        rows.append(images[initial_index:final_index])
    
    resultant_stitch = concat_tile_resize(rows) # Generate resultant stitched image

    # Get the number of stitches, and increment by 1 to save
    stitch_count = len([img for img in os.listdir(PREDICTIONS_DIR) if (img.startswith(STITCHED_IMAGE_PREFIX) and not img.startswith("."))])
    cv2.imwrite(os.path.join(PREDICTIONS_DIR, "{}{}.jpeg".format(STITCHED_IMAGE_PREFIX, stitch_count)), resultant_stitch)

def vconcat_resize_min(im_list, interpolation=cv2.INTER_CUBIC):
    """
    Concatenate images together vertically, and resizes if necessary to the smallest width
    `im_list`: List of cv2 images to tile vertically
    """
    w_min = min(im.shape[1] for im in im_list)
    im_list_resize = [cv2.resize(im, (w_min, int(im.shape[0] * w_min / im.shape[1])), interpolation=interpolation)
                      for im in im_list]
    return cv2.vconcat(im_list_resize)

def hconcat_resize_min(im_list, interpolation=cv2.INTER_CUBIC):
    """
    Concatenates images together horizontally, and resizes if necessary to the smallest height
    `im_list`: List of cv2 images to tile horizontally
    """
    h_min = min(im.shape[0] for im in im_list)
    im_list_resize = [cv2.resize(im, (int(im.shape[1] * h_min / im.shape[0]), h_min), interpolation=interpolation)
                      for im in im_list]
    return cv2.hconcat(im_list_resize)

def concat_tile_resize(im_list_2d, interpolation=cv2.INTER_CUBIC):
    """
    Tiles images into a 2D grid
    `im_list_2d`: A 2D list of cv2 images to tile together
    """
    im_list_v = [hconcat_resize_min(im_list_h, interpolation=cv2.INTER_CUBIC) for im_list_h in im_list_2d]
    return vconcat_resize_min(im_list_v, interpolation=cv2.INTER_CUBIC)

if __name__ == "__main__":
    print(predict("Image Recognition/data/images/n.jpeg"))