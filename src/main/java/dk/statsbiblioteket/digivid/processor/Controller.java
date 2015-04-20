package dk.statsbiblioteket.digivid.processor;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import jfxtras.scene.control.CalendarPicker;
import jfxtras.scene.control.CalendarTextField;
import jfxtras.scene.control.CalendarTimePicker;
import jfxtras.scene.control.CalendarTimeTextField;

import java.awt.event.TextEvent;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

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

    /*public TableView<FileObject> tableView;
    public Label filelabel;
    public Label currentFilename;
    */

    private Path dataPath;
    private Stage myStage;
    @FXML
    public Label currentFilename;
    @FXML
    public TableColumn<FileObject, Date> lastmodifiedColumn;
    @FXML
    public javafx.scene.control.Label txtFilename;
    @FXML
    public javafx.scene.control.TextArea txtComments;
    @FXML
    public TableView<FileObject> tableView;
    @FXML
    public javafx.scene.control.ComboBox cmbQuality;
    @FXML
    public CalendarTimeTextField startTimePicker;
    @FXML
    public CalendarTextField startDatePicker;
    @FXML
    public CalendarTimeTextField endTimePicker;
    @FXML
    public CalendarTextField endDatePicker;
    @FXML
    public ToggleGroup channelGroup;
    @FXML
    public Label error;

    public TextField altChannel;
    @FXML
    public GridPane channelGridPane;

    @FXML
    public javafx.scene.control.DatePicker dpStart;
    @FXML
    public javafx.scene.control.DatePicker dpEnd;

    @FXML
    private void handleTableviewClicked(ActionEvent event) {
        System.out.println("You clicked me!");
        txtFilename.setText("Hello  ");
    }


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
                    StandardWatchEventKinds.ENTRY_DELETE);
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
        tableView.setOnMouseClicked(new FileclickMouseEventHandler());
    }

    private void nullifyLowerPane() {
        txtFilename.setText(null);
        txtComments.setText(null);
        altChannel.setText(null);
        cmbQuality.setValue(null);
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
        addChannelButton("dr1", "DR 1", "white", 0, 0);
        addChannelButton("dr2", "DR 2", "lightgray", 0, 1);
        addChannelButton("tv2d", "TV2 Danmark", "white", 0, 2);
        addChannelButton("tv2l", "TV2 Lorry", "lightgray", 0, 3);
        addChannelButton("tv2s", "TV Syd", "white", 0, 4);
        addChannelButton("tv2n", "TV2 Nord", "lightgray",1,0);
        addChannelButton("tvfyn", "TV Fyn", "darkgray",1,1);
        addChannelButton("tv2mv", "TV2 Midt/Vest", "lightgray", 1, 2);
        addChannelButton("tv2born", "TV2 Bornholm", "darkgray", 1, 3);
        addChannelButton("tv2oj", "TV2 Østjylland", "lightgray", 1, 4);
        addChannelButton("tv3", "TV3", "white", 2, 0);
        addChannelButton("tv3p", "TV3+", "lightgray", 2, 1);
        addChannelButton("tvdk", "TV Danmark", "white", 2, 2);
        addChannelButton("kanal5", "Kanal 5", "lightgray", 2, 3);
        addChannelButton("dk4", "DK4", "white", 2, 4);
        addChannelButton("tvsport", "TV Sport", "lightgray", 3, 0);
        altChannel = new TextField();
        altChannel.setId("altChannel");
        altChannel.setPrefWidth(150.0);
        channelGridPane.getChildren().add(altChannel);
        GridPane.setRowIndex(altChannel, 4);
        GridPane.setColumnIndex(altChannel, 0);

        altChannel.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                 if (newValue == null || newValue.length() == 0) {
                    for (Toggle tg: Controller.this.channelGroup.getToggles()) {
                        ((RadioButton) tg).setDisable(false); ;
                    }
                 } else {
                     for (Toggle tg: Controller.this.channelGroup.getToggles()) {
                         ((RadioButton) tg).setDisable(true);
                         tg.setSelected(false);
                     }
                 }
            }
        });
    }

    private void addChannelButton(String channelName, String displayName, String color, int row, int column) {
        Channel ch1 = new Channel(channelName, displayName, color);
        RadioButton rb1 = new RadioButton();
        rb1.setText(ch1.displayName);
        rb1.setUserData(ch1);
        rb1.setStyle("-fx-background-color:" + ch1.colour);
        rb1.setToggleGroup(channelGroup);
        rb1.setPrefWidth(150.0);
        channelGridPane.getChildren().add(rb1);
        GridPane.setColumnIndex(rb1, column);
        GridPane.setRowIndex(rb1, row);
    }


    public void loadFilenames(ActionEvent actionEvent) {
                loadFilenames();
            }

    public void loadFilenames() {
        if (tableView != null) {
            ObservableList<FileObject> fileObjects = FXCollections.observableList(new ArrayList<FileObject>());
            //tableView.addEventHandler(MouseEvent.MOUSE_CLICKED, new FileclickMouseEventHandler());
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

    /**
     * Reads all the values set by the user and sets them on the current FileObject before calling the commit()
     * method on that object.
     * @param actionEvent
     */
    public void commit(ActionEvent actionEvent) {
        FileObjectImpl thisRow = (FileObjectImpl) tableView.getSelectionModel().getSelectedItem();
        final Calendar startDateCalendar = startDatePicker.getCalendar();
        if (startDateCalendar == null) {
            error.setText("No Start Date Set.");
            return;
        }
        final Date startDate = startDateCalendar.getTime();
        final Date startTime = startTimePicker.getCalendar().getTime();
        startDate.setHours(startTime.getHours());
        startDate.setMinutes(startTime.getMinutes());
        thisRow.setStartDate(startDate);
        final Date endDate = endDatePicker.getCalendar().getTime();
        final Date endTime = endTimePicker.getCalendar().getTime();
        endDate.setHours(endTime.getHours());
        endDate.setMinutes(endTime.getMinutes());
        thisRow.setEndDate(endDate);
        final Toggle selectedToggle = channelGroup.getSelectedToggle();
        String altChannel = this.altChannel.getText();
        if (altChannel != null && altChannel.length() > 0 ) {
            thisRow.setChannel(altChannel);
        } else {
            String channel = null;
            if ( selectedToggle  != null ) {
                channel = ((Channel) ((RadioButton) selectedToggle).getUserData()).getChannelName();
            }
            thisRow.setChannel(channel);
        }
        thisRow.setQuality(cmbQuality.getValue().toString());
        thisRow.setVhsLabel(txtComments.getText());
        error.setText(null);
        thisRow.commit();
        nullifyLowerPane();
    }

    public class FileclickMouseEventHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                error.setText(null);
                FileObjectImpl thisRow = (FileObjectImpl) ((TableView) mouseEvent.getSource()).getSelectionModel().getSelectedItem();
                if (thisRow != null) {
                    Controller.this.txtFilename.setText(thisRow.getFilename());
                    GregorianCalendar startCalendar = new GregorianCalendar();
                    if (thisRow.getStartDate() != null) {
                        startCalendar.setTime(thisRow.getStartDate());
                        Controller.this.startDatePicker.setCalendar(startCalendar);
                        Controller.this.startTimePicker.setCalendar(startCalendar);
                    }
                    GregorianCalendar endCalendar = new GregorianCalendar();
                    if (thisRow.getEndDate() != null) {
                        endCalendar.setTime(thisRow.getEndDate());
                        Controller.this.endDatePicker.setCalendar(endCalendar);
                        Controller.this.endTimePicker.setCalendar(endCalendar);
                    }
                    final String quality = thisRow.getQuality();
                    if (quality != null) {
                        Controller.this.cmbQuality.getSelectionModel().select(quality);
                    }
                    Controller.this.txtComments.setText(thisRow.getVhsLabel());
                    String currentChannel = thisRow.getChannel();
                    boolean inGrid = false;
                    for (Node channelNode: Controller.this.channelGridPane.getChildren()) {
                        if (channelNode instanceof RadioButton) {
                            Channel buttonChannel = (Channel) channelNode.getUserData();
                            if (buttonChannel.getChannelName().equals(currentChannel)) {
                                ((RadioButton) channelNode).setSelected(true);
                                inGrid = true;
                            } else {
                                ((RadioButton) channelNode).setSelected(false);
                            }
                        }
                    }
                    if (inGrid) {
                        Controller.this.altChannel.setText(null);
                    } else {
                        Controller.this.altChannel.setText(thisRow.getChannel());
                    }
                }
            }
        }
    }

}
