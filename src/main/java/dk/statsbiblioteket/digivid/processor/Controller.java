package dk.statsbiblioteket.digivid.processor;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

public class Controller {

    private Path dataPath;
    private TextField altChannel;
    private String manufacturer;
    private String model;
    private String serialNo;

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
    @FXML
    public TextField txtManufacturer;
    @FXML
    public TextField txtModel;
    @FXML
    public TextField txtSerial;
    @FXML
    public void handleSerial() {
        writeSerial();
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
        try {
            List<List<String>> channels = getCSV(DigividProcessor.channelCSV);
            for(List<String> channel : channels) {
                addChannelButton(channel.get(0), channel.get(1), channel.get(2), Integer.parseInt(channel.get(3)), Integer.parseInt(channel.get(4)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


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
        }) ;

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
        }) ;

        SimpleDateFormat myDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        readSerial();

        // Custom rendering of the table cell to have format specified "yyyy-mm-dd.
        lastmodifiedColumn.setCellFactory(column -> {
            return new TableCell<FileObject, Date>() {
                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        // Format date.
                        setText(myDateFormatter.format(item));
                    }
                }
            };
        });

        processedColumn.setCellFactory(column -> {
            return new TableCell<FileObject, Boolean>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        // Format date.
                        setText(item.toString());

                        // Style all dates in March with a different color.
                        if (item) {
                            setTextFill(javafx.scene.paint.Color.BLACK);
                            //setStyle("-fx-background-color: yellow");
                        } else {
                            setTextFill(javafx.scene.paint.Color.CHOCOLATE);
                            setStyle("");
                        }
                    }
                }
            };
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

    private void writeSerial() {
        Path newFilePath = Paths.get(DigividProcessor.serial);
        try {
            if (Files.exists(newFilePath)) {
                Files.delete(newFilePath);
                String msg = txtManufacturer.getText() + "," + txtModel.getText() + "," + txtSerial.getText()+",1";
                Files.write(Paths.get(DigividProcessor.serial), msg.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readSerial() {
        Path newFilePath = Paths.get(DigividProcessor.serial);
        try {
            if (Files.exists(newFilePath)) {
                List<String> lines = Files.readAllLines(Paths.get(DigividProcessor.serial), Charset.defaultCharset());
                List<String> metadata = Arrays.asList(lines.get(0).split(","));

                txtManufacturer.setText(metadata.get(0));
                txtModel.setText(metadata.get(1));
                txtSerial.setText(metadata.get(2));
            } else {
                String msg = ",,,1";
                Files.write(Paths.get(DigividProcessor.serial), msg.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception ex) {
            ex.printStackTrace();
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

    private static List<List<String>> getCSV(String csvFile) throws IOException {


        String line = null;
        BufferedReader stream = null;
        List<List<String>> csvData = new ArrayList<List<String>>();

        try {
            stream = new BufferedReader(new FileReader(csvFile));
            while ((line = stream.readLine()) != null) {
                String[] splitted = line.split(",");
                List<String> dataLine = new ArrayList<String>(splitted.length);
                for (String data : splitted)
                    dataLine.add(data);
                csvData.add(dataLine);
            }
        } finally {
            if (stream != null)
                stream.close();
        }

        return csvData;

    }

    /*public Map<String, String> getCSV(String csvFile) {
        BufferedReader br = null;
        String line = "";
        String csvSplitBy = ",";
        Map<String, String> channels = new HashMap<String, String>();


        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                String[] channel = line.split(csvSplitBy);
                channels.put(channel[0], channel[1], channel[2], Integer.parseInt(channel[3]), Integer.parseInt(channel[4]));
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
        return channels;
    }*/

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
            if ( selectedToggle != null) {
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

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public String getSerialNo() {
        return serialNo;
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
                playCurrentFile();
            }
        }
    }

    public void playCurrentFile() {
        FileObjectImpl thisRow = (FileObjectImpl) tableView.getSelectionModel().getSelectedItem();
        try {
            ProcessBuilder  pb = new ProcessBuilder(DigividProcessor.player,DigividProcessor.recordsDir+"/"+thisRow.getFilename()); //" C:\\Test\\test.mp4");
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
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
