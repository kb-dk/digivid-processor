package dk.statsbiblioteket.digivid.processor;

import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import org.controlsfx.dialog.Dialogs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {

    static final Text helper;
    static final double DEFAULT_WRAPPING_WIDTH;
    static final double DEFAULT_LINE_SPACING;
    static final String DEFAULT_TEXT;
    static final TextBoundsType DEFAULT_BOUNDS_TYPE;

    static {
        helper = new Text();
        DEFAULT_WRAPPING_WIDTH = helper.getWrappingWidth();
        DEFAULT_LINE_SPACING = helper.getLineSpacing();
        DEFAULT_TEXT = helper.getText();
        DEFAULT_BOUNDS_TYPE = helper.getBoundsType();
    }

    public static double computeTextWidth(Font font, String text, double help0) {
        helper.setText(text);
        helper.setFont(font);

        helper.setWrappingWidth(0.0D);
        helper.setLineSpacing(0.0D);
        double d = Math.min(helper.prefWidth(-1.0D), help0);
        helper.setWrappingWidth((int) Math.ceil(d));
        d = Math.ceil(helper.getLayoutBounds().getWidth());

        helper.setWrappingWidth(DEFAULT_WRAPPING_WIDTH);
        helper.setLineSpacing(DEFAULT_LINE_SPACING);
        helper.setText(DEFAULT_TEXT);
        return d;
    }

    static public void showErrorDialog(String description, Thread t, Throwable e) {
        if (e == null)
            Dialogs.create().title("Error").message(description).showError();
        else
            Dialogs.create().title("Error").message(description +
                    "An uncaught exception was thrown in thread " + t + ".\n" +
                    "Click below to view the stacktrace, or close this " +
                    "dialog to terminate the application.").showException(e);
        Platform.exit();
        System.exit(-1);
    }

    static public void showErrorDialog(Thread t, Throwable e) {
        Utils.showErrorDialog("", t, e);
    }

    static public void showWarning(String informationStr) {
        Dialogs.create().title("Warning").message(informationStr).showWarning();
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