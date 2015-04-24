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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import jfxtras.scene.control.CalendarTextField;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

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
    public TableColumn<FileObject, Boolean> processedColumn;
    @FXML
    public javafx.scene.control.Label txtFilename;
    @FXML
    public javafx.scene.control.TextField txtVhsLabel;
    @FXML
    public javafx.scene.control.TextArea txtComment;
    @FXML
    public TableView<FileObject> tableView;
    @FXML
    public javafx.scene.control.ComboBox cmbQuality;
    @FXML
    public TextField startTimeField;
    @FXML
    public DatePicker startDatePicker;
    @FXML
    public TextField endTimeField;
    @FXML
    public DatePicker endDatePicker;
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
    public javafx.scene.layout.AnchorPane detailVHS;
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
        txtVhsLabel.setText(null);
        txtComment.setText(null);
        altChannel.setText(null);
        cmbQuality.setValue(null);
    }

    @FXML
    void initialize() {
        detailVHS.setVisible(false);
        if (lastmodifiedColumn != null) {
            lastmodifiedColumn.setComparator(new Comparator<Date>() {
                @Override
                public int compare(Date o1, Date o2) {
                    return o1.compareTo(o2);
                }
            });
        }
        createChannels(Main.channelCSV);
        altChannel = new TextField();
        altChannel.setId("altChannel");
        altChannel.setPrefWidth(150.0);
        channelGridPane.getChildren().add(altChannel);
        GridPane.setRowIndex(altChannel, 4);
        GridPane.setColumnIndex(altChannel, 0);

        startDatePicker.setOnMouseClicked(event ->
                endDatePicker.setValue (startDatePicker.getValue()));

        altChannel.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                if (newValue == null || newValue.length() == 0) {
                    for (Toggle tg : Controller.this.channelGroup.getToggles()) {
                        ((RadioButton) tg).setDisable(false);
                        ;
                    }
                } else {
                    for (Toggle tg : Controller.this.channelGroup.getToggles()) {
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
                ObservableList<TableColumn<FileObject,?>> sortOrder = tableView.getSortOrder();
                sortOrder.removeAll();
                sortOrder.addAll(processedColumn, lastmodifiedColumn);
                tableView.sort();
                tableView.getSelectionModel().select(0);
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

    public void createChannels(String csvFile) {
        BufferedReader br = null;
        String line = "";
        String csvSplitBy = ",";
        StringBuilder strBuilder = new StringBuilder();

        try {

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                String[] channel = line.split(csvSplitBy);
                addChannelButton(channel[0], channel[1], channel[2], Integer.parseInt(channel[3]), Integer.parseInt(channel[4]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
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
        //final Calendar startDateCalendar = startDatePicker.getCalendar();
        if (startDatePicker.getValue() == null) {
            error.setText("No Start Date Set.");
            return;
        }
        final LocalDate localStartDate = startDatePicker.getValue();
        final Date startDate = new Date(localStartDate.getYear(), localStartDate.getMonthValue(),localStartDate.getDayOfMonth());
        //final Date startDate = startDateCalendar.getTime();
        //final Calendar startDatePickerCalendar= startDatePicker.getCalendar();
        if (startTimeField.getText().isEmpty()) {
            error.setText("No Start Time Set.");
            return;
        }
        final String[] startTimeStr = startTimeField.getText().split(":");
        startDate.setHours(Integer.parseInt(startTimeStr[0]));
        startDate.setMinutes(Integer.parseInt(startTimeStr[1]));
        thisRow.setStartDate(startDate);
        final Date endDate;
        if (endDatePicker.getValue() != null && !endDatePicker.getValue().toString().isEmpty()) {
            endDate = new Date(endDatePicker.getValue().toEpochDay()*1000);
        }
        else
        {
            error.setText("No End Date Set.");
            return;
        }

        final String[] endTime;
        if (endTimeField.getText() != null && !endTimeField.getText().isEmpty()) {
            endTime = endTimeField.getText().split(":");
        }
        else
        {
            error.setText("No End Time Set.");
            return;
        }
        endDate.setHours(Integer.parseInt(endTime[0]));
        endDate.setMinutes(Integer.parseInt(endTime[1]));
        thisRow.setEndDate(endDate);
        final Toggle selectedToggle = channelGroup.getSelectedToggle();
        String altChannel = this.altChannel.getText();
        if (altChannel != null && altChannel.length() > 0 ) {
            String channelPattern = "^[a-z0-9]{3,}$";
            if (Pattern.matches(channelPattern,altChannel)) {
                thisRow.setChannel(altChannel);
            }
            else {
                error.setText("Channel not valid");
                return;
            }

        } else {
            String channel = null;
            if ( selectedToggle  != null ) {
                channel = ((Channel) ((RadioButton) selectedToggle).getUserData()).getChannelName();
            }
            thisRow.setChannel(channel);
        }
        if (thisRow.getChannel() == null) {
            error.setText("No channel specified.");
            return;
        }
        thisRow.setQuality(cmbQuality.getValue().toString());
        thisRow.setVhsLabel(txtVhsLabel.getText());
        thisRow.setComment(txtComment.getText());
        error.setText(null);
        thisRow.commit();
        detailVHS.setVisible(false);
        //loadFile(thisRow);
        //tableView.getSelectionModel().select(thisRow);
    }

    public class FileclickMouseEventHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                FileObjectImpl thisRow = (FileObjectImpl) ((TableView) mouseEvent.getSource()).getSelectionModel().getSelectedItem();
                loadFile(thisRow);
                detailVHS.setVisible(true);
            }
            else if (mouseEvent.getButton() == MouseButton.MIDDLE.SECONDARY) {
                FileObjectImpl thisRow = (FileObjectImpl) tableView.getSelectionModel().getSelectedItem();
                try {
                    ProcessBuilder  pb = new ProcessBuilder(Main.player,Main.recordsDir+"/"+thisRow.getFilename()); //" C:\\Test\\test.mp4");
                    Process start = pb.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadFile(FileObjectImpl thisRow) {
        error.setText(null);
        if (thisRow != null) {
            Controller.this.txtFilename.setText(thisRow.getFilename());
            GregorianCalendar startCalendar = new GregorianCalendar();
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            if (thisRow.getStartDate() != null) {
                startCalendar.setTime(thisRow.getStartDate());
                Controller.this.startDatePicker.setValue(thisRow.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                Controller.this.startTimeField.setText(timeFormat.format(startCalendar.getTime()));
            } else {
                Controller.this.startDatePicker.setValue(null);
                Controller.this.startTimeField.setText("");
            }
            GregorianCalendar endCalendar = new GregorianCalendar();
            if (thisRow.getEndDate() != null) {
                endCalendar.setTime(thisRow.getEndDate());
                Controller.this.endDatePicker.setValue(thisRow.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                Controller.this.endTimeField.setText(timeFormat.format(endCalendar.getTime()));
            } else {
                Controller.this.endDatePicker.setValue(null);
                Controller.this.endTimeField.setText("");
            }
            final String quality = thisRow.getQuality();
            if (quality != null) {
                Controller.this.cmbQuality.getSelectionModel().select(quality);
            }
            Controller.this.txtVhsLabel.setText(thisRow.getVhsLabel());
            Controller.this.txtComment.setText(thisRow.getComment());
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
