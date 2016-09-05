package dk.statsbiblioteket.digivid.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk7.Jdk7Module;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;

/**
 * Class which beside the properties about the videfile also has methods for renaming the videofile and
 * commiting the class which also writes a json-file
 */
public class VideoFileObject {

    public static final String COMMENTS = ".comments";
    public static final String TEMPORARY = ".temporary";
    private Logger log = LoggerFactory.getLogger(getClass());


    //These properties are in JSON
    private final MonitoredProperty<Long> filesize = new MonitoredProperty<>(this, "filesize", null);
    private final MonitoredProperty<String> filename = new MonitoredProperty<>(this, "filename", null);
    private final MonitoredProperty<Long> startDate = new MonitoredProperty<>(this, "startDate", null);
    private final MonitoredProperty<String> vhsLabel = new MonitoredProperty<>(this, "vhsLabel", null);
    private final MonitoredProperty<String> comment = new MonitoredProperty<>(this, "comment", null);
    private final MonitoredProperty<String> quality = new MonitoredProperty<>(this, "quality", null);
    private final MonitoredProperty<String> channel = new MonitoredProperty<>(this, "channel", null);
    private final MonitoredProperty<Long> endDate = new MonitoredProperty<>(this, "endDate", null);
    private final MonitoredProperty<String> checksum = new MonitoredProperty<>(this, "checksum", null);
    private final MonitoredProperty<String> manufacturer = new MonitoredProperty<>(this, "manufacturer", null);
    private final MonitoredProperty<String> model = new MonitoredProperty<>(this, "model", null);
    private final MonitoredProperty<String> serialNo = new MonitoredProperty<>(this, "serialNo", null);
    private final MonitoredProperty<String> encoderName = new MonitoredProperty<>(this, "encoderName", null);

    //This is used just for the display table
    private final MonitoredProperty<Instant> lastModified = new MonitoredProperty<>(this, "lastModified", null);

    private final SimpleBooleanProperty dirty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty processed = new SimpleBooleanProperty(false);

    public static VideoFileObject create(Path tsFile) throws IOException {

        Path vhsFileMetadataFilePath = tsFile.resolveSibling(tsFile.getFileName().toString() + COMMENTS);
        Path tmpMetadataPath = tsFile.resolveSibling(tsFile.getFileName().toString() + TEMPORARY);

        ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk7Module());

        VideoFileObject obj;
        if (Files.exists(vhsFileMetadataFilePath)) {
            obj = mapper.readValue(vhsFileMetadataFilePath.toFile(), VideoFileObject.class);
        } else if (Files.exists(tmpMetadataPath)) {
            obj = mapper.readValue(tmpMetadataPath.toFile(), VideoFileObject.class);
        } else {
            obj = new VideoFileObject();
        }

        obj.setFilename(tsFile.getFileName().toString());

        obj.setFilesize(Files.size(tsFile));
        obj.setLastModified(Files.getLastModifiedTime(tsFile).toInstant());

        try {
            obj.setEncoderName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            obj.setEncoderName("unknown");
        }

        //Just loaded, not dirty
        obj.markClean();

        //as it is not dirty, it is processed if the comments file exists
        obj.setProcessed(Files.exists(obj.getVhsFileMetadataFilePath()));

