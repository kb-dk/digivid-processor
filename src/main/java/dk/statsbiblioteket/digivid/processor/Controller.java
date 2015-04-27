package dk.statsbiblioteket.digivid.processor;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

public class Controller {

    private Path dataPath;
    private TextField altChannel;

    private static final String hourPattern =  "([01]?[0-9]|2[0-3]):[0-5][0-9]";
    private static final String channelPattern = "^[a-z0-9]{3,}$";

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
    public javafx.scene.control.ComboBox<String> cmbQuality;
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
    @FXML
    public GridPane channelGridPane;
    @FXML
    public javafx.scene.layout.AnchorPane detailVHS;

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
        createChannels(DigividProcessor.channelCSV);
        altChannel = new TextField();
        altChannel.setId("altChannel");
        altChannel.setPrefWidth(150.0);
        channelGridPane.getChildren().add(altChannel);
        GridPane.setRowIndex(altChannel, 4);
        GridPane.setColumnIndex(altChannel, 0);

        //Every time a date is put in the startdate picker the enddate picker is put to the same value.
        startDatePicker.setOnAction(event -> {
            endDatePicker.setValue(startDatePicker.getValue());
        });

        altChannel.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                if (newValue == null || newValue.length() == 0) {
                    for (Toggle tg : Controller.this.channelGroup.getToggles()) {
                        ((RadioButton) tg).setDisable(false);
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
                        assert tsFiles != null;
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

        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                String[] channel = line.split(csvSplitBy);
                addChannelButton(channel[0], channel[1], channel[2], Integer.parseInt(channel[3]), Integer.parseInt(channel[4]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Input error: " + e.getMessage());
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
        if (startDatePicker.getValue() == null) {
            error.setText("No Start Date Set.");
            return;
        }
        if (startTimeField.getText().isEmpty()) {
            error.setText("No Start Time Set.");
            return;
        }
        else if (!Pattern.matches(hourPattern,startTimeField.getText())){
            error.setText("Start time not valid");
            return;
        }
        String[] timeStr = startTimeField.getText().split(":");
        LocalDate localDate = startDatePicker.getValue();
        Instant instant = localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
//        final Date startDate = Date.from(instant);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Date.from(instant));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(timeStr[1]));
//        startDate.setHours(Integer.parseInt(timeStr[0]));
//        startDate.setMinutes(Integer.parseInt(timeStr[1]));
        thisRow.setStartDate(calendar.getTime());

        if (endDatePicker.getValue() == null && !endDatePicker.getValue().toString().isEmpty()) {
            error.setText("No End Date Set.");
            return;
        }


        if (endTimeField.getText().isEmpty()) {
            error.setText("No End Time Set.");
            return;
        }
        else if (!Pattern.matches(hourPattern,endTimeField.getText())){
            error.setText("End time not valid");
            return;
        }
        timeStr = endTimeField.getText().split(":");
        localDate = endDatePicker.getValue();
        instant = localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
        calendar = Calendar.getInstance();
        calendar.setTime(Date.from(instant));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(timeStr[1]));
//        endDate.setHours(Integer.parseInt(timeStr[0]));
//        endDate.setMinutes(Integer.parseInt(timeStr[1]));
        thisRow.setEndDate(calendar.getTime());

        final Toggle selectedToggle = channelGroup.getSelectedToggle();
        String altChannel = this.altChannel.getText();
        if (altChannel != null && altChannel.length() > 0 ) {

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
                channel = ((Channel) selectedToggle.getUserData()).getChannelName();
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
    }

    public class FileclickMouseEventHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                FileObjectImpl thisRow = (FileObjectImpl) ((TableView) mouseEvent.getSource()).getSelectionModel().getSelectedItem();
                loadFile(thisRow);
                detailVHS.setVisible(true);
            }
            else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                FileObjectImpl thisRow = (FileObjectImpl) tableView.getSelectionModel().getSelectedItem();
                try {
                    ProcessBuilder  pb = new ProcessBuilder(DigividProcessor.player,DigividProcessor.recordsDir+"/"+thisRow.getFilename()); //" C:\\Test\\test.mp4");
                    pb.start();
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
