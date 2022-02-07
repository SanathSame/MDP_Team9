from re import L
from receive_images_and_predict import predict

# TODO actually implement the movement and the constants
def rotate_left(angle):
    pass

def rotate_right(angle):
    pass

def move_backward(distance):
    pass

def move_forward(distance):
    pass

BOT_LENGTH = 25
DISTANCE_TO_OBSTACLE = 10
OBSTACLE_LENGTH = 10

def go_around_bullseye():
    """
    Precondition: Bot is facing the obstacle
    Bot will take a picture, and detect what the picture is
    If the picture is a bullseye, keep rotation around the obstacle until an image which is not the bullseye is found
    Returns the non-bullseye image
    """
    predictions = predict()
    while 'bullseye' in predictions:
        rotate_left(90)
        move_forward(OBSTACLE_LENGTH /2 + DISTANCE_TO_OBSTACLE + BOT_LENGTH / 2)
        rotate_right(90)
        move_forward(OBSTACLE_LENGTH /2 + DISTANCE_TO_OBSTACLE + BOT_LENGTH / 2)
        rotate_right(90)
        predictions = predict()

    return predictions