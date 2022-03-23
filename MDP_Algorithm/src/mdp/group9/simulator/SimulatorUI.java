package mdp.group9.simulator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class SimulatorUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SimulatorUI.fxml"));
        HBox rootElement = loader.load();

        Scene scene = new Scene(rootElement);
        stage.setScene(scene);
        stage.show();
    }
}
