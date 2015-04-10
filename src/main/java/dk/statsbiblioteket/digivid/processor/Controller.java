package dk.statsbiblioteket.digivid.processor;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

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


    public AnchorPane content;

    public TableView tableView;
    public Label filelabel;

    public void loadFilenames(ActionEvent actionEvent) {
        ObservableList<FileObject> fileObjects = FXCollections.observableList(new ArrayList<FileObject>());
        FileObject file1 = new FileObject("file1", 1234L);
        FileObject file2 = new FileObject("file2", 3456L);
        fileObjects.add(file1);
        fileObjects.add(file2);
        tableView.setItems(fileObjects);
        tableView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    System.out.println(mouseEvent);
                    Parent newParent;
                    FXMLLoader loader;
                    try {
                        loader = new FXMLLoader(getClass().getResource("process.fxml"));
                        newParent = loader.load();
                        Controller controller =  loader.<Controller>getController();
                        FileObject thisRow = (FileObject) ((TableView) mouseEvent.getSource()).getSelectionModel().getSelectedItem();
                        controller.filelabel.setText(thisRow.getFilename());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Stage mainWindow = (Stage)  ((Node) mouseEvent.getSource()).getScene().getWindow();
                    mainWindow.setScene(new Scene(newParent));
                    mainWindow.show();
                }
            }
        });
    }
}
