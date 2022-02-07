package mdp.group9.simulator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Popup;
import javafx.util.Duration;
import mdp.group9.*;

import java.util.ArrayList;
import java.util.List;

public class SimulatorController {

    @FXML
    private GridPane gridPane;

    @FXML
    private ImageView robotCar;

    private PathPlanner planner = new PathPlanner();
    private Obstacle[] obstacles;
    public static final double GRID_SIZE = 30.0;

    @FXML
    public void startPath() throws Exception {
        List<List<AStar.Cell>> path = planner.planPath(obstacles);
        Timeline timeline = new Timeline();
        Alert readImgAlert = new Alert(Alert.AlertType.NONE, "Recognising image...", ButtonType.OK);
        readImgAlert.setTitle("Please Wait");

        Duration currTime = Duration.millis(0);
        for (List<AStar.Cell> currPath : path) {
            for (AStar.Cell cell : currPath) {
                timeline.getKeyFrames().add(new KeyFrame(currTime, actionEvent -> moveRobot(cell.x, cell.y)));
                currTime = currTime.add(Duration.millis(500));
            }
            currTime = currTime.add(Duration.seconds(1));
//            timeline.getKeyFrames().add(new KeyFrame(currTime, actionEvent -> readImgAlert.show()));
//            currTime = currTime.add(Duration.seconds(3)); // pause for 3s to simulate "recognition"
//            timeline.getKeyFrames().add(new KeyFrame(currTime, actionEvent -> readImgAlert.hide()));
        }

//        Alert finishedAlert = new Alert(Alert.AlertType.INFORMATION, "Robot successfully visited every image!");
//        finishedAlert.setHeaderText("Simulation complete.");

        timeline.play();
//        timeline.setOnFinished(actionEvent -> finishedAlert.show());
    }

    /**
     * Allows users to set obstacles from GUI
     */
    @FXML
    public void setObstacles() {
        gridPane.getChildren().removeIf(node -> node instanceof Rectangle); // clear grid of obstacles
        List<Obstacle> obstaclesList = new ArrayList<>(); // temp list to hold created obstacles
        Scene scene = robotCar.getScene();

        for (int i = 1; i <= 10; i++) {
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
            gridPane.add(createImage(position.getDir(), Color.DEEPSKYBLUE, Color.BLACK),
                    position.getX(), gridPane.getRowCount() - 1 - position.getY(), 1, 1);

            // add boundaries
            for (int virtualX = -1; virtualX <= 1; virtualX++) {
                for (int virtualY = -1; virtualY <= 1; virtualY++) {
                    if (!(virtualX == 0 && virtualY == 0)) {
                        int boundaryX = virtualX + position.getX();
                        int boundaryY = virtualY + position.getY();
                        if (boundaryX >= 0 && boundaryX <= 19 && boundaryY >= 0 && boundaryY <= 19) {
                            // virtual boundary within grid limits
                            gridPane.add(new Rectangle(GRID_SIZE, GRID_SIZE, Color.LIGHTGRAY),
                                    boundaryX, gridPane.getRowCount() - 1 - boundaryY, 1, 1);
                        }
                    }
                }
            }

            // add image reading position
            Position readingPos = planner.getTargetPosition(obstacle);
            gridPane.add(createImage(readingPos.getDir(), Color.ORANGE, Color.DARKGOLDENROD),
                    readingPos.getX(), gridPane.getRowCount() - 1 - readingPos.getY(), 1, 1);
        }

        Arena arena = new Arena(new RobotCar(), obstacles);
        int[][] grid = arena.getGrid();
        planner.setGrid(grid);
    }

    /**
     * Helper method to move robot from one grid cell to another
     * @param x
     * @param y
     */
    private void moveRobot(int x, int y) {
        // if needed, rotate robot
        int oldX = GridPane.getColumnIndex(robotCar);
        int oldY = gridPane.getRowCount() - 1 - GridPane.getRowIndex(robotCar);

        // colour the robot's path
        gridPane.add(new Rectangle(GRID_SIZE, GRID_SIZE, Color.LIGHTGREEN), oldX,
                gridPane.getRowCount() - 1 - oldY, 1, 1);

        if (x - oldX > 0 && robotCar.getRotate() != 0) {
            // heading east, but robotCar not facing east
            robotCar.setRotate(0);
        } else if (x - oldX < 0 && robotCar.getRotate() != 180) {
            // heading west, but robotCar not facing west
            robotCar.setRotate(180);
        } else if (y - oldY > 0 && robotCar.getRotate() != -90) {
            // heading north, but robotCar not facing north
            robotCar.setRotate(-90);
        } else if (y - oldY < 0 && robotCar.getRotate() != 90) {
            // heading south, but robotCar not facing south
            robotCar.setRotate(90);
        } else if (x - oldX > 0 && y - oldY > 0 && robotCar.getRotate() != -45) {
            // heading northeast, but robotCar not facing northeast
            robotCar.setRotate(-45);
        } else if (x - oldX < 0 && y - oldY > 0 && robotCar.getRotate() != 225) {
            // heading northwest, but robotCar not facing northwest
            robotCar.setRotate(225);
        } else if (x - oldX > 0 && y - oldY < 0 && robotCar.getRotate() != 45) {
            // heading southeast, but robotCar not facing southeast
            robotCar.setRotate(45);
        } else if (x - oldX < 0 && y - oldY < 0 && robotCar.getRotate() != 135) {
            // heading southwest, but robotCar not facing southwest
            robotCar.setRotate(135);
        }

        gridPane.getChildren().remove(robotCar);
        gridPane.add(robotCar, x, gridPane.getRowCount() - 1 - y, 1, 1);
    }

    /**
     * Helper method to create an Image represented using a Pane
     * @param directionUI
     * @return
     */
    private Rectangle createImage(Direction directionUI, Color fillColor, Color headColor) {
        Rectangle image = new Rectangle(GRID_SIZE, GRID_SIZE);
        Paint background = new LinearGradient(0.7109004739336492, 0.4360189573459716,
                0.7203791469194313, 0.4360189573459715,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, fillColor), new Stop(1, headColor));
        image.setFill(background);
        image.setStroke(Color.BLACK);
        image.setStrokeType(StrokeType.INSIDE);
        switch (directionUI) {
            case NORTH -> image.setRotate(-90);
            case SOUTH -> image.setRotate(90);
            case WEST -> image.setRotate(180);
        }
        return image;
    }
}
