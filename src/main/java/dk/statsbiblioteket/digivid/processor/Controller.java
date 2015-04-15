package dk.statsbiblioteket.digivid.processor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
    public Label currentFilename;

    private Path dataPath;
    private Stage myStage;

    public DirectoryChooser directoryChooser = new DirectoryChooser();

    public void setStage(Stage stage) {
        myStage = stage;
    }

    public Path getDataPath() {
        return dataPath;
    }

    public void setDataPath(Path dataPath) {
        this.dataPath = dataPath;
        try {
            WatchService service = getDataPath().getFileSystem().newWatchService();
            getDataPath().register(service,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            new Thread(){
                @Override
                public void run() {
                    while(true) {
                        try {
                            WatchKey key = service.take();
                            if (!key.pollEvents().isEmpty()) {
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadFilenames();
                                    }
                                });
                            }
                            key.reset();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tableView.addEventHandler(MouseEvent.MOUSE_CLICKED, new FileclickMouseEventHandler());
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
    }

    public void loadFilenames(ActionEvent actionEvent) {
        loadFilenames();
    }

    public void loadFilenames() {
        if (tableView != null) {
            ObservableList<FileObject> fileObjects = FXCollections.observableList(new ArrayList<FileObject>());
            if (getDataPath() != null) {
                DirectoryStream<Path> tsFiles = null;
                try {
                    tsFiles = Files.newDirectoryStream(getDataPath(), "*.ts");
                    for (Path tsFile : tsFiles) {
                        fileObjects.add(new FileObjectImpl(tsFile));
                    }
                    tableView.setItems(fileObjects);
                } catch (IOException e) {
                    throw new RuntimeException("" + getDataPath().toAbsolutePath());
                } finally {
                    try {
                        tsFiles.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else
            {
                try {
                    Files.createDirectories(getDataPath());
                }
                catch (IOException iex) {
                    throw new RuntimeException("" + getDataPath().toAbsolutePath());
                }
            }
        }
    }

    public class FileclickMouseEventHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                FileObjectImpl thisRow = (FileObjectImpl) ((TableView) mouseEvent.getSource()).getSelectionModel().getSelectedItem();
                Controller.this.currentFilename.setText(thisRow.getFilename());



            }
            /*if (mouseEvent.getClickCount() == 2) {
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
            }*/
        }
    }

}