        return obj;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk7Module());

        //Object to JSON in String
        return mapper.writeValueAsString(this);
    }


    @JsonIgnore
    public Path getVideoFilePath() {
        return DigividProcessor.recordsDir.resolve(getFilename());
    }

    @JsonIgnore
    public Path getVhsFileMetadataFilePath() {
        return DigividProcessor.recordsDir.resolve(getFilename()+COMMENTS);
    }

    @JsonIgnore
    public Path getTmpFileMetadataFilePath() {
        return DigividProcessor.recordsDir.resolve(getFilename()+TEMPORARY);
    }


    public String getFilename() {
        return filename.get();
    }

    public void setFilename(String filename) {
        this.filename.set(filename);
    }

    public MonitoredProperty<String> filenameProperty() {
        return filename;
    }


    public Long getFilesize() {
        final long K = 1024;
        long size;
        try {
            size = getVideoFilePath().toFile().length() / K;
        } catch (Exception e) {
            size = 0L;
        }
        setFilesize(size);
        return size;
    }

    public void setFilesize(Long filesize) {
        this.filesize.set(filesize);
    }

    public MonitoredProperty<Long> filesizeProperty() {
        return filesize;
    }


    public Long getStartDate() {
        return startDate.get();
    }

    public void setStartDate(Long startDate) {
        this.startDate.set(startDate);
    }

    public MonitoredProperty<Long> startDateProperty() {
        return startDate;
    }


    public String getVhsLabel() {
        return vhsLabel.get();
    }

    public void setVhsLabel(String vhsLabel) {
        this.vhsLabel.set(vhsLabel);
    }

    public MonitoredProperty<String> vhsLabelProperty() {
        return vhsLabel;
    }


    public String getComment() {
        return comment.get();
    }

    public void setComment(String comment) {
        this.comment.set(comment);
    }

    public MonitoredProperty<String> commentProperty() {
        return comment;
    }


    public String getQuality() {
        return quality.get();
    }

    public void setQuality(String quality) {
        this.quality.set(quality);
    }

    public MonitoredProperty<String> qualityProperty() {
        return quality;
    }


    public String getChannel() {
        return channel.get();
    }

    public void setChannel(String channel) {
        this.channel.set(channel);
    }

    public MonitoredProperty<String> channelProperty() {
        return channel;
    }


    public Long getEndDate() {
        return endDate.get();
    }

    public void setEndDate(Long endDate) {
        this.endDate.set(endDate);
    }

    public MonitoredProperty<Long> endDateProperty() {
        return endDate;
    }


    public String getChecksum() {
        return checksum.get();
    }

    public void setChecksum(String checksum) {
        this.checksum.set(checksum);
    }

    public MonitoredProperty<String> checksumProperty() {
        return checksum;
    }


    public String getManufacturer() {
        return manufacturer.get();
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer.set(manufacturer);
    }

    public MonitoredProperty<String> manufacturerProperty() {
        return manufacturer;
    }


    public String getModel() {
        return model.get();
    }

    public void setModel(String model) {
        this.model.set(model);
    }

    public MonitoredProperty<String> modelProperty() {
        return model;
    }


    public String getSerialNo() {
        return serialNo.get();
    }

    public void setSerialNo(String serialNo) {
        this.serialNo.set(serialNo);
    }

    public MonitoredProperty<String> serialNoProperty() {
        return serialNo;
    }


    public String getEncoderName() {
        return encoderName.get();
    }

    public void setEncoderName(String encoderName) {
        this.encoderName.set(encoderName);
    }

    public MonitoredProperty<String> encoderNameProperty() {
        return encoderName;
    }


    public SimpleBooleanProperty processedProperty() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed.set(processed);

        this.setFilename(this.getFilename());
    }

    @JsonIgnore
    public Boolean isProcessed() {
        if (dirty.getValue()){ //if it is dirty, it is most certainly not processed
            setProcessed(false);
            return false;
        }
        if (Files.exists(getVhsFileMetadataFilePath())){
            return true;
        }
        return processed.getValue();
    }



    @JsonIgnore
    public Instant getLastModified() {
        return lastModified.get();
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified.set(lastModified);
    }

    public MonitoredProperty<Instant> lastModifiedProperty() {
        return lastModified;
    }

    /**
     * Filenames look like:
     * dr1_digivid.1425196800-2015-03-01-09.00.00_1425200400-2015-03-01-10.00.00.ts
     * @return The new filename
     */
    private String buildFilename() {
        DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd-HH.mm.ss");
        return String.format("%s_digivid_%s-%s_%s-%s.ts",
                getChannel(),
                getStartDate() / 1000L,
                dateFormat.format(getStartDate()),
                getEndDate() / 1000L,
                dateFormat.format(getEndDate()));
    }

    /**
     * This is the heart of the processing functionality.
     * It renames the file to correspond to the specified localProperties and writes a json-file
     *
     * Synchronized to lock commit and preprocess from race conditioning
     */
    public synchronized void commit() {
        //Assume the caller have already validated that all the values are acceptable

        Path oldPath = getVideoFilePath();

        //Create filename to match metadata
        String newName = buildFilename();
        //This is the path to where the new file should be
        Path newPath = oldPath.resolveSibling(newName);
        //Move
        try {
            //Delete the metadata files
            //If it crashes during this commit, the metadata is lost... Just saying
            Files.deleteIfExists(getVhsFileMetadataFilePath());
            Files.deleteIfExists(getTmpFileMetadataFilePath());

            if (!(Files.exists(newPath) && Files.isSameFile(oldPath, newPath))) {
                Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);

                //Update VideoFilePath to point to the moved file
                setFilename(newName);
            }
        } catch (IOException e) {
            Utils.errorDialog("Exception when renaming the data file", e);
        }

        //Checksum
        try (InputStream checksumInputStream = Files.newInputStream(newPath)) {
            setChecksum(DigestUtils.md5Hex(checksumInputStream));
        } catch (IOException e) {
            Utils.errorDialog("Exception when calculating data file checksum", e);
        }

        //Make json metadata
        String vhsFileMetadata = null;
        try {
            vhsFileMetadata = toJson();
        } catch (JsonProcessingException e) {
            Utils.errorDialog("Exception when creating new metadata file", e);
        }

        //Clean existing comments file
        writeFile(vhsFileMetadata, getVhsFileMetadataFilePath());

        try {
            Files.deleteIfExists(getTmpFileMetadataFilePath());
        } catch (IOException e) {
            Utils.errorDialog("Exception when deleting the old metadata file", e);
        }
        setProcessed(true);//Inform watchers that this file is now processed

        markClean();
    }

    private void writeFile(String contents, Path location) {
        try {
            Files.deleteIfExists(location);
        } catch (IOException e) {
            Utils.errorDialog("Exception when deleting the old metadata file", e);
        }

        //Write new comments file
        try {
            Files.write(location, contents.getBytes("UTF-8"));
        } catch (IOException e) {
            Utils.errorDialog("Exception when writing the new metadata file", e);
        }
    }

    /**
     * This preprocesses the videofileobject .
     */
    public synchronized void preprocess() {
        if (dirty.getValue()) {//Only update temp file is anything actually changed

            try { //Delete the comments file, if it exists
                Files.deleteIfExists(getVhsFileMetadataFilePath());
                setProcessed(false); //No longer processed
            } catch (IOException e) {
                Utils.errorDialog("Exception when resetting the processed state", e);
            }

            String jsonMetadata = null;
            try {
                jsonMetadata = toJson();
            } catch (JsonProcessingException e) {
                Utils.errorDialog("Exception when creating new metadata file", e);
            }

            writeFile(jsonMetadata, getTmpFileMetadataFilePath());
            markClean();
        }

    }

    private void markClean() {
        dirty.setValue(false);
    }

    @Override
    public String toString() {
        return "VideoFileObject{" +
                "filesize=" + getFilesize() +
                ", filename=" + getFilename() +
                ", startDate=" + getStartDate() +
                ", vhsLabel=" + getVhsLabel() +
                ", comment=" + getComment() +
                ", quality=" + getQuality() +
                ", channel=" + getChannel() +
                ", endDate=" + getEndDate() +
                ", checksum=" + getChecksum() +
                ", manufacturer=" + getManufacturer() +
                ", model=" + getModel() +
                ", serialNo=" + getSerialNo() +
                ", encoderName=" + getEncoderName() +
                '}';
    }

    @Override
    public int hashCode() {
        return getVideoFilePath().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VideoFileObject)) return false;

        VideoFileObject that = (VideoFileObject) o;

        return getVideoFilePath().equals(that.getVideoFilePath());

    }

    public class MonitoredProperty<T> extends SimpleObjectProperty<T> {


        public MonitoredProperty(Object bean, String name, T initValue) {
            super(bean, name, initValue);

            this.addListener((observable, oldValue, newValue) -> {
                if (ObjectUtils.notEqual(oldValue, newValue)) { //If anything changes, mark the file as dirty
                    dirty.set(true);
                }
            });
        }
    }
}
