package dk.statsbiblioteket.digivid.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk7.Jdk7Module;
import focusproblem.FocusProblem;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Observable;

import static javafx.collections.FXCollections.observableArrayList;

/**
 * Class which beside the properties about the videfile also has methods for renaming the videofile and
 * commiting the class which also writes a json-file
 */
public class VideoFileObject {

    private static Logger log = LoggerFactory.getLogger(VideoFileObject.class);
    private static final String COMMENTS = ".comments";
    private static final String TEMPORARY = ".temporary";

    //JSon ignored properties
    private Path videoFilePath;
    private Path vhsFileMetadataFilePath;
    private final MonitoredProperty<Instant> lastModified = new MonitoredProperty<>(this, "lastModified", null);

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

    private final List<MonitoredProperty<?>> properties = Collections.unmodifiableList(Arrays.asList(filesize,filename,startDate,vhsLabel,comment,quality,quality,channel,endDate,checksum,manufacturer,model,serialNo,encoderName,lastModified));


    public static VideoFileObject createFromTS(Path path) throws IOException {

        Path vhsFileMetadataFilePath = path.getParent().resolve(path.getFileName().toString() + COMMENTS);

        Path tmpMetadataPath = path.getParent().resolve(path.getFileName().toString() + TEMPORARY);

        VideoFileObject videoFileObject;
        if (Files.exists(vhsFileMetadataFilePath)) {
            videoFileObject = VideoFileObject.fromJson(vhsFileMetadataFilePath);
            videoFileObject.setVhsFileMetadataFilePath(vhsFileMetadataFilePath);
        } else if (Files.exists(tmpMetadataPath)) {
            videoFileObject = VideoFileObject.fromJson(tmpMetadataPath);
            videoFileObject.setVhsFileMetadataFilePath(tmpMetadataPath);
        } else {
            videoFileObject = new VideoFileObject();
            videoFileObject.setVhsFileMetadataFilePath(tmpMetadataPath);
        }

        videoFileObject.setVideoFilePath(path);

        videoFileObject.setFilename(path.getFileName().toString());

        videoFileObject.setFilesize(Files.size(path));
        videoFileObject.setLastModified(Files.getLastModifiedTime(path).toInstant());

        return videoFileObject;
    }

