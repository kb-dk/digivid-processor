package dk.statsbiblioteket.digivid.processor;

import javafx.beans.binding.Bindings;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Shows GUI and requests videofiles to be renamed and a JSON-file to be generated according to the GUI-users choices.
 */
public class Controller {

    public static final String CHECKMARK = Character.toString((char) 10003);
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private static Logger log = LoggerFactory.getLogger(Controller.class);
    @FXML public TableView<VideoFileObject> tableView;
    @FXML public GridPane channelGridPane;
    @FXML public TableColumn<VideoFileObject, Instant> lastmodifiedColumn;
    @FXML public TableColumn<VideoFileObject, Boolean> processedColumn;
    @FXML
    public TableColumn<VideoFileObject, String> filenameColumn;
    @FXML
    public TableColumn<VideoFileObject, Long> filesizeColumn;
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

    private Path dataPath;
    private TextField altChannel;
    private VideoFileObject thisVideoFileRow;

    private static void checkConfigfile() throws FileNotFoundException {
        Path recordsPath = Paths.get(DigividProcessor.recordsDir);
        Path channelsCSVPath = Paths.get(DigividProcessor.channelCSV);
        Path playerPath = Paths.get(DigividProcessor.player);
        Path localPropertiesPath = Paths.get(DigividProcessor.localProperties);
        exists(recordsPath);
        exists(channelsCSVPath);
        exists(playerPath);
    }

    private static void exists(Path recordsPath) throws FileNotFoundException {
        if (!Files.exists(recordsPath)){
            throw new FileNotFoundException("File "+recordsPath + " is not found");
        }
    }

    @FXML
    public void handleLocalProperties() {
        writeLocalProperties();
    }

    @FXML
    void initialize() throws FileNotFoundException {
        detailVHS.setVisible(false);
        checkConfigfile();

        try {
            List<List<String>> channels = Utils.getCSV(DigividProcessor.channelCSV);
            for (List<String> channel : channels) {
                if (channel.size() > 4) {
                    if (channel.get(5).equals("Radiobutton")) {
                        addChannelButton(channel.get(0), channel.get(1), channel.get(2), Integer.parseInt(channel.get(3)),
                                Integer.parseInt(channel.get(4)));
                    } else if (channel.get(5).equals("TextField"))
                        addChannelTextfield();
                }
            }
        } catch (IOException e) {
            log.error("Caught exception while reading {}", DigividProcessor.channelCSV, e);
            Utils.showErrorDialog("Caught exception while reading channels\n\n", Thread.currentThread(), e);
        }

        //Bind changes to channelGroup to update the altChannel field
        for (Toggle toggle : channelGroup.getToggles()) {
            toggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
               if (newValue){
                   altChannel.setText(((Channel) toggle.getUserData()).getChannelName());
               }
            });
        }

        startDatePicker.setDateTimeFormatter(dtf);
        startDatePicker.withLocale(Locale.GERMAN); //This makes it use 24h days.

        endDatePicker.setDateTimeFormatter(dtf);
        endDatePicker.withLocale(Locale.GERMAN);

        txtFilename.textProperty().addListener((ObservableValue<? extends String> observableValue, String oldValue, String newValue) ->
        {
            // expand the textfield
            txtFilename.setPrefWidth(Utils.computeTextWidth(txtFilename.getFont(),newValue, 0.0D) + 20);
        });

        readLocalProperties();
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
        filenameColumn.setCellFactory(column -> new TableCell<VideoFileObject, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        VideoFileObject videoFile = VideoFileObject.createFromTS(
                                Paths.get(DigividProcessor.recordsDir, item));
                        if (videoFile.isProcessed())
                            setTextFill(Color.GREEN);
                        else
                            setTextFill(Color.BLUE);

                    } catch (IOException e) {
                        log.error("Failed to figure out processing state of file "+item,e);
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
                        Bindings.unbindBidirectional(endDatePicker.textProperty(),oldFile.endDateProperty());

                        //save the old values
                        oldFile.preprocess();
                    }
                    if (newFile != null) {
                        //load the newly selected file
                        thisVideoFileRow = newFile;
                        detailVHS.setVisible(true);

                        //Initialise the channel Fields when a new file is loaded
                        String currentChannel = newFile.getChannel();
                        for (Node channelNode : channelGridPane.getChildren()) {
                            if (channelNode instanceof RadioButton) {
                                Channel buttonChannel = (Channel) channelNode.getUserData();
                                if (buttonChannel.getChannelName().equals(currentChannel)) {
                                    ((RadioButton) channelNode).setSelected(true);
                                } else {
                                    ((RadioButton) channelNode).setSelected(false);
                                }
                            }
                        }
                        altChannel.textProperty().bindBidirectional(newFile.channelProperty());

                        //bind it's properties
                        txtVhsLabel.textProperty().bindBidirectional(newFile.vhsLabelProperty());
                        txtComment.textProperty().bindBidirectional(newFile.commentProperty());
                        txtFilename.textProperty().bindBidirectional(newFile.filenameProperty());
                        txtProcessedManufacturer.textProperty().bindBidirectional(newFile.manufacturerProperty());
                        txtProcessedModel.textProperty().bindBidirectional(newFile.modelProperty());
                        txtProcessedSerial.textProperty().bindBidirectional(newFile.serialNoProperty());
                        if (txtProcessedManufacturer.textProperty().getValue() == null)
                            txtProcessedManufacturer.textProperty().setValue(txtManufacturer.textProperty().getValue());
                        if (txtProcessedModel.textProperty().getValue() == null)
                            txtProcessedModel.textProperty().setValue(txtModel.textProperty().getValue());
                        if (txtProcessedSerial.textProperty().getValue() == null)
                            txtProcessedSerial.textProperty().setValue(txtSerial.textProperty().getValue());

                        //This one has to happen in inverse order
                        newFile.qualityProperty().bindBidirectional(cmbQuality.valueProperty());

                        Bindings.bindBidirectional(startDatePicker.textProperty(),
                                                   newFile.startDateProperty(),
                                                   getConverter());

                        Bindings.bindBidirectional(endDatePicker.textProperty(),
                                                   newFile.endDateProperty(),
                                                   getConverter());
                    }
                });
        tableView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                playCurrentFile();
            }
        });
    }

    public void setupFolderWatcher() {
        try {
            WatchService service = getDataPath().getFileSystem().newWatchService();
            getDataPath().register(service,
                                   StandardWatchEventKinds.ENTRY_MODIFY,
                                   StandardWatchEventKinds.ENTRY_CREATE,
                                   StandardWatchEventKinds.ENTRY_DELETE);
            new Thread() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            WatchKey key = service.take();
                            ObservableList<VideoFileObject> items = tableView.getItems();
                            for (WatchEvent<?> watchEvent : key.pollEvents()) {
                                if (watchEvent.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY)){
                                    Path modifiedFile = getDataPath().resolve((Path) watchEvent.context());
                                    String filename = modifiedFile.getFileName().toString();
                                    if (filename.endsWith(".ts")){
                                        for (VideoFileObject videoFileObject : items) {
                                            if (videoFileObject.getFilename().equals(filename)) {
                                                videoFileObject.setFilesize(Files.size(modifiedFile));
                                                videoFileObject.setLastModified(Files.getLastModifiedTime(modifiedFile).toInstant());
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (watchEvent.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)){
                                    Path createdFile = getDataPath().resolve((Path) watchEvent.context());
                                    String filename = createdFile.getFileName().toString();

                                    if (filename.endsWith(".ts")) {
                                        VideoFileObject videoFileObject = VideoFileObject.createFromTS(createdFile);
                                        if (!items.contains(videoFileObject)) {
                                            items.add(videoFileObject);
                                            tableView.sort();
                                        }
                                    }
                                }
                                if (watchEvent.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)){
                                    Path deletedFile = getDataPath().resolve((Path) watchEvent.context());
                                    String filename = deletedFile.getFileName().toString();
                                    if (filename.endsWith(".ts")) {
                                        for (int i = 0; i < items.size(); i++) {
                                            VideoFileObject videoFileObject = items.get(i);
                                            if (videoFileObject.getFilename().equals(
                                                    filename)) {
                                                items.remove(i);
                                                Files.deleteIfExists(videoFileObject.getVhsFileMetadataFilePath());
                                            }
                                        }
                                    }
                                }
                            }
                            boolean valid = key.reset();
                            if (!valid) {
                                break;
                            }
                        } catch (InterruptedException | IOException e) {
                            log.error("Thread error in setDataPath: " + e.getMessage(), e);
                            Utils.showErrorDialog(Thread.currentThread(), e);
                        }
                    }
                }
            }.start();

        } catch (IOException e) {
            log.error("Error in setDataPath: " + e.getMessage(), e);
            Utils.showErrorDialog(Thread.currentThread(), e);
        }
    }

    private StringConverter<Long> getConverter() {
        return new StringConverter<Long>() {
            @Override
            public String toString(Long object) {
                if (object == null){
                    return "";
                }
                return dtf.format(Instant.ofEpochMilli(object));
            }

            @Override
            public Long fromString(String string) {
                if (string == null || string.isEmpty()){
                    return null;
                }
                return Instant.from(dtf.parse(string)).toEpochMilli();
            }
        };
    }

    /**
     * Deletes the localProperties.csv file if it already exists and writes information about content of Manufacturer, Model
     * and localProperties number to localProperties.csv
     */
    private void writeLocalProperties() {
        Path newFilePath = Paths.get(DigividProcessor.localProperties);
        try {
            Path parentDir = newFilePath.getParent();
            if (!Files.exists(parentDir))
                Files.createDirectories(parentDir);
            String msg = String.format("%s,%s,%s", txtManufacturer.getText(), txtModel.getText(), txtSerial.getText());
            Files.write(newFilePath, msg.getBytes("UTF-8"), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        } catch (IOException ioe) {
            log.error("Caught error while writing {}", DigividProcessor.localProperties, ioe);
            Utils.showErrorDialog("Caught error while writing local properties\n\n", Thread.currentThread(), ioe);
        }
    }

    /**
     * Reads information from the meatadata.csv file and put it in the fields for Manufacturer, Model and Serialnumber
     */
    private void readLocalProperties() {
        try {
            Path newFilePath = Paths.get(DigividProcessor.localProperties);
            if (Files.exists(newFilePath)) {
                List<String> lines = Files.readAllLines(Paths.get(DigividProcessor.localProperties),
                        Charset.defaultCharset());
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
        } catch (IOException e) {
            log.warn("Error occured reading {}", DigividProcessor.localProperties);
            Utils.showErrorDialog("Caught exception while reading channelfile\n\n", Thread.currentThread(), e);
        }
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

    private void addChannelTextfield() {
        altChannel = new TextField();
        altChannel.setId("altChannel");
        altChannel.setPrefWidth(150.0);
        channelGridPane.getChildren().add(altChannel);
        GridPane.setRowIndex(altChannel, 4);
        GridPane.setColumnIndex(altChannel, 0);
    }

    /**
     * The tableview displays an overview of ts-files
     */
    protected void loadFilenames() {
        ObservableList<VideoFileObject> videoFileObjects = FXCollections.observableList(new ArrayList<>());

        //Initial setup of files in table
        try (DirectoryStream<Path> tsFiles = Files.newDirectoryStream(getDataPath(), "*.ts")) {
            for (Path tsFile : tsFiles) {
                videoFileObjects.add(VideoFileObject.createFromTS(tsFile));
            }
            tableView.setItems(videoFileObjects);
        } catch (IOException e) {
            throw new RuntimeException("" + getDataPath().toAbsolutePath(),e);
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
        File file = thisVideoFileRow.getVideoFilePath().toFile();
        boolean fileIsNotLocked = file.renameTo(file);
        if (validGUIvalues(thisVideoFileRow, fileIsNotLocked)) {
            String tmpMetadataFilename = thisVideoFileRow.getVideoFilePath().getFileName() + ".temporary";
            Path tmpMetadataPath = thisVideoFileRow.getVideoFilePath().getParent().resolve(Paths.get(tmpMetadataFilename));
            try {
                if (Files.exists(tmpMetadataPath))
                    Files.delete(tmpMetadataPath);
            } catch (IOException e) {
                log.error("IO exception happened when deleting the file in commit",e);
                Utils.showErrorDialog(Thread.currentThread(), e);
            }
            thisVideoFileRow.commit();
        }
    }

    private boolean validGUIvalues(VideoFileObject thisVideoFileRow, boolean fileIsNotLocked) {
        if (fileIsNotLocked) {
            if (!ValidateVideoMetadata()) return false;
          //  if (!setValidDate(thisVideoFileRow)) return false;
            detailVHS.setVisible(false);
            return true;
        } else {
            Utils.showWarning("The file is currently locked by another program and cannot be altered.");
            return false;
        }
    }

    private boolean ValidateVideoMetadata() {
        String txtProcessedManufacturerText = txtProcessedManufacturer.getText();
        if (txtProcessedManufacturerText == null || (txtProcessedManufacturerText.trim().isEmpty())) {
            Utils.showWarning("Manufacturer is not allowed to be empty");
            return false;
        }
        if (txtProcessedModel.getText() == null || (txtProcessedModel.getText().trim().isEmpty())) {
            Utils.showWarning("Model field is not allowed to be empty");
            return false;
        }

        if (txtProcessedSerial.getText() == null || (txtProcessedSerial.getText().trim().isEmpty())) {
            Utils.showWarning("Serial number is not allowed to be empty");
            return false;
        }

        if (txtVhsLabel.getText() == null || (txtVhsLabel.getText().trim().isEmpty())) {
            Utils.showWarning("Video label is not allowed to be empty");
            return false;
        }
        return true;
    }

    /**
     * Show the video file in the player
     */
    public void playCurrentFile() {
        try {
            ProcessBuilder pb = new ProcessBuilder(DigividProcessor.player,
                    new File(DigividProcessor.recordsDir, thisVideoFileRow.getFilename()).getAbsolutePath());
            pb.start();
        } catch (IOException e) {
            log.error("{} could not be played", thisVideoFileRow.getFilename());
            Utils.showErrorDialog("The file could not be played\n\n", Thread.currentThread(), e);
        }
    }

}
