package dk.statsbiblioteket.digivid.processor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Performs initializing steps like reading information from the property file and setup the GUI
 */
public class DigividProcessor extends Application {

    static public Stage primaryStage;
    protected static String recordsDir;
    protected static String channelCSV;
    protected static String player;
    protected static String localProperties;
    private static Logger log = LoggerFactory.getLogger(DigividProcessor.class);

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
        localProperties = properties.getProperty("digivid.processor.localVHSProperties");
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        DigividProcessor.primaryStage = stage;
        DigividProcessor.primaryStage.setTitle("Video processor");


        // Load root layout from fxml file.
        FXMLLoader rootLoader = new FXMLLoader();
        rootLoader.setLocation(getClass().getClassLoader().getResource("filelist.fxml"));
        AnchorPane rootLayout = rootLoader.load();

        //Setup the controller
        final Controller controller = rootLoader.getController();
        controller.setDataPath(Paths.get(recordsDir));
        controller.setupFolderWatcher();
        controller.setupTableView();
        controller.loadFilenames();

        // Set the scene containing the root layout.
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(rootLayout, screenBounds.getWidth(), screenBounds.getHeight());
        DigividProcessor.primaryStage.setScene(scene);

        Platform.setImplicitExit(true);
        //Show the scene, blocks until closed
        DigividProcessor.primaryStage.show();

    }

    @Override
    public void init() throws Exception {
        super.init();
        //Uncaught exceptions should become error dialogs
        Thread.setDefaultUncaughtExceptionHandler(
                (Thread t, Throwable e) -> {
                    Platform.runLater(
                            () -> {
                                Utils.errorDialog("Caught Exception " + e + " in thread " + t.toString(), e);
                            });
                });
        Thread.currentThread().setUncaughtExceptionHandler(
                (Thread t, Throwable e) -> {
                    Utils.errorDialog("Caught Exception " + e + " in thread " + t.toString(), e);
                });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        //Window closed, so kill the app
        Platform.exit();
        //System.exit(0);
    }
}
