package dk.statsbiblioteket.digivid.processor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
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
    private static final String channelPattern = "^[a-zæøå0-9]{3,}$";
    private static Logger log = LoggerFactory.getLogger(Controller.class);
    @FXML public TableView<VideoFileObject> tableView;
    @FXML public GridPane channelGridPane;
    @FXML public TableColumn<VideoFileObject, Date> lastmodifiedColumn;
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
    private boolean temporaryFileSave = true;
    private boolean changedField = false;
    private VideoFileObject thisVideoFileRow;

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

        detailVHS.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent t) {
                changedField = true;
            }
        });

        //The different items gets saved in a temporary json-file when the control loses focus
        //Start lost focus eventhandlers
        txtVhsLabel.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && txtVhsLabel != null) {
                storeTextFieldInformation(txtVhsLabel);
                changedField = false;
            }
        });
        startTimeField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && startTimeField != null) {
                if (Pattern.matches(hourPattern, startTimeField.getText()) || startTimeField.getText().isEmpty()) {
                    setStartCalendar(thisVideoFileRow);
                    thisVideoFileRow.preprocess();
                } else {
                    Utils.showWarning("Start time is not valid (should be hh:mm)");
                    startTimeField.requestFocus();
                }
                changedField = false;
            }
        });
        endTimeField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && endTimeField != null) {
                if (Pattern.matches(hourPattern, endTimeField.getText()) || (endTimeField.getText().isEmpty())) {
                    setEndCalendar(thisVideoFileRow);
                    thisVideoFileRow.preprocess();
                } else {
                    Utils.showWarning("Start time is not valid (should be hh:mm)");
                    endTimeField.requestFocus();
                }
                changedField = false;
            }
        });
        txtProcessedManufacturer.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && txtProcessedManufacturer != null) {
                storeTextFieldInformation(txtProcessedManufacturer);
                changedField = false;
            }
        });
        txtProcessedModel.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && txtProcessedModel != null) {
                storeTextFieldInformation(txtProcessedModel);
                changedField = false;
            }
        });
        txtComment.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && txtComment != null) {
                thisVideoFileRow.setComment(txtComment.getText());
                thisVideoFileRow.preprocess();
                changedField = false;
            }
        });
        txtProcessedSerial.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && txtProcessedSerial != null) {
                storeTextFieldInformation(txtSerial);
                changedField = false;
            }
        });
        altChannel.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && altChannel != null) {
                storeTextFieldInformation(altChannel);
                changedField = false;
            }
        });
        cmbQuality.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && cmbQuality != null) {
                thisVideoFileRow.setQuality(cmbQuality.getValue());
                thisVideoFileRow.preprocess();
                changedField = false;
            }
        });
        startDatePicker.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && startDatePicker != null) {
                setStartCalendar(thisVideoFileRow);
                thisVideoFileRow.preprocess();
                changedField = false;
            }
        });

        endDatePicker.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && changedField && startDatePicker != null) {
                setEndCalendar(thisVideoFileRow);
                thisVideoFileRow.preprocess();
                changedField = false;
            }
        });
        //End lost focus eventhandlers

        txtFilename.setEditable(false);
        //Use css-style to make the textfield seem like a label
        txtFilename.getStyleClass().add("copyable-label");
        txtFilename.textProperty().addListener((ob, o, n) ->
        {
            // expand the textfield
            txtFilename.setPrefWidth(Utils.computeTextWidth(txtFilename.getFont(),
                    txtFilename.getText(), 0.0D) + 20);
        });

        startDatePicker.setOnAction((event) -> endDatePicker.setValue(startDatePicker.getValue()));
        startDatePicker.setConverter(new StringConverter<LocalDate>() {
            DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE;

            @Override
            public String toString(LocalDate localDate) {
                try {
                    if (localDate != null)
                        return dtf.format(localDate);
                    return dtf.format(LocalDate.now());
                } catch (Exception e) {
                    return dtf.format(LocalDate.now());
                }


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
                if (localDate != null)
                    return dtf.format(localDate);
                return dtf.format(LocalDate.now());
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
         * The filename is colored blue when it has a temporary json-file, but is not yet processed.
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
                    File tmpMetadata = new File(DigividProcessor.recordsDir + "/" + item + ".temporary");
                    if (tmpMetadata.exists())
                        setTextFill(Color.BLUE);
                    else
                        setTextFill(Color.GREEN);
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
                        setText(Character.toString((char) CHECKMARK));
                    } else {
                        setText("");
                    }
                }
            }
        });

        /**
         * Indicate with a checkmark if the file is processed
         */
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

        channelGroup.selectedToggleProperty().addListener((ov, t, t1) -> {
            if (temporaryFileSave) {
                final Toggle selectedToggle = channelGroup.getSelectedToggle();
                RadioButton chk = (RadioButton) t1.getToggleGroup().getSelectedToggle(); // Cast object to radio button
                thisVideoFileRow.setChannel(((Channel) selectedToggle.getUserData()).getChannelName());
                thisVideoFileRow.preprocess();
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

    private void storeTextFieldInformation(TextField txtField) {
        thisVideoFileRow.setVhsLabel(txtField.getText());
        thisVideoFileRow.preprocess();
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
        ObservableList<VideoFileObject> videoFileObjects = FXCollections.observableList(new ArrayList<>());
        if (tableView != null) {
            videoFileObjects.clear();
            if (getDataPath() != null) {
                DirectoryStream<Path> tsFiles = null;
                try {
                    tsFiles = Files.newDirectoryStream(getDataPath(), "*.ts");
                    for (Path tsFile : tsFiles) {
                        if (!(tsFile.getFileName().toString().startsWith("temp"))) //Skip files that start with "temp"
                        {
                            videoFileObjects.add(new VideoFileObject(tsFile));
                        }
                        
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
                } else
                    tableView.getSelectionModel().select(0);
            } else {
                log.error("Datapath is not defined when file is loaded");
                Utils.showErrorDialog(Thread.currentThread(),
                        new Exception("Datapath is not defined when file is loaded"));
            }
        }
    }

    /**
     * Assigne current VideoFileObject values to GUI values before calling commit()
     *
     * @param actionEvent The event that activated commit
     */
    public void commit(ActionEvent actionEvent) {
        File file = thisVideoFileRow.videoFilePath.toFile();
        boolean fileIsNotLocked = file.renameTo(file);
        if (validGUIvalues(thisVideoFileRow, fileIsNotLocked)) {
            String tmpMetadataFilename = thisVideoFileRow.videoFilePath.getFileName() + ".temporary";
            Path tmpMetadataPath = thisVideoFileRow.videoFilePath.getParent().resolve(Paths.get(tmpMetadataFilename));
            try {
                if (Files.exists(tmpMetadataPath))
                    Files.delete(tmpMetadataPath);
            } catch (IOException e) {
                log.error("IO exception happened when deleting the file in commit");
                Utils.showErrorDialog(Thread.currentThread(), e);
            }

            thisVideoFileRow.commit();
        }
    }

    /**
     * Assigne current VideoFileObject values to GUI values before calling preprocess()
     *
     * @param actionEvent The event that activated preprocess
     */
    public void preprocess(ActionEvent actionEvent) {
        File file = thisVideoFileRow.videoFilePath.toFile();
        boolean fileIsNotLocked = file.renameTo(file);
        if (validGUIvalues(thisVideoFileRow, fileIsNotLocked))
            thisVideoFileRow.preprocess();
    }

    private boolean validGUIvalues(VideoFileObject thisVideoFileRow, boolean fileIsNotLocked) {
        if (fileIsNotLocked) {
            if (!setValidVideoMetadata(thisVideoFileRow)) return false;
            if (!setValidChannel(thisVideoFileRow)) return false;
            if (!setValidDate(thisVideoFileRow)) return false;
            detailVHS.setVisible(false);
            return true;
        } else {
            Utils.showWarning("The file is currently locked by another program and cannot be altered.");
            return false;
        }
    }

    private boolean setValidDate(VideoFileObject thisVideoFileRow) {
        if (!validateDate()) return false;

        Calendar startCalendar = setStartCalendar(thisVideoFileRow);

        Calendar endCalendar = setEndCalendar(thisVideoFileRow);

        return validateStartBeforeEnd(startCalendar, endCalendar);
    }

    private Calendar setEndCalendar(VideoFileObject thisVideoFileRow) {
        LocalDate localDate = endDatePicker.getValue();
        if (localDate == null)
            return null;
        Instant instant = localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(Date.from(instant));

        String[] timeStr;
        if (!endTimeField.getText().isEmpty())
            timeStr = endTimeField.getText().split(":");
        else
            timeStr = "00:00".split(":");
        endCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr[0]));
        endCalendar.set(Calendar.MINUTE, Integer.parseInt(timeStr[1]));
        thisVideoFileRow.setEndDate(endCalendar.getTime().getTime());
        return endCalendar;
    }

    private Calendar setStartCalendar(VideoFileObject thisVideoFileRow) {
        LocalDate localDate = startDatePicker.getValue();
        if (localDate == null)
            return null;
        Instant instant = localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(Date.from(instant));

        String[] timeStr;
        if (!startTimeField.getText().isEmpty())
            timeStr = startTimeField.getText().split(":");
        else
            timeStr = "00:00".split(":");
        startCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr[0]));
        startCalendar.set(Calendar.MINUTE, Integer.parseInt(timeStr[1]));
        thisVideoFileRow.setStartDate(startCalendar.getTime().getTime());
        return startCalendar;
    }

    private boolean validateStartBeforeEnd(Calendar startCalendar, Calendar endCalendar) {
        if (startCalendar.getTime().after(endCalendar.getTime())) {
            Utils.showWarning("Start time has to be before end time");
            return false;
        }
        return true;
    }

    private boolean validateDate() {
        if (startDatePicker.getValue() == null) {
            Utils.showWarning("No Start Date Set.");
            return false;
        }
        if (startTimeField.getText() == null || (startTimeField.getText().isEmpty())) {
            Utils.showWarning("No Start Time Set.");
            startTimeField.requestFocus();
            return false;
        } else if (!Pattern.matches(hourPattern, startTimeField.getText())) {
            Utils.showWarning("Start time not valid (should be hh:mm)");
            startTimeField.requestFocus();
            return false;
        }
        if ((endDatePicker.getValue() != null && endDatePicker.getValue().toString().isEmpty()) ||
                (endDatePicker.getValue() == null)) {
            Utils.showWarning("No End Date Set.");
            return false;
        }

        if (endTimeField.getText() == null || (endTimeField.getText().isEmpty())) {
            Utils.showWarning("No End Time Set.");
            endTimeField.requestFocus();
            return false;
        } else if (!Pattern.matches(hourPattern, endTimeField.getText())) {
            Utils.showWarning("End time not valid (should be hh:mm)");
            endTimeField.requestFocus();
            return false;
        }
        return true;
    }

    private boolean setValidChannel(VideoFileObject thisVideoFileRow) {
        final Toggle selectedToggle = channelGroup.getSelectedToggle();
        String altChannel = this.altChannel.getText();
        if (!validateChannel(altChannel)) return false;
        if (altChannel != null && altChannel.length() > 0) {
            thisVideoFileRow.setChannel(altChannel);
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

    private boolean validateChannel(String altChannel) {
        if ((altChannel != null && altChannel.length() > 0) && (!Pattern.matches(channelPattern, altChannel))) {
            Utils.showWarning("Channel has to be at least 3 lowercase alphanumeric characters");
            return false;
        }
        return true;
    }

    private boolean setValidVideoMetadata(VideoFileObject thisVideoFileRow) {
        if (!ValidateVideoMetadata()) return false;
        thisVideoFileRow.setVhsLabel(txtVhsLabel.getText());
        thisVideoFileRow.setManufacturer(txtProcessedManufacturer.getText());
        thisVideoFileRow.setModel(txtProcessedModel.getText());
        thisVideoFileRow.setSerialNo(txtProcessedSerial.getText());
        thisVideoFileRow.setQuality(cmbQuality.getValue());
        thisVideoFileRow.setComment(txtComment.getText());
        return true;
    }

    private boolean ValidateVideoMetadata() {
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

    /**
     * Show the file details for the file (which is found in the files localProperties file), that the user clicked on
     */
    private void loadFile(VideoFileObject currentVideoFile) {
        thisVideoFileRow = currentVideoFile;
        temporaryFileSave = false;
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
        temporaryFileSave = true;
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
