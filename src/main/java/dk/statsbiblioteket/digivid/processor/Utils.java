package dk.statsbiblioteket.digivid.processor;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Utils {

    private static Logger log = LoggerFactory.getLogger(Utils.class);

    static final Text helper = new Text();
    static final double DEFAULT_WRAPPING_WIDTH = helper.getWrappingWidth();
    static final double DEFAULT_LINE_SPACING = helper.getLineSpacing();
    static final String DEFAULT_TEXT = helper.getText();

    public static double computeTextWidth(Font font, String text, double minimumWidth) {
        helper.setText(text);
        helper.setFont(font);

        helper.setWrappingWidth(0.0D);
        helper.setLineSpacing(0.0D);
        double d = Math.min(helper.prefWidth(-1.0D), minimumWidth);
        helper.setWrappingWidth((int) Math.ceil(d));
        d = Math.ceil(helper.getLayoutBounds().getWidth());

        helper.setWrappingWidth(DEFAULT_WRAPPING_WIDTH);
        helper.setLineSpacing(DEFAULT_LINE_SPACING);
        helper.setText(DEFAULT_TEXT);
        return d;
    }

    private static void rethrow(String description, Throwable e) {
        log.error("Fatal exception, closing down: '{}'", description, e);
        throw new RuntimeException(e);

//        Platform.exit();
//        System.exit(-1);

    }


    static public void errorDialog(String description, Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(description +
                             "Program generated a Fatal Error.'" +
                             description + "'. Close this " +
                             "dialog to terminate the application.");
        Optional<ButtonType> result = alert.showAndWait();

        rethrow(description,e);
    }

    static public void warningDialog(String informationStr) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(informationStr);
        alert.showAndWait();
    }

    public static List<List<String>> getCSV(String csvFile) throws IOException {
        List<List<String>> csvData = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(csvFile), StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] splitted = line.split(",");
            List<String> dataLine = new ArrayList<>(splitted.length);
            Collections.addAll(dataLine, splitted);
            csvData.add(dataLine);
        }
        return csvData;
    }

}