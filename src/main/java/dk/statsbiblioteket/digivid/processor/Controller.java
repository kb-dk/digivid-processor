package dk.statsbiblioteket.digivid.processor;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;
import jfxtras.scene.control.LocalDateTimeTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Shows GUI and requests videofiles to be renamed and a JSON-file to be generated according to the GUI-users choices.
 */
public class Controller {

    public static final String CHECKMARK = Character.toString((char) 10003);
    private static final String dateTimePattern = "yy-MM-dd HH:mm";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimePattern).withZone(ZoneId.systemDefault());
    @FXML public TableView<VideoFileObject> tableView;
    @FXML public TableColumn<VideoFileObject, Instant> lastmodifiedColumn;
    @FXML public TableColumn<VideoFileObject, Boolean> processedColumn;
    @FXML
    public TableColumn<VideoFileObject, String> filenameColumn;
    @FXML
    public TableColumn<VideoFileObject, Long> filesizeColumn;
    @FXML public GridPane channelGridPane;
    @FXML public TextArea txtComment;
    @FXML public ComboBox<String> cmbQuality;
    @FXML
    public TextField txtFilename;
    @FXML public TextField txtVhsLabel;
    @FXML public TextField txtManufacturer;
    @FXML public TextField txtModel;
    @FXML public TextField txtSerial;
    @FXML
    public TextField txtProcessedManufacturer;
    @FXML
    public TextField txtProcessedModel;
    @FXML
    public TextField txtProcessedSerial;
    @FXML public LocalDateTimeTextField startDatePicker;
    @FXML public LocalDateTimeTextField endDatePicker;
    @FXML public ToggleGroup channelGroup;
    @FXML public javafx.scene.layout.AnchorPane detailVHS;
    private Logger log = LoggerFactory.getLogger(getClass());
    private Path dataPath;
    private TextField altChannel;
    private VideoFileObject thisVideoFileRow;

    private static void exists(Path recordsPath) throws FileNotFoundException {
        if (!Files.exists(recordsPath)) {
            throw new FileNotFoundException("File " + recordsPath + " is not found");
        }
    }

    @FXML
    public void handleLocalProperties() throws IOException {

        Path parentDir = DigividProcessor.localProperties.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        String msg = String.format("%s,%s,%s", txtManufacturer.getText(), txtModel.getText(), txtSerial.getText());
        Files.write(DigividProcessor.localProperties, msg.getBytes("UTF-8"), StandardOpenOption.WRITE, StandardOpenOption.CREATE);

    }

    @FXML
    void initialize() throws IOException {
        detailVHS.setVisible(false);
        try {
            exists(DigividProcessor.recordsDir);
            exists(DigividProcessor.channelCSV);
            exists(DigividProcessor.player);

            setupChannelButtons();
        } catch (Exception e) {
            Utils.errorDialog("Report the following error to the administratior: ", e);
        }

        Callback<Throwable, Void> parseErrorDialogs = param -> {
            //callback that displays warnings if the datepickers cannot parse the user input
            Utils.warningDialog(
                    "Attempt to parse date according the the format '" + dateTimePattern + "' failed with:\n" +
                    param.getMessage() + "\nResetting value", param);
            return null;
        };

        startDatePicker.setDateTimeFormatter(dtf);
        startDatePicker.withLocale(Locale.GERMAN); //This makes it use 24h days.
        startDatePicker.setParseErrorCallback(parseErrorDialogs);

        endDatePicker.setDateTimeFormatter(dtf);
        endDatePicker.withLocale(Locale.GERMAN);
        endDatePicker.setParseErrorCallback(parseErrorDialogs);

        // expand the textfield dynamically
        txtFilename.textProperty().addListener((ObservableValue<? extends String> observableValue, String oldValue, String newValue) -> txtFilename.setPrefWidth(Utils.computeTextWidth(txtFilename.getFont(), newValue, 0.0D) + 20));

        readLocalProperties();

        enableAutoSaver(DigividProcessor.autoSaveInterval);

    }

    private void enableAutoSaver(int saveInterval) {
        Timer timer = new Timer(true); //True for daemon process
        TimerTask saveDirtyFiles = new TimerTask() {

            @Override
            public void run() {
                if (thisVideoFileRow != null){
                    thisVideoFileRow.preprocess();
                }
            }
        };

        //This is fixed delay
        timer.schedule(saveDirtyFiles, saveInterval, saveInterval);
    }

    private void setupChannelButtons() throws IOException {
            List<List<String>> channels = Utils.getCSV(DigividProcessor.channelCSV);
            for (List<String> channel : channels) {
                if (channel.size() > 5) {
                    String channelName = channel.get(0);
                    String displayName = channel.get(1);
                    String colour = channel.get(2);
                    int rowIndex = Integer.parseInt(channel.get(3));
                    int columnIndex = Integer.parseInt(channel.get(4));
                    String type = channel.get(5);

                    Control thing = null;
                    if (type.equals("Radiobutton")) {
                        RadioButton rb1 = new RadioButton();
                        thing = rb1;
                        rb1.setText(displayName);
                        rb1.setUserData(new Channel(channelName, displayName, colour));
                        rb1.setStyle("-fx-background-color:" + colour);
                        rb1.setToggleGroup(channelGroup);
                    } else if (type.equals("TextField")) {
                        if (altChannel != null){
                            Utils.errorDialog("Error setting up channel buttons. More than one channel of type TextField",null);
                        }
                        altChannel = new TextField();
                        thing = altChannel;
                        altChannel.setId("altChannel");
                    }
                    if (thing != null) { //stuff common for the button and textfield
                        thing.setPrefWidth(150.0);
                        channelGridPane.getChildren().add(thing);
                        GridPane.setRowIndex(thing, rowIndex);
                        GridPane.setColumnIndex(thing, columnIndex);
                    }

                }
            }

        //Bind changes to channelGroup to update the altChannel field
        for (Toggle toggle : channelGroup.getToggles()) {
            toggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    altChannel.setText(((Channel) toggle.getUserData()).getChannelName());
                }
            });
        }
        //Bind changes to altChannel to select correct button
        altChannel.textProperty().addListener((observable, oldChannelName, newChannelName) -> {
            for (Node channelNode : channelGridPane.getChildren()) {
                if (channelNode instanceof RadioButton) {
                    Channel buttonChannel = (Channel) channelNode.getUserData();
                    if (buttonChannel.getChannelName().equals(newChannelName)) {
                        ((RadioButton) channelNode).setSelected(true);
                    } else {
                        ((RadioButton) channelNode).setSelected(false);
                    }
                }
            }

        });
    }


    protected Path getDataPath() {
        return dataPath;
    }

    protected void setDataPath(Path dataPath) {
        this.dataPath = dataPath;
    }

    public void setupTableView() {
        /**
         * Custom rendering of the table cell to have format specified "yyyy-mm-dd HH:mm.
         */
        lastmodifiedColumn.setCellFactory(column -> {
            return new TableCell<VideoFileObject, Instant>() {
                @Override
                protected void updateItem(Instant date, boolean empty) {
                    super.updateItem(date, empty);
                    setAlignment(Pos.CENTER);

                    if (date == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(dtf.format(date));
                    }
                }
            };
        });
        lastmodifiedColumn.setComparator(Instant::compareTo);



        /**
         * The filename is colored blue when it is not yet processed
         */
        filenameColumn.setCellFactory((TableColumn<VideoFileObject, String> column) -> new TableCell<VideoFileObject, String>() {

            @Override
            protected void updateItem(String fileName, boolean empty) {
                super.updateItem(fileName, empty);
                if (fileName == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(fileName);
                    VideoFileObject item = (VideoFileObject) getTableRow().getItem();
                    if (item == null){ return; }
                    if (item.isProcessed()) {
                        setTextFill(Color.GREEN);
                    } else {
                        setTextFill(Color.BLUE);
                    }
                }
            }
        });


        /**
         * Indicate with a checkmark if the file is processed
         */
        processedColumn.setCellFactory(column -> new TableCell<VideoFileObject, Boolean>() {
            @Override
            protected void updateItem(Boolean processed, boolean empty) {
                super.updateItem(processed, empty);
                if (processed == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setAlignment(Pos.CENTER);
                    if (processed) {
                        setText(CHECKMARK);
                    } else {
                        setText("");
                    }
                }
            }
        });

        filesizeColumn.setCellFactory(column -> new TableCell<VideoFileObject, Long>() {
            @Override
            protected void updateItem(Long filesize, boolean empty) {
                super.updateItem(filesize, empty);
                if (filesize == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    setText(String.format("%d", filesize));
                }
            }
        });

        ObservableList<TableColumn<VideoFileObject, ?>> sortOrder = tableView.getSortOrder();
        sortOrder.clear();
        sortOrder.addAll(processedColumn, lastmodifiedColumn);

        tableView.itemsProperty().addListener(
                (observable, oldValue, newValue) -> {
                    //Tableview refreshes when files are added or removed
                    tableView.refresh();
                });


        //This listener causes the table to be refreshed whenever the processed value change.
        //This is nessesary only because the colours will not update otherwise
        ChangeListener<Boolean> processedTableRefresher = (observable1, oldValue, newValue) -> {
            if (oldValue != newValue) {
                tableView.refresh();
            }
        };

        tableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldFile, newFile) -> {
                    if (oldFile != null) {
                        //Unbind the old propertes

                        txtVhsLabel.textProperty().unbindBidirectional(oldFile.vhsLabelProperty());
                        txtComment.textProperty().unbindBidirectional(oldFile.commentProperty());
                        txtFilename.textProperty().unbindBidirectional(oldFile.filenameProperty());
                        txtProcessedManufacturer.textProperty().unbindBidirectional(oldFile.manufacturerProperty());
                        txtProcessedModel.textProperty().unbindBidirectional(oldFile.modelProperty());
                        txtProcessedSerial.textProperty().unbindBidirectional(oldFile.serialNoProperty());
                        altChannel.textProperty().unbindBidirectional(oldFile.channelProperty());

                        //This one has to happen in inverse order
                        oldFile.qualityProperty().unbindBidirectional(cmbQuality.valueProperty());

                        Bindings.unbindBidirectional(startDatePicker.textProperty(), oldFile.startDateProperty());
                        Bindings.unbindBidirectional(endDatePicker.textProperty(), oldFile.endDateProperty());

                        oldFile.processedProperty().removeListener(processedTableRefresher);
                        //save the old values
                        oldFile.preprocess();
                    }
                    if (newFile != null) {
                        //load the newly selected file
                        thisVideoFileRow = newFile;
                        detailVHS.setVisible(true);

                        //ChannelButtons are slaved to altChannel, so it is enough to link this
                        altChannel.textProperty().bindBidirectional(newFile.channelProperty());

                        //bind it's properties
                        txtVhsLabel.textProperty().bindBidirectional(newFile.vhsLabelProperty());
                        txtComment.textProperty().bindBidirectional(newFile.commentProperty());
                        txtFilename.textProperty().bindBidirectional(newFile.filenameProperty());
                        txtProcessedManufacturer.textProperty().bindBidirectional(newFile.manufacturerProperty());
                        txtProcessedModel.textProperty().bindBidirectional(newFile.modelProperty());
                        txtProcessedSerial.textProperty().bindBidirectional(newFile.serialNoProperty());

                        if (newFile.getQuality() != null) {//Set value from file, if any
                            cmbQuality.setValue(newFile.getQuality());
                        }
                        //Then bind so that cmbQuality value wins over file value
                        //This way, the default value from cmbQuality is only overwritten if explicit value is set
                        newFile.qualityProperty().bindBidirectional(cmbQuality.valueProperty());

                        //Set the manufacturer, serial and model from default values, if not set already
                        if (txtProcessedManufacturer.textProperty().getValue() == null)
                            txtProcessedManufacturer.textProperty().setValue(txtManufacturer.textProperty().getValue());
                        if (txtProcessedModel.textProperty().getValue() == null)
                            txtProcessedModel.textProperty().setValue(txtModel.textProperty().getValue());
                        if (txtProcessedSerial.textProperty().getValue() == null)
                            txtProcessedSerial.textProperty().setValue(txtSerial.textProperty().getValue());


                        datePickerBindBidirectional(newFile.startDateProperty(), startDatePicker);
                        datePickerBindBidirectional(newFile.endDateProperty(), endDatePicker);

                        newFile.processedProperty().addListener(processedTableRefresher);

                        //Tableview refreshes when changing selection
                        //tableView.refresh();
                    }
                });
        tableView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                playCurrentFile();
            }
        });
    }

    private void datePickerBindBidirectional(VideoFileObject.MonitoredProperty<Long> dateProp, LocalDateTimeTextField picker) {

        //for the DatePickers, the textProperty is subservient to the localDateTime property.
        //So, we set the local date time property to what is in the file
        //And then we hook the text property, which is automatically updated when the localDateTime
        //property is updated, to reflect back into the file

        Long date = dateProp.getValue();
        if (date != null) {
            picker.setLocalDateTime(
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()));
        }
        Bindings.bindBidirectional(picker.textProperty(),
                                   dateProp,
                                   new StringConverter<Long>() {
                                       @Override
                                       public String toString(Long object) {
                                           if (object == null) {
                                               return "";
                                           }
                                           return dtf.format(Instant.ofEpochMilli(object));
                                       }

                                       @Override
                                       public Long fromString(String string) {
                                           if (string == null || string.isEmpty()) {
                                               return null;
                                           }
                                           return Instant.from(dtf.parse(string)).toEpochMilli();
                                       }
                                   });
    }

    public void setupFolderWatcher() {

        Runnable folderWatcherRunnable = () -> {
            try (WatchService service = getDataPath().getFileSystem().newWatchService()) {

                getDataPath().register(service,
                                       StandardWatchEventKinds.ENTRY_MODIFY,
                                       StandardWatchEventKinds.ENTRY_CREATE,
                                       StandardWatchEventKinds.ENTRY_DELETE);

                while (true) {
                    try {
                        WatchKey key = service.take();
                        ObservableList<VideoFileObject> items = tableView.getItems();
                        for (WatchEvent<?> watchEvent : key.pollEvents()) {
                            if (watchEvent.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                                Path modifiedFile = getDataPath().resolve((Path) watchEvent.context());
                                String filename = modifiedFile.getFileName().toString();
                                if (filename.endsWith(".ts")) {
                                    for (VideoFileObject videoFileObject : items) {
                                        if (videoFileObject.getFilename().equals(filename)) {
                                            videoFileObject.setFilesize(Files.size(modifiedFile));
                                            videoFileObject.setLastModified(
                                                    Files.getLastModifiedTime(modifiedFile).toInstant());
                                            break;
                                        }
                                    }
                                }
                            }
                            if (watchEvent.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                                Path createdFile = getDataPath().resolve((Path) watchEvent.context());
                                String filename = createdFile.getFileName().toString();

                                if (filename.endsWith(".ts")) {
                                    VideoFileObject videoFileObject = VideoFileObject.create(createdFile);
                                    if (!items.contains(videoFileObject)) { //not there, just add
                                        items.add(videoFileObject);
                                        tableView.sort();
                                    } else { //update instead
                                        for (VideoFileObject item : items) {
                                            if (item.getFilename().equals(filename)) {
                                                item.setFilesize(Files.size(createdFile));
                                                item.setLastModified(
                                                        Files.getLastModifiedTime(createdFile).toInstant());
                                            }
                                        }
                                    }
                                }
                            }
                            if (watchEvent.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                                Path deletedFile = getDataPath().resolve((Path) watchEvent.context());
                                String filename = deletedFile.getFileName().toString();
                                if (filename.endsWith(".ts")) {
                                    for (int i = 0; i < items.size(); i++) {
                                        VideoFileObject videoFileObject = items.get(i);
                                        if (videoFileObject.getFilename().equals(
                                                filename)) {
                                            items.remove(i);
                                            Path vhsFileMetadataFilePath = videoFileObject.getVhsFileMetadataFilePath();
                                            Files.deleteIfExists(vhsFileMetadataFilePath);
                                            Path tmpFileMetadataFilePath = videoFileObject.getTmpFileMetadataFilePath();
                                            Files.deleteIfExists(tmpFileMetadataFilePath);
                                        }
                                    }
                                }
                            }
                        }
                        boolean valid = key.reset();
                        if (!valid) {
                            throw new RuntimeException(
                                    "Folder watcher for " + getDataPath() + " failed.\nThe key is no longer valid");
                        }
                    } catch (InterruptedException | IOException e) {
                        throw new RuntimeException("Folder watcher for " + getDataPath() + " failed.", e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to set up folder watcher for " + getDataPath(), e);
            }
        };

        Thread newThread = new Thread(folderWatcherRunnable);
        newThread.setDaemon(true);

        newThread.setUncaughtExceptionHandler( //This catches all the RuntimeExceptions from above, so the user is warned
                (Thread t, Throwable e) -> {
                    Utils.errorDialog("Exception in thread " + t.getName(), e);
                });
        newThread.setName("Folder Watcher Thread");

        newThread.start();
    }

    /**
     * Reads information from the meatadata.csv file and put it in the fields for Manufacturer, Model and Serialnumber
     */
    private void readLocalProperties() throws IOException {
        Path newFilePath = DigividProcessor.localProperties;
        if (Files.exists(newFilePath)) {
            List<String> lines = Files.readAllLines(DigividProcessor.localProperties, Charset.defaultCharset());
            String metadataLine = lines.get(0) + " ";
            List<String> localProperties = Arrays.asList(metadataLine.split(","));
            txtManufacturer.setText(localProperties.get(0));
            txtModel.setText(localProperties.get(1));
            txtSerial.setText(localProperties.get(2).trim());
        } else {
            Path parentDir = newFilePath.getParent();
            if (!Files.exists(parentDir))
                Files.createDirectories(parentDir);
            String msg = ",,";
            Files.write(newFilePath, msg.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }

    }

    /**
     * The tableview displays an overview of ts-files
     */
    protected void loadFilenames() throws IOException {
        ObservableList<VideoFileObject> videoFileObjects = FXCollections.observableList(new ArrayList<>());

        //Initial setup of files in table
        try (DirectoryStream<Path> tsFiles = Files.newDirectoryStream(getDataPath(), "*.ts")) {
            for (Path tsFile : tsFiles) {
                if (!tsFile.getFileName().startsWith("temp"))
                    videoFileObjects.add(VideoFileObject.create(tsFile));
            }
            tableView.setItems(videoFileObjects);
        }

        tableView.sort();
        SortedList<VideoFileObject> sortedVideoFileList = new SortedList<>(videoFileObjects);
        Object[] fileObjectAsArray = sortedVideoFileList.toArray();
        int currentIndexInGrid = -1;
        if (thisVideoFileRow != null) {
            VideoFileObject loopVideoFileObject;
            for (int i = 0; i < sortedVideoFileList.size(); i++) {
                loopVideoFileObject = ((VideoFileObject) fileObjectAsArray[i]);
                if (thisVideoFileRow.equals(loopVideoFileObject))
                    currentIndexInGrid = i;
            }
            tableView.getSelectionModel().select(currentIndexInGrid);
        } else {
            tableView.getSelectionModel().select(0);
        }
    }

    /**
     * Assigne current VideoFileObject values to GUI values before calling commit()
     *
     * @param actionEvent The event that activated commit
     */
    public void commit(ActionEvent actionEvent) {
        if (!isFileInUse()) {
            if (validateValues(thisVideoFileRow)) {
                thisVideoFileRow.commit();
            }
        }
    }

    private boolean validateValues(VideoFileObject thisVideoFileRow) {

        String manufacturer = thisVideoFileRow.getManufacturer();
        if (manufacturer == null || (manufacturer.trim().isEmpty())) {
            Utils.warningDialog("Manufacturer is not allowed to be empty");
            return false;
        }
        String model = thisVideoFileRow.getModel();
        if (model == null || (model.trim().isEmpty())) {
            Utils.warningDialog("Model field is not allowed to be empty");
            return false;
        }

        String serial = thisVideoFileRow.getSerialNo();
        if (serial == null || (serial.trim().isEmpty())) {
            Utils.warningDialog("Serial number is not allowed to be empty");
            return false;
        }

        String label = thisVideoFileRow.getVhsLabel();
        if (label == null || (label.trim().isEmpty())) {
            Utils.warningDialog("Video label is not allowed to be empty");
            return false;
        }

        String channel = thisVideoFileRow.getChannel();
        if (channel == null || (channel.trim().isEmpty())) {
            Utils.warningDialog("A channel has to be specified");
            return false;
        }

        Long startDate = thisVideoFileRow.getStartDate();
        if (startDate == null || startDate == 0L) {
            Utils.warningDialog("Start date is not allowed to be empty");
            return false;
        }

        Long endDate = thisVideoFileRow.getEndDate();
        if (endDate == null || endDate == 0L) {
            Utils.warningDialog("End date is not allowed to be empty");
            return false;
        }

        if (startDate >= endDate) {
            Utils.warningDialog("Negative Duration: \n " +
                                "Start date: '" + startDatePicker.getText() + "'\n" +
                                "End date:   '" + endDatePicker.getText() + "'");
            return false;

        }

        return true;
    }

    private boolean isFileInUse() {
        File file = thisVideoFileRow.getVideoFilePath().toFile();
        boolean couldRename = file.renameTo(file);
        if (!couldRename) {
            Utils.warningDialog("The file '"+file+"' is currently locked by another program and cannot be committed.");
            return true;
        }
        return false;
    }

    /**
     * Show the video file in the player
     */
    public void playCurrentFile() {
        try {
            ProcessBuilder pb = new ProcessBuilder(DigividProcessor.player.toFile().getAbsolutePath(),
                    DigividProcessor.recordsDir.resolve(thisVideoFileRow.getFilename()).toFile().getAbsolutePath());
            pb.start();
        } catch (IOException e) {
            Utils.warningDialog("The file '"+thisVideoFileRow.getFilename()+"' could not be played", e);
        }
    }

}
