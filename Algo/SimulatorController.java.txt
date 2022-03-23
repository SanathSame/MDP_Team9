package mdp.group9.simulator;

import javafx.animation.*;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.util.Duration;
import mdp.group9.*;
import mdp.group9.tasks.ImageRecTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.security.Key;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SimulatorController {

    @FXML
    private Pane arenaPane;
    @FXML
    private Text timerText;
    @FXML
    private Text presetText;
    @FXML
    private Pane car;

    private LocalTime timer;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("mm:ss:SS");
    private Timeline timerTimeline;
    private SequentialTransition carAnimation;

    private final PathPlanner planner = new PathPlanner();
    private Obstacle[] obstacles;
    private List<AStar.Cell> pathsCombined; // for next and prev step
    private int currStep; // for next and prev step
    private int currPreset = 1;

    public static final double GRID_SIZE = 30.0;
    public static final double GRID_SIZE_CM = 10.0;
    private final int MAX_OBSTACLES = 8;
    private final double TURN_RADIUS_FORWARD = 20 / GRID_SIZE_CM * GRID_SIZE;
    private final double TURN_RADIUS_SIDE = 40 / GRID_SIZE_CM * GRID_SIZE;

    private int readImgSpeed = 500;
    private int straightLineSpeed = 20; // per cm
    private int rotateSpeed = 1000;

    @FXML
    public void startPath() throws Exception {
        if (carAnimation != null) carAnimation.stop();
        if (timerTimeline != null) timerTimeline.stop();

        List<List<AStar.Cell>> path = planner.planPath(obstacles);

        // timer
        timerText.setFill(Color.BLACK);
        timer = LocalTime.parse("00:00:00");
        timerTimeline = new Timeline(new KeyFrame(Duration.ONE, actionEvent -> {
            timer = timer.plus(1, ChronoUnit.MILLIS);
            timerText.setText(timer.format(dtf));
        }));
        timerTimeline.setCycleCount(Animation.INDEFINITE);

        carAnimation = new SequentialTransition();
        double currX = car.getLayoutX(), currY = car.getLayoutY();
        int currRotate = (int) car.getRotate();
        KeyValue xKeyValue = null, yKeyValue = null, rotateKeyValue = null;
        for (List<AStar.Cell> currPath : path) {
            List<String> commands = new MotionPlanner().toCommands(currPath);
            System.out.println(commands);
            for (String command : commands) {
                Timeline timeline = new Timeline();
                if (command.startsWith("F") || command.startsWith("B"))
                {
                    double distanceInCm = Integer.parseInt(command.substring(2).trim());
                    double distance = distanceInCm / GRID_SIZE_CM * GRID_SIZE;
                    distance = command.startsWith("B") ? -distance : distance;
                    switch (currRotate) {
                        case 270 -> {
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY - distance);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(straightLineSpeed * distanceInCm), yKeyValue));
                            currY = (double) yKeyValue.getEndValue();
                        }
                        case 0, 360 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX + distance);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(straightLineSpeed * distanceInCm), xKeyValue));
                            currX = (double) xKeyValue.getEndValue();
                        }
                        case 90 -> {
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY + distance);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(straightLineSpeed * distanceInCm), yKeyValue));
                            currY = (double) yKeyValue.getEndValue();
                        }
                        case 180 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX - distance);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(straightLineSpeed * distanceInCm), xKeyValue));
                            currX = (double) xKeyValue.getEndValue();
                        }
                    }
                }

                else if (command.startsWith("RF"))
                {
                    switch (currRotate) {
                        case 270 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX + TURN_RADIUS_SIDE);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY - TURN_RADIUS_FORWARD);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 360);
                            // have to manually set rotate to 0 so that future turns turn clockwise rather than counter-clockwise
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), actionEvent -> car.setRotate(0),
                                    xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 0, 360 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX + TURN_RADIUS_FORWARD);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY + TURN_RADIUS_SIDE);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 90);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 90 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX - TURN_RADIUS_SIDE);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY + TURN_RADIUS_FORWARD);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 180);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 180 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX - TURN_RADIUS_FORWARD);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY - TURN_RADIUS_SIDE);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 270);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                    }
                    currX = (double) xKeyValue.getEndValue();
                    currY = (double) yKeyValue.getEndValue();
                    currRotate = (int) rotateKeyValue.getEndValue();
                    if (currRotate == 360) currRotate = 0;
                }

                else if (command.startsWith("RB"))
                {
                    switch (currRotate) {
                        case 270 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX + TURN_RADIUS_FORWARD);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY + TURN_RADIUS_SIDE);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 180);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 0, 360 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX - TURN_RADIUS_SIDE);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY + TURN_RADIUS_FORWARD);
                            if (currRotate == 0) {
                                rotateKeyValue = new KeyValue(car.rotateProperty(), -90);
                            } else {
                                rotateKeyValue = new KeyValue(car.rotateProperty(), 270);
                            }
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), actionEvent -> car.setRotate(270),
                                    xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 90 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX - TURN_RADIUS_FORWARD);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY - TURN_RADIUS_SIDE);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 0);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 180 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX + TURN_RADIUS_SIDE);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY - TURN_RADIUS_FORWARD);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 90);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                    }
                    currX = (double) xKeyValue.getEndValue();
                    currY = (double) yKeyValue.getEndValue();
                    currRotate = (int) rotateKeyValue.getEndValue();
                    if (currRotate == -90) currRotate = 270;
                }

                else if (command.startsWith("LF"))
                {
                    switch (currRotate) {
                        case 270 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX - TURN_RADIUS_SIDE);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY - TURN_RADIUS_FORWARD);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 180);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 0, 360 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX + TURN_RADIUS_FORWARD);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY - TURN_RADIUS_SIDE);
                            if (currRotate == 0) {
                                rotateKeyValue = new KeyValue(car.rotateProperty(), -90);
                            } else {
                                rotateKeyValue = new KeyValue(car.rotateProperty(), 270);
                            }
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), actionEvent -> car.setRotate(270),
                                    xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 90 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX + TURN_RADIUS_SIDE);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY + TURN_RADIUS_FORWARD);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 0);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 180 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX - TURN_RADIUS_FORWARD);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY + TURN_RADIUS_SIDE);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 90);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                    }
                    currX = (double) xKeyValue.getEndValue();
                    currY = (double) yKeyValue.getEndValue();
                    currRotate = (int) rotateKeyValue.getEndValue();
                    if (currRotate == -90) currRotate = 270;
                }

                else if (command.startsWith("LB"))
                {
                    switch (currRotate) {
                        case 270 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX - TURN_RADIUS_FORWARD);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY + TURN_RADIUS_SIDE);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 360);
                            // have to manually set rotate to 0 so that future turns turn clockwise rather than counter-clockwise
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), actionEvent -> car.setRotate(0),
                                    xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 0, 360 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX - TURN_RADIUS_SIDE);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY - TURN_RADIUS_FORWARD);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 90);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 90 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX + TURN_RADIUS_FORWARD);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY - TURN_RADIUS_SIDE);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 180);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                        case 180 -> {
                            xKeyValue = new KeyValue(car.layoutXProperty(), currX + TURN_RADIUS_SIDE);
                            yKeyValue = new KeyValue(car.layoutYProperty(), currY + TURN_RADIUS_FORWARD);
                            rotateKeyValue = new KeyValue(car.rotateProperty(), 270);
                            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(rotateSpeed), xKeyValue, yKeyValue, rotateKeyValue));
                        }
                    }
                    currX = (double) xKeyValue.getEndValue();
                    currY = (double) yKeyValue.getEndValue();
                    currRotate = (int) rotateKeyValue.getEndValue();
                    if (currRotate == 360) currRotate = 0;
                }

                if (timeline.getKeyFrames().size() != 0) {
                    carAnimation.getChildren().add(timeline);
                }
            }

            // add pause for reading image
            carAnimation.getChildren().add(new PauseTransition(Duration.millis(readImgSpeed)));
        }

        carAnimation.setOnFinished(actionEvent -> {
            timerText.setFill(Color.RED);
            timerTimeline.stop();
        });
        carAnimation.play();
        timerTimeline.play();
    }

    @FXML
    public void pausePath() {
        timerTimeline.pause();
        carAnimation.pause();
    }

    /**
     * Allows users to set obstacles from GUI
     */
    @FXML
    public void setArena() {
        Scene scene = arenaPane.getScene();
        arenaPane.getChildren().removeIf(node -> node instanceof Rectangle); // clear grid of obstacles

        List<Obstacle> obstaclesList = new ArrayList<>(); // temp list to hold created obstacles
        for (int i = 1; i <= MAX_OBSTACLES; i++) {
            String obX = ((TextField) scene.lookup("#obX" + i)).getText();
            String obY = ((TextField) scene.lookup("#obY" + i)).getText();
            String obDir = ((TextField) scene.lookup("#obDir" + i)).getText();

            if (obX.isEmpty() && obY.isEmpty() && obDir.isEmpty()) {
                // no obstacle set
                continue;
            } else if (obX.isEmpty() || obY.isEmpty() || obDir.isEmpty()) {
                // obstacle not filled in properly
                System.out.println("Obstacle " + i + " is missing details!");
                new Alert(Alert.AlertType.ERROR, "Obstacle " + i + " is missing details!").showAndWait();
                return;
            } else {
                // valid obstacle set
                // check valid direction
                Direction dir;
                switch (obDir.toUpperCase()) {
                    case "N" -> dir = Direction.NORTH;
                    case "S" -> dir = Direction.SOUTH;
                    case "E" -> dir = Direction.EAST;
                    case "W" -> dir = Direction.WEST;
                    default -> {
                        System.out.println("Obstacle " + i + " has invalid direction! Use only N/S/E/W");
                        new Alert(Alert.AlertType.ERROR, "Obstacle " + i + " has invalid direction! Use only N/S/E/W").showAndWait();
                        return;
                    }
                }

                // check valid x and y
                try {
                    int x = Integer.parseInt(obX) - 1; // UI is 1-indexed
                    int y = Integer.parseInt(obY) - 1; // UI is 1-indexed
                    if (x < 0 || x > 19 || y < 0 || y > 19) {
                        System.out.println("Obstacle " + i + " has invalid x or y!");
                        new Alert(Alert.AlertType.ERROR, "Obstacle " + i + " has invalid x or y!").showAndWait();
                        return;
                    }
                    obstaclesList.add(new Obstacle(x, y, dir));
                } catch (NumberFormatException e) {
                    System.out.println("Obstacle " + i + " has invalid x or y!");
                    new Alert(Alert.AlertType.ERROR, "Obstacle " + i + " has invalid x or y!").showAndWait();
                }
            }
        }

        if (obstaclesList.isEmpty()) {
            return;
        }

        // create the obstacles
        obstacles = obstaclesList.toArray(new Obstacle[0]);
        for (Obstacle obstacle : obstacles) {
            Position position = obstacle.getPos();
            Rectangle obstacleImg = createImage(position.getDir(), Color.DEEPSKYBLUE, Color.BLACK);
            obstacleImg.setLayoutX(position.getX() * GRID_SIZE);
            obstacleImg.setLayoutY((Arena.HEIGHT - 1 - position.getY()) * GRID_SIZE);
            arenaPane.getChildren().add(obstacleImg);

            // add boundaries
            for (int virtualX = -1; virtualX <= 1; virtualX++) {
                for (int virtualY = -1; virtualY <= 1; virtualY++) {
                    if (!(virtualX == 0 && virtualY == 0)) {
                        int boundaryX = virtualX + position.getX();
                        int boundaryY = virtualY + position.getY();
                        if (boundaryX >= 0 && boundaryX <= 19 && boundaryY >= 0 && boundaryY <= 19) {
                            // virtual boundary within grid limits
                            Rectangle boundaryImg = new Rectangle(GRID_SIZE, GRID_SIZE, Color.LIGHTGRAY);
                            boundaryImg.setLayoutX(boundaryX * GRID_SIZE);
                            boundaryImg.setLayoutY((Arena.HEIGHT - 1 - boundaryY) * GRID_SIZE);
                            arenaPane.getChildren().add(boundaryImg);
                        }
                    }
                }
            }

            // add image reading position
            Position readingPos = planner.getTargetPosition(obstacle);
            Rectangle readingPosImg = createImage(readingPos.getDir(), Color.ORANGE, Color.DARKGOLDENROD);
            readingPosImg.setLayoutX(readingPos.getX() * GRID_SIZE);
            readingPosImg.setLayoutY((Arena.HEIGHT - 1 - readingPos.getY()) * GRID_SIZE);
            arenaPane.getChildren().add(readingPosImg);
        }

        Arena arena = new Arena(new RobotCar(), obstacles);
        int[][] grid = arena.getGrid();
        planner.setGrid(grid);

        // create robot
        int robotX = Integer.parseInt(((TextField) scene.lookup("#robotX")).getText()) - 1;
        int robotY = Integer.parseInt(((TextField) scene.lookup("#robotY")).getText()) - 1;
        Direction robotDir = Direction.NORTH;
        switch (((TextField) scene.lookup("#robotDir")).getText()) {
            case "N" -> {
                robotDir = Direction.NORTH;
                car.setRotate(270);
            }
            case "S" -> {
                robotDir = Direction.SOUTH;
                car.setRotate(90);
            }
            case "E" -> {
                robotDir = Direction.EAST;
                car.setRotate(0);
            }
            case "W" -> {
                robotDir = Direction.WEST;
                car.setRotate(180);
            }
        }
        planner.setStartingPos(robotX, robotY, robotDir);
        car.setLayoutX((robotX - 1) * GRID_SIZE); // additional -1 to account for car width
        car.setLayoutY((Arena.HEIGHT - 1 - robotY - 1) * GRID_SIZE); // additional -1 since Y starts from top
        arenaPane.getChildren().remove(car);
        arenaPane.getChildren().add(car);
    }

    /**
     * Helper method to move robot from one grid cell to another
     * @param x
     * @param y
     */
    private void moveRobot(int x, int y, Direction dir) {
        car.setLayoutX((x - 1) * GRID_SIZE); // additional -1 to account for car width
        car.setLayoutY((Arena.HEIGHT - 1 - y - 1) * GRID_SIZE); // additional -1 since Y starts from top
        switch (dir) {
            case NORTH -> car.setRotate(270);
            case SOUTH -> car.setRotate(90);
            case EAST -> car.setRotate(0);
            case WEST -> car.setRotate(180);
        }
    }

    @FXML
    private void setPath() throws Exception {
        List<List<AStar.Cell>> paths = planner.planPath(obstacles);
        pathsCombined = paths.get(0);
        MotionPlanner motionPlanner = new MotionPlanner();

        // print commands
        List<String> commands = motionPlanner.toCommands(paths.get(0));
        System.out.println(commands);
        for (int i = 1; i < paths.size(); i++) {
            // print commands
            commands = motionPlanner.toCommands(paths.get(i));
            System.out.println(commands);

            paths.get(i).remove(0); // first cell of curr path same as last cell of prev path
            pathsCombined.addAll(paths.get(i));
        }
        currStep = 0;
    }

    @FXML
    private void nextStep() {
        if (currStep+1 != pathsCombined.size()) {
            currStep++;
            AStar.Cell currCell = pathsCombined.get(currStep);
            moveRobot(currCell.x, currCell.y, currCell.dir);
        }
    }

    @FXML
    private void prevStep() {
        if (currStep > 0) {
            currStep--;
            AStar.Cell currCell = pathsCombined.get(currStep);
            moveRobot(currCell.x, currCell.y, currCell.dir);
        }
    }

    @FXML
    private void nextPreset() throws Exception {
        presetText.setText((++currPreset) + "");
        setPreset();
    }

    @FXML
    private void prevPreset() throws Exception {
        presetText.setText((--currPreset) + "");
        setPreset();
    }

    private void setPreset() throws Exception {
        File file = new File("resources/mdp/group9/simulator/presets/preset_" + currPreset + ".txt");
        Scanner reader = new Scanner(file);
        reader.nextLine(); // remove header
        Scene scene = arenaPane.getScene();

        // read robot
        String[] robotLine = reader.nextLine().split(" ");
        ((TextField) scene.lookup("#robotX")).setText(robotLine[0]);
        ((TextField) scene.lookup("#robotY")).setText(robotLine[1]);
        ((TextField) scene.lookup("#robotDir")).setText(robotLine[2]);

        // read obstacles
        int counter = 1;
        while (reader.hasNextLine()) {
            String[] obstacleLine = reader.nextLine().split(" ");
            ((TextField) scene.lookup("#obX" + counter)).setText(obstacleLine[0]);
            ((TextField) scene.lookup("#obY" + counter)).setText(obstacleLine[1]);
            ((TextField) scene.lookup("#obDir" + counter)).setText(obstacleLine[2]);
            counter++;
        }

        // remove existing obstacles if not overwritten
        for (; counter <= MAX_OBSTACLES; counter++) {
            ((TextField) scene.lookup("#obX" + counter)).setText("");
            ((TextField) scene.lookup("#obY" + counter)).setText("");
            ((TextField) scene.lookup("#obDir" + counter)).setText("");
        }

        setArena();
    }

    @FXML
    private void saveSettings() {
        Scene scene = arenaPane.getScene();
        straightLineSpeed = Integer.parseInt(((TextField) scene.lookup("#straightLineSpeedField")).getText());
        rotateSpeed = Integer.parseInt(((TextField) scene.lookup("#rotateSpeedField")).getText());
        readImgSpeed = Integer.parseInt(((TextField) scene.lookup("#readImgSpeedField")).getText());
        AStar.SAFE_TURN = ((CheckBox) scene.lookup("#safeTurnCheckBox")).isSelected();
        AStar.ALLOW_EDGE_MOVEMENT = ((CheckBox) scene.lookup("#moveOnEdgeCheckBox")).isSelected();
    }

    /**
     * Helper method to create an Image represented using a Pane
     * @param directionUI
     * @return
     */
    public Rectangle createImage(Direction directionUI, Color fillColor, Color headColor) {
        Rectangle image = new Rectangle(GRID_SIZE, GRID_SIZE);
        Paint background = new LinearGradient(0.7109004739336492, 0.4360189573459716,
                0.7203791469194313, 0.4360189573459715,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, fillColor), new Stop(1, headColor));
        image.setFill(background);
        image.setStroke(Color.BLACK);
        image.setStrokeType(StrokeType.INSIDE);
        switch (directionUI) {
            case NORTH -> image.setRotate(270);
            case SOUTH -> image.setRotate(90);
            case WEST -> image.setRotate(180);
        }
        return image;
    }
}
