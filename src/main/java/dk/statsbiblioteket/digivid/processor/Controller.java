package dk.statsbiblioteket.digivid.processor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

/**
 *
 * In outline this sample is based on
 * https://docs.oracle.com/javafx/2/fxml_get_started/fxml_tutorial_intermediate.htm
 *
 * Some other things we need to do/understand:
 *
 * i) Loading a new scene (form) on the stage
 * http://www.coderanch.com/t/620836/JavaFX/java/switch-scenes-stage-javafx
 *
 *
 * ii) Submitting data from one screen to the next:
 * http://www.coderanch.com/t/621496/JavaFX/java/Submitting-form-data-screen-javafx
 * http://stackoverflow.com/questions/14187963/passing-parameters-javafx-fxml/14190310#14190310
 */
public class Controller {

    public TableView<FileObject> tableView;
    public Label filelabel;
    public TableColumn<FileObject, Date> lastmodifiedColumn;

    private Path dataPath;

    public TableView tableView;
    public Label filelabel;
    public DirectoryChooser directoryChooser = new DirectoryChooser();
    public Path getDataPath() {
        return dataPath;
    }

    public void setDataPath(Path dataPath) {
        this.dataPath = dataPath;
    }

    @FXML
    void initialize() {
        if (lastmodifiedColumn != null) {
            lastmodifiedColumn.setComparator(new Comparator<Date>() {
                @Override
                public int compare(Date o1, Date o2) {
                    return o1.compareTo(o2);
                }
            });
        }
        if (tableView != null) {
            tableView.addEventHandler(MouseEvent.MOUSE_CLICKED, new FileclickMouseEventHandler());
            loadFilenames();
        }
    }

    public void loadFilenames(ActionEvent actionEvent) {
        loadFilenames();
    }

    public void loadFilenames() {
        ObservableList<FileObject> fileObjects = FXCollections.observableList(new ArrayList<FileObject>());
        File fileDir = directoryChooser.showDialog(((Node)actionEvent.getTarget()).getScene().getWindow());
        try {
            Files.walk(Paths.get(fileDir.getCanonicalPath())).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    fileObjects.add(new FileObject(filePath.getFileName().toString(), 1234L)); // System.out.println(filePath);
                }
            });
        }
        catch (IOException iex) {
            iex.printStackTrace();
        }
        tableView.setItems(fileObjects);
        //tableView.addEventHandler(MouseEvent.MOUSE_CLICKED, new FileclickMouseEventHandler());
        if (dataPath != null) {
            DirectoryStream<Path> tsFiles = null;
            try {
                tsFiles = Files.newDirectoryStream(dataPath, "*.ts");
            } catch (IOException e) {
                throw new RuntimeException("" + dataPath.toAbsolutePath());
            }
            for (Path tsFile : tsFiles) {
                fileObjects.add(new FileObjectImpl(tsFile));
            }
            tableView.setItems(fileObjects);
        }
    }

    /*public static class FileclickMouseEventHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getClickCount() == 2) {
                Parent newParent;
                FXMLLoader loader;
                try {
                    loader = new FXMLLoader(getClass().getClassLoader().getResource("process.fxml"));
                    newParent = loader.load();
                    Controller controller = loader.<Controller>getController();
                    FileObjectImpl thisRow = (FileObjectImpl) ((TableView) mouseEvent.getSource()).getSelectionModel().getSelectedItem();
                    controller.filelabel.setText(thisRow.getFilename());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Stage mainWindow = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
                mainWindow.setScene(new Scene(newParent));
                mainWindow.show();
            }
        }
    }*/

}
