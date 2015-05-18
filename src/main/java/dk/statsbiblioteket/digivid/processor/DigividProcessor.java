package dk.statsbiblioteket.digivid.processor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;


public class DigividProcessor extends Application {

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

    static public void showErrorDialog(Thread t, Throwable e) {

        Dialogs.create().title("Error").message("An uncaught exception was thrown in thread " + t + ".\n" +
                "Click below to view the stacktrace, or close this " +
                "dialog to terminate the application.").showException(e);
        Platform.exit();
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Platform.runLater(() -> showErrorDialog(t, e)));
        Thread.currentThread().setUncaughtExceptionHandler(DigividProcessor::showErrorDialog);

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
            AnchorPane rootLayout = rootLoader.load();
            final Controller controller = rootLoader.getController();
            controller.setDataPath(Paths.get(recordsDir));
            controller.loadFilenames();
            // Show the scene containing the root layout.
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(rootLayout, screenBounds.getWidth(), screenBounds.getHeight());
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException ioe) {
            log.error("Error occured while loading file in initRootLayout", ioe);
            DigividProcessor.showErrorDialog(Thread.currentThread(), ioe);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0);
    }

}
