# Setup

Open a terminal, and then run the following commands

```
git clone https://github.com/ultralytics/yolov5  # clone
cd yolov5
python -m venv venv # setup virtual environment
venv\Scripts\activate.bat # activate virtual environment
pip install -r requirements.txt  # install
```

If you are using a GPU, install CUDA from [here](https://developer.nvidia.com/cuda-downloads?target_os=Windows&target_arch=x86_64&target_version=10&target_type=exe_network)
- windows, x86_64, 10, exe (network)

Install pytorch [here](https://pytorch.org/get-started/locally/). On my computer, these were the following options I chose
- Stable, windows, pip, python, cuda 11.3

Under the "Run this command" section of the table, run the generated command from the webpage inside the same terminal. An example of the command is

```
pip3 install torch==1.10.1+cu113 torchvision==0.11.2+cu113 torchaudio===0.10.1+cu113 -f https://download.pytorch.org/whl/cu113/torch_stable.html
```

# Training

1. Place classes to predict under `generate_images/foreground_images`. Each file should be labelled the class you want to predict
2. Place random background images in `generate_images/background_images`
3. Run the command `python generate_images/generate_dataset.py` to generate images for the dataset
4. Create a new file `custom_dataset.yaml` and fill in the appropriate data. For this project, check `custom_dataset.yaml` to view the configuration
5. Run the following command to begin training

    ```
    python train.py --img 640 --cfg yolov5s.yaml --hyp hyp.scratch.yaml --batch 16 --epochs 32 --data custom_dataset.yaml --weights yolov5s.pt --workers 2 --name custom_dataset
    ```

    This will train a new model which is saved under `runs/train/custom_datasetX`, where `X` is an increasing number. Check the latest number. Weights for the model will be saved under `runs/train/custom_datasetX/weights/*.pt`. There will be a `best.pt` and a `last.pt`

    If you are running into an "out of memory" error, then try to reduce the number of workers, or reduce the batch size

    If you are trying to finetune the model, run the following command instead:

    ```
    python train.py --img 640 --cfg yolov5s.yaml --hyp hyp.finetune.yaml --batch 16 --epochs 32 --data custom_dataset.yaml --weights weights/best.pt --workers 1 --name custom_dataset
    ```

    The above takes already preloaded weights from an existing file under `weights/best.pt`, and uses `hyp.finetune.yaml` parameters to continue training

# Image Detection

To run detection, place images to run detection on under `data/images`. Then run the following command:  

```
python detect.py --weights weights/best.pt --img 640 --conf 0.4
```

Predictions will be saved under `runs/detect/expX`, where `X` is the experiment number.

To run detection using your webcam as a source, run the following command

```
python detect.py --weights weights/best_21_01_2022.pt --source 0
``` 

# Running Server for YOLOv5

Run the following command to start a server on `http://localhost:5000`

```
python app.py
```

To send an image for object detection with cURL, run the following command:

```
curl -X POST http://127.0.0.1:5000/predict --form "fileupload=@2008.jpg"
```

The server expects a `POST` request to `/predict` with a file attached under the `image` field in the form.

If you want to check sample code to send images to the server, check `send_images.py`

# TODO

- Create NGINX reverse proxy
- rpi send images from camera feed periodically to server

# Links

- https://github.com/ultralytics/yolov5
- https://www.youtube.com/watch?v=gDoMYuyY_qw
- https://youtu.be/a9Bre0YJ8L8Rocket
- https://youtu.be/5h5UtLau3Vc
- https://github.com/ultralytics/yolov5/issues/36