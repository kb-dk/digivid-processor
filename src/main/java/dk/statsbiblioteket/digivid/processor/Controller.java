package dk.statsbiblioteket.digivid.processor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Shows GUI and requests videofiles to be renamed and a JSON-file to be generated according to the GUI-users choices.
 */
public class Controller {

    private static final int CHECKMARK = 10003;
    private static final String hourPattern = "([01]?[0-9]|2[0-3]):[0-5][0-9]";
    private static final String channelPattern = "^[a-z0-9]{3,}$";
    private static Logger log = LoggerFactory.getLogger(Controller.class);
    @FXML public TableView<VideoFileObject> tableView;
    @FXML public GridPane channelGridPane;
    @FXML public TableColumn<VideoFileObject, Date> lastmodifiedColumn;
    @FXML public TableColumn<VideoFileObject, Boolean> processedColumn;
    @FXML
    public TableColumn<VideoFileObject, Long> filesizeColumn;
    @FXML public TextArea txtComment;
    @FXML public ComboBox<String> cmbQuality;
    @FXML
    public TextField txtFilename;
    @FXML public TextField txtVhsLabel;
    @FXML public TextField startTimeField;
    @FXML public TextField endTimeField;
    @FXML public TextField txtManufacturer;
    @FXML public TextField txtModel;
    @FXML public TextField txtSerial;
    @FXML
    public TextField txtProcessedManufacturer;
    @FXML
    public TextField txtProcessedModel;
    @FXML
    public TextField txtProcessedSerial;
    @FXML public DatePicker startDatePicker;
    @FXML public DatePicker endDatePicker;
    @FXML public ToggleGroup channelGroup;
    @FXML public javafx.scene.layout.AnchorPane detailVHS;
    private Path dataPath;
    private TextField altChannel;

    private static void checkConfigfile() {
        try {
            Path recordsPath = Paths.get(DigividProcessor.recordsDir);
            Path channelsCSVPath = Paths.get(DigividProcessor.channelCSV);
            Path playerPath = Paths.get(DigividProcessor.player);
            Path localPropertiesPath = Paths.get(DigividProcessor.localProperties);
            boolean pathExist = Files.exists(recordsPath) && Files.exists(channelsCSVPath) && Files.exists(playerPath);
            if (!pathExist) {
                Utils.showErrorDialog("Configuration file is invalid. Contact system administrator.", Thread.currentThread(), null);
            }
        } catch (Exception ex) {
            Utils.showErrorDialog("Configuration file is not valid.\n\n", Thread.currentThread(), ex);
        }
    }

    @FXML
    public void handleLocalProperties() {
        writeLocalProperties();
    }

