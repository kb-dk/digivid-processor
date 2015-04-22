package dk.statsbiblioteket.digivid.processor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Main extends Application {

    private Stage primaryStage;
    private AnchorPane rootLayout;
    private static String recordsDir;
    protected static String channelDir;

    @Override
    public void start(Stage primaryStage) throws Exception{
        /*final MediaPlayer oracleVid = new MediaPlayer(
                new Media("http://download.oracle.com/otndocs/products/javafx/oow2010-2.flv")
        );
        primaryStage.setScene(new Scene(new Group(new MediaView(oracleVid)), 540, 208));
        primaryStage.show();

        oracleVid.play();
        */

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Video processor");
        initRootLayout();

    }

    /**
     * Initializes the root layout.
     */
    public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(getClass().getClassLoader().getResource("filelist.fxml"));
            rootLayout = (AnchorPane) rootLoader.load();
            final Controller controller = rootLoader.getController();
            controller.setDataPath(Paths.get(recordsDir));
            controller.loadFilenames();
            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the main stage.
     * @return
     */
    public Stage getPrimaryStage() {
        return primaryStage;
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
        channelDir = properties.getProperty("digivid.processor.channels");
        launch(args);
    }


}
