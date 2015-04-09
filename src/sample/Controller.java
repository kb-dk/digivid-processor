package sample;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;

public class Controller {
    public TableView tableView;

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
                }
            }
        });
    }
}
