from numpy import safe_eval
import requests as r
from torch import unsafe_chunk

def detect(image, server_address):
    """
    Sends image to server_address for a prediction

    image: Numpy array representation of the image
    server_address: The location to send the data to
    """

    response = r.post(server_address, files={"image": image})
    return response.text

if __name__ == "__main__":
    image = open('generate_images/foreground_images/2015.jpg', 'rb')
    response = detect(image, 'http://c9be-2401-7400-6006-8ed7-9440-53e7-4f1f-9029.ngrok.io//predict')
    print(response)