    @FXML
    void initialize() {
        detailVHS.setVisible(false);
        checkConfigfile();
        if (lastmodifiedColumn != null) lastmodifiedColumn.setComparator(Date::compareTo);
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

        txtFilename.setEditable(false);
        //Use css-style to make the textfield seem like a label
        txtFilename.getStyleClass().add("copyable-label");
        txtFilename.textProperty().addListener((ob, o, n) ->
        {
            // expand the textfield
            txtFilename.setPrefWidth(Utils.computeTextWidth(txtFilename.getFont(),
                    txtFilename.getText(), 0.0D) + 20);
        });

        startDatePicker.setOnAction(event -> endDatePicker.setValue(startDatePicker.getValue()));
        startDatePicker.setConverter(new StringConverter<LocalDate>() {
            DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE;

            @Override
            public String toString(LocalDate localDate) {
                return dtf.format(localDate);
            }

            @Override
            public LocalDate fromString(String s) {
                return LocalDate.parse(s, dtf);
            }
        });

        endDatePicker.setConverter(new StringConverter<LocalDate>() {
            DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE;

            @Override
            public String toString(LocalDate localDate) {
                return dtf.format(localDate);
            }

            @Override
            public LocalDate fromString(String s) {
                return LocalDate.parse(s, dtf);
            }
        });

        /**
         * Custom rendering of the table cell to have format specified "yyyy-mm-dd hh:mm.
         */
        lastmodifiedColumn.setCellFactory(column -> {
            SimpleDateFormat myDateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm");
            return new TableCell<VideoFileObject, Date>() {
                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);
                    setAlignment(Pos.CENTER);

                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(myDateFormatter.format(item));
                    }
                }
            };
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
                        setText(Character.toString((char) CHECKMARK));
                    } else {
                        setText("");
                    }
                }
            }
        });

        /**
         * Enable/disable the channel radiobuttons depending on whether altChannel is empty or not
         */
        altChannel.textProperty().addListener((observableValue, oldValue, newValue) -> {
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
        });
        readLocalProperties();
    }

    private Path getDataPath() {
        return dataPath;
    }

    protected void setDataPath(Path dataPath) {
        this.dataPath = dataPath;
        try {
            WatchService service = getDataPath().getFileSystem().newWatchService();
            getDataPath().register(service,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);
            new Thread() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            WatchKey key = service.take();
                            if (!key.pollEvents().isEmpty()) {
                                Platform.runLater(Controller.this::loadFilenames);
                            }
                            boolean valid = key.reset();
                            if (!valid)
                                break;
                        } catch (InterruptedException e) {
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
        tableView.setOnMouseClicked(new FileclickMouseEventHandler());
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
        if (tableView != null) {
            ObservableList<VideoFileObject> videoFileObjects = FXCollections.observableList(new ArrayList<>());
            if (getDataPath() != null) {
                DirectoryStream<Path> tsFiles = null;
                try {
                    tsFiles = Files.newDirectoryStream(getDataPath(), "*.ts");
                    for (Path tsFile : tsFiles) {
                        videoFileObjects.add(new VideoFileObject(tsFile));
                    }
                    tableView.setItems(videoFileObjects);
                } catch (IOException e) {
                    throw new RuntimeException("" + getDataPath().toAbsolutePath());
                } finally {
                    try {
                        if (tsFiles != null)
                            tsFiles.close();
                    } catch (IOException ioe) {
                        log.error("Error occured while loading files", ioe);
                        Utils.showErrorDialog("Error occured while loading files\n\n", Thread.currentThread(), ioe);
                    }
                }
                ObservableList<TableColumn<VideoFileObject, ?>> sortOrder = tableView.getSortOrder();
                sortOrder.clear();
                sortOrder.addAll(processedColumn, lastmodifiedColumn);
                tableView.sort();
                tableView.getSelectionModel().select(0);
            } else {
                log.error("Datapath is not defined when file is loaded");
                Utils.showErrorDialog(Thread.currentThread(),
                        new Exception("Datapath is not defined when file is loaded"));
            }
        }
    }

    /**
     * Reads all the values set by the user and sets them on the current VideoFileObject before calling the commit()
     * method on that object.
     *
     * @param actionEvent The event that activated commit
     */
    public void commit(ActionEvent actionEvent) {

        VideoFileObject thisVideoFileRow = tableView.getSelectionModel().getSelectedItem();

        File file = thisVideoFileRow.videoFilePath.toFile();
        boolean fileIsNotLocked = file.renameTo(file);
        if (fileIsNotLocked) {
            if (!setValidVideoMetadata(thisVideoFileRow)) return;
            if (!setValidChannel(thisVideoFileRow)) return;
            if (!setValidDate(thisVideoFileRow)) return;

            detailVHS.setVisible(false);

            thisVideoFileRow.commit();
        } else {
            Utils.showWarning("The file is currently locked by another program and cannot be altered.");
        }
    }

    private boolean setValidDate(VideoFileObject thisVideoFileRow) {
        if (startDatePicker.getValue() == null) {
            Utils.showWarning("No Start Date Set.");
            return false;
        }
        if (startTimeField.getText() == null || (startTimeField.getText().isEmpty())) {
            Utils.showWarning("No Start Time Set.");
            return false;
        } else if (!Pattern.matches(hourPattern, startTimeField.getText())) {
            Utils.showWarning("Start time not valid");
            return false;
        }
        if ((endDatePicker.getValue() != null && endDatePicker.getValue().toString().isEmpty()) ||
                (endDatePicker.getValue() == null)) {
            Utils.showWarning("No End Date Set.");
            return false;
        }

        if (endTimeField.getText() == null || (endTimeField.getText().isEmpty())) {
            Utils.showWarning("No End Time Set.");
            return false;
        } else if (!Pattern.matches(hourPattern, endTimeField.getText())) {
            Utils.showWarning("End time not valid");
            return false;
        }

        LocalDate localDate = startDatePicker.getValue();
        Instant instant = localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(Date.from(instant));

        String[] timeStr = startTimeField.getText().split(":");
        startCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr[0]));
        startCalendar.set(Calendar.MINUTE, Integer.parseInt(timeStr[1]));
        thisVideoFileRow.setStartDate(startCalendar.getTime().getTime());

        localDate = endDatePicker.getValue();
        instant = localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(Date.from(instant));

        timeStr = endTimeField.getText().split(":");
        endCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr[0]));
        endCalendar.set(Calendar.MINUTE, Integer.parseInt(timeStr[1]));
        thisVideoFileRow.setEndDate(endCalendar.getTime().getTime());

        if (startCalendar.getTime().after(endCalendar.getTime())) {
            Utils.showWarning("Start time has to be before end time");
            return false;
        }
        return true;
    }

    private boolean setValidChannel(VideoFileObject thisVideoFileRow) {
        final Toggle selectedToggle = channelGroup.getSelectedToggle();
        String altChannel = this.altChannel.getText();
        if (altChannel != null && altChannel.length() > 0) {
            if (Pattern.matches(channelPattern, altChannel)) {
                thisVideoFileRow.setChannel(altChannel);
            } else {
                Utils.showWarning("Channel has to be at least 3 lowercase alphanumeric characters");
                return false;
            }
        } else {
            String channel = null;
            if (selectedToggle != null) {
                channel = ((Channel) selectedToggle.getUserData()).getChannelName();
            }
            thisVideoFileRow.setChannel(channel);
        }
        if (thisVideoFileRow.getChannel() == null) {
            Utils.showWarning("No channel specified.");
            return false;
        }
        return true;
    }

    private boolean setValidVideoMetadata(VideoFileObject thisVideoFileRow) {
        if (txtProcessedManufacturer.getText() == null || (txtProcessedManufacturer.getText().trim().isEmpty())) {
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
        thisVideoFileRow.setVhsLabel(txtVhsLabel.getText());
        thisVideoFileRow.setManufacturer(txtProcessedManufacturer.getText());
        thisVideoFileRow.setModel(txtProcessedModel.getText());
        thisVideoFileRow.setSerialNo(txtProcessedSerial.getText());
        thisVideoFileRow.setQuality(cmbQuality.getValue());
        thisVideoFileRow.setComment(txtComment.getText());
        return true;
    }

    /**
     * Show the video file in the player
     */
    public void playCurrentFile() {
        VideoFileObject thisRow = tableView.getSelectionModel().getSelectedItem();
        try {
            ProcessBuilder pb = new ProcessBuilder(DigividProcessor.player,
                    new File(DigividProcessor.recordsDir, thisRow.getFilename()).getAbsolutePath());
            pb.start();
        } catch (IOException e) {
            log.error("{} could not be played", thisRow.getFilename());
            Utils.showErrorDialog("The file could not be played\n\n", Thread.currentThread(), e);
        }
    }

    /**
     * Show the file details for the file (which is found in the files localProperties file), that the user clicked on
     */
    private void loadFile(VideoFileObject currentVideoFile) {
        txtFilename.setText(currentVideoFile.getFilename());
        GregorianCalendar startCalendar = new GregorianCalendar();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        if (currentVideoFile.getStartDate() != null) {
            startCalendar.setTime(new Date(currentVideoFile.getStartDate()));
            startDatePicker.setValue(new Date(currentVideoFile.getStartDate()).
                    toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            startTimeField.setText(timeFormat.format(startCalendar.getTime()));
        } else {
            startDatePicker.setValue(null);
            startTimeField.setText("");
        }
        GregorianCalendar endCalendar = new GregorianCalendar();
        if (currentVideoFile.getEndDate() != null) {
            endCalendar.setTime(new Date(currentVideoFile.getEndDate()));
            endDatePicker.setValue(new Date(currentVideoFile.getEndDate()).
                    toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            endTimeField.setText(timeFormat.format(endCalendar.getTime()));
        } else {
            endDatePicker.setValue(null);
            endTimeField.setText("");
        }
        final String quality = currentVideoFile.getQuality();
        if (quality != null)
            cmbQuality.getSelectionModel().select(quality);
        else
            cmbQuality.getSelectionModel().select(4);
        txtVhsLabel.setText(currentVideoFile.getVhsLabel());
        txtComment.setText(currentVideoFile.getComment());

        if (currentVideoFile.getManufacturer() != null)
            txtProcessedManufacturer.setText(currentVideoFile.getManufacturer());
        else
            txtProcessedManufacturer.setText(txtManufacturer.getText());

        if (currentVideoFile.getModel() != null)
            txtProcessedModel.setText(currentVideoFile.getModel());
        else
            txtProcessedModel.setText(txtModel.getText());

        if (currentVideoFile.getSerialNo() != null)
            txtProcessedSerial.setText(currentVideoFile.getSerialNo());
        else
            txtProcessedSerial.setText(txtSerial.getText());

        String currentChannel = currentVideoFile.getChannel();
        boolean inGrid = false;
        for (Node channelNode : Controller.this.channelGridPane.getChildren()) {
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
            altChannel.setText(null);
        } else {
            altChannel.setText(currentVideoFile.getChannel());
        }
    }

    public class FileclickMouseEventHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                VideoFileObject thisVideoFile =
                        (VideoFileObject) ((TableView) mouseEvent.getSource()).getSelectionModel().getSelectedItem();
                if (thisVideoFile != null) {
                    loadFile(thisVideoFile);
                    detailVHS.setVisible(true);
                }
            } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                playCurrentFile();
            }
        }
    }
}
