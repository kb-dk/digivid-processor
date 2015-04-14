package dk.statsbiblioteket.digivid.processor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Paths;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader rootLoader = new FXMLLoader(getClass().getClassLoader().getResource("filelist.fxml"));
        Parent root = rootLoader.load();
        ((Controller) rootLoader.getController()).setDataPath(Paths.get("src/test/data/emptyData"));
        primaryStage.setTitle("Hello World");
        final Scene scene = new Scene(root, 900, 675);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