    public static VideoFileObject fromJson(Path commentFile) throws IOException {
        byte[] bytes = Files.readAllBytes(commentFile);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk7Module());
        VideoFileObject obj = mapper.readValue(new String(bytes,"UTF-8"), VideoFileObject.class);
        obj.setVideoFilePath(commentFile.getParent().resolve(obj.getFilename()));
        obj.setVhsFileMetadataFilePath(commentFile);
        return obj;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk7Module());

        //Object to JSON in String
        String jsonInString = mapper.writeValueAsString(this);
        return jsonInString;
    }


    @JsonIgnore
    public Path getVideoFilePath() {
        return videoFilePath;
    }

    public void setVideoFilePath(Path videoFilePath) {
        this.videoFilePath = (videoFilePath);
    }


    @JsonIgnore
    public Path getVhsFileMetadataFilePath() {
        return vhsFileMetadataFilePath;
    }


    public void setVhsFileMetadataFilePath(Path vhsFileMetadataFilePath) {
        this.vhsFileMetadataFilePath = (vhsFileMetadataFilePath);
    }


    public String getFilename() {
        return filename.get();
    }

    public MonitoredProperty<String> filenameProperty() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename.set(filename);
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

    public MonitoredProperty<Long> filesizeProperty() {
        return filesize;
    }

    public void setFilesize(Long filesize) {
        this.filesize.set(filesize);
    }


    public Long getStartDate() {
        return startDate.get();
    }

    public MonitoredProperty<Long> startDateProperty() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate.set(startDate);
    }


    public String getVhsLabel() {
        return vhsLabel.get();
    }

    public MonitoredProperty<String> vhsLabelProperty() {
        return vhsLabel;
    }

    public void setVhsLabel(String vhsLabel) {
        this.vhsLabel.set(vhsLabel);
    }


    public String getComment() {
        return comment.get();
    }

    public MonitoredProperty<String> commentProperty() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment.set(comment);
    }


    public String getQuality() {
        return quality.get();
    }

    public MonitoredProperty<String> qualityProperty() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality.set(quality);
    }


    public String getChannel() {
        return channel.get();
    }

    public MonitoredProperty<String> channelProperty() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel.set(channel);
    }


    public Long getEndDate() {
        return endDate.get();
    }

    public MonitoredProperty<Long> endDateProperty() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate.set(endDate);
    }


    public String getChecksum() {
        return checksum.get();
    }

    public MonitoredProperty<String> checksumProperty() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum.set(checksum);
    }


    public String getManufacturer() {
        return manufacturer.get();
    }

    public MonitoredProperty<String> manufacturerProperty() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer.set(manufacturer);
    }


    public String getModel() {
        return model.get();
    }

    public MonitoredProperty<String> modelProperty() {
        return model;
    }

    public void setModel(String model) {
        this.model.set(model);
    }


    public String getSerialNo() {
        return serialNo.get();
    }

    public MonitoredProperty<String> serialNoProperty() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo.set(serialNo);
    }


    public String getEncoderName() {
        return encoderName.get();
    }

    public MonitoredProperty<String> encoderNameProperty() {
        return encoderName;
    }

    public void setEncoderName(String encoderName) {
        this.encoderName.set(encoderName);
    }


    @JsonIgnore
    public Boolean isProcessed() {
        return Files.exists(getVhsFileMetadataFilePath());
    }


    @JsonIgnore
    public Instant getLastModified() {
        return lastModified.get();
    }

    public MonitoredProperty<Instant> lastModifiedProperty() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified.set(lastModified);
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
     */
    public synchronized void commit() {
        Path oldPath = getVideoFilePath();

        //Create filename to match metadata
        String newName = buildFilename();
        //This is the path to where the new file should be
        Path newPath = oldPath.getParent().resolve(newName);
        //Move
        try {
            if (!(Files.exists(newPath) && Files.isSameFile(oldPath, newPath))) {
                Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                setFilename(newName);
                //Update VideoFilePath to point to the moved file
                setVideoFilePath(newPath);
            }
        } catch (IOException e) {
            log.error("IO exception happened when moving the file in commit", e);
            Utils.showErrorDialog(Thread.currentThread(), e);
            return;
        }


        //Checksum
        try (InputStream checksumInputStream = Files.newInputStream(newPath)) {
            setChecksum(DigestUtils.md5Hex(checksumInputStream));
        } catch (IOException e) {
            log.error("IO exception happened when setting checksum in commit", e);
            Utils.showErrorDialog(Thread.currentThread(), e);
            return;
        }

        //Encoder
        try {
            setEncoderName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            setEncoderName("unknown");
        }

        //Make comments metadata
        String vhsFileMetadata = null;
        try {
            vhsFileMetadata = toJson();
        } catch (JsonProcessingException e) {
            log.error("JSON exception happened when generating the file in commit", e);
            Utils.showErrorDialog(Thread.currentThread(), e);
            return;
        }

        //Clean existing comments file
        Path newVHSFileMetadataPath = newPath.getParent().resolve(newPath.getFileName().toString() + COMMENTS);
        writeFile(vhsFileMetadata, newVHSFileMetadataPath);

        Path oldTempFile = oldPath.getParent().resolve(oldPath.getFileName().toString() + TEMPORARY);
        try {
            Files.deleteIfExists(oldTempFile);
        } catch (IOException e) {
            log.error("IO exception happened when deleting old file", e);
            return;
        }
    }

    private void writeFile(String contents, Path location) {
        try {
            Files.deleteIfExists(location);
        } catch (IOException e) {
            log.error("IO exception happened when deleting the file in commit", e);
            Utils.showErrorDialog(Thread.currentThread(), e);
            return;
        }

        //Write new comments file
        try {
            Files.write(location, contents.getBytes("UTF-8"));
        } catch (IOException e) {
            log.error("IO exception happened when writing the file in commit", e);
            Utils.showErrorDialog(Thread.currentThread(), e);
            return;
        }
    }

    /**
     * This preprocesses the videofileobject .
     */
    public synchronized void preprocess() {
        if (isProcessed()){ //Do not preprocess if the file is allready processed
            return;
        }
        Path newPath = getVideoFilePath().getParent().resolve(Paths.get(getFilename()));

        //Encoder
        try {
            setEncoderName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            setEncoderName("unknown");
        }

        if (isDirty()) {//Only update temp file is anything actually changed

            String vhsFileMetadata = null;
            try {
                vhsFileMetadata = toJson();
            } catch (JsonProcessingException e) {
                log.error("JSON exception happened when generating the file in commit", e);
                Utils.showErrorDialog(Thread.currentThread(), e);
                return;
            }

            Path newVHSFileMetadataPath = newPath.getParent().resolve(newPath.getFileName().toString() + TEMPORARY);
            writeFile(vhsFileMetadata, newVHSFileMetadataPath);
            markClean();
        }

    }

    public boolean isDirty(){
        return properties.stream().anyMatch(MonitoredProperty::isDirty);
    }

    public void markClean() {
        properties.forEach(monitoredProperty -> monitoredProperty.setDirty(false));
    }

    public class MonitoredProperty<T> extends SimpleObjectProperty<T> {

        SimpleBooleanProperty dirty;

        public MonitoredProperty(Object bean, String name, T initValue) {
            super(bean, name, initValue);
            dirty = new SimpleBooleanProperty(false);

            this.addListener((observable, oldValue, newValue) -> {
                if (ObjectUtils.notEqual(oldValue, newValue)) { //If anything changes, mark the field as dirty
                    dirty.set(true);
                }
            });
        }

        public MonitoredProperty(Object bean, String name) {
            this(bean, name, null);
        }

        public boolean isDirty() {
            return dirty.get();
        }

        public void setDirty(boolean newValue) {
            dirty.set(newValue);
        }
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
}
