package dk.statsbiblioteket.digivid.processor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class DigividProcessor extends Application {

	protected static String recordsDir;
    protected static String channelCSV;
    protected static String player;
    protected static String serial;

    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.setTitle("Video processor");
        initRootLayout(primaryStage);
    }

    /**
     * Initializes the root layout.
     */
    private void initRootLayout(Stage primaryStage) {
        try {
            // Load root layout from fxml file.
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(getClass().getClassLoader().getResource("filelist.fxml"));
            AnchorPane rootLayout = (AnchorPane) rootLoader.load();
            final Controller controller = rootLoader.getController();
            controller.setDataPath(Paths.get(recordsDir));
            controller.loadFilenames();
            // Show the scene containing the root layout.
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(rootLayout, screenBounds.getWidth(), screenBounds.getHeight());
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0);
    }

    public static void main(String[] args) {
        String propertiesLocation = System.getProperty("digivid.config");
        if (propertiesLocation == null) {
            throw new RuntimeException("Must define location of root properties with -Ddigivid.config=....");
        }
        Path propertiesPath = Paths.get(propertiesLocation);
        if (!Files.exists(propertiesPath)) {
            throw new RuntimeException("No such file: " + propertiesPath);
        }
        Properties properties = new Properties();
        try {
            properties.load(Files.newInputStream(propertiesPath));
        } catch (IOException e) {
            throw new RuntimeException("Could not read properties file " + propertiesPath, e);
        }
        recordsDir = properties.getProperty("digivid.processor.recordsdir");
        channelCSV = properties.getProperty("digivid.processor.channels");
        player = properties.getProperty("digivid.processor.player");
        serial = properties.getProperty("digivid.processor.serial");
        launch(args);
    }
}
