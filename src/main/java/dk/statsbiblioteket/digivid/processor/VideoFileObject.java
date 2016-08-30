package dk.statsbiblioteket.digivid.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk7.Jdk7Module;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.commons.codec.digest.DigestUtils;
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
import java.util.Date;

/**
 * Class which beside the properties about the videfile also has methods for renaming the videofile and
 * commiting the class which also writes a json-file
 */
public class VideoFileObject {

    private static Logger log = LoggerFactory.getLogger(VideoFileObject.class);
    private static final String COMMENTS = ".comments";
    private static final String temporary = ".temporary";


    private final MonitoredProperty<Long> filesize;
    private final MonitoredProperty<Path> videoFilePath;
    private final MonitoredProperty<Path> vhsFileMetadataFilePath;
    private final MonitoredProperty<String> filename;
    private final MonitoredProperty<Long> startDate;
    private final MonitoredProperty<String> vhsLabel;
    private final MonitoredProperty<String> comment;
    private final MonitoredProperty<String> quality;
    private final MonitoredProperty<String> channel;
    private final MonitoredProperty<Long> endDate;
    private final MonitoredProperty<String> checksum;
    private final MonitoredProperty<String> manufacturer;
    private final MonitoredProperty<String> model;
    private final MonitoredProperty<String> serialNo;
    private final MonitoredProperty<String> encoderName;


    public VideoFileObject() {
        this(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null);
    }

    public VideoFileObject(String vhsLabel, Path videoFilePath, Path vhsFileMetadataFilePath, String filename, Long filesize, Long startDate, String comment, String quality, String channel, Long endDate, String checksum, String manufacturer, String model, String serialNo, String encoderName) {
        this.vhsLabel = new MonitoredProperty<>(this, "vhsLabel", vhsLabel);
        this.videoFilePath = new MonitoredProperty<>(this, "videoFilePath", videoFilePath);
        this.vhsFileMetadataFilePath = new MonitoredProperty<>(this, "vhsFileMetadataFilePath",
                                                                  vhsFileMetadataFilePath);

        this.filename = new MonitoredProperty<>(this, "filename", filename);

        this.filesize = new MonitoredProperty<>(this, "filesize", filesize);

        this.startDate = new MonitoredProperty<>(this, "startDate", startDate);

        this.comment = new MonitoredProperty<>(this, "comment", comment);

        this.quality = new MonitoredProperty<>(this, "quality", quality);

        this.channel = new MonitoredProperty<>(this, "channel", channel);

        this.endDate = new MonitoredProperty<>(this, "endDate", endDate);

        this.checksum = new MonitoredProperty<>(this, "checksum", checksum);

        this.manufacturer = new MonitoredProperty<>(this, "manufacturer", manufacturer);

        this.model = new MonitoredProperty<>(this, "model", model);

        this.serialNo = new MonitoredProperty<>(this, "serialNo", serialNo);

        this.encoderName = new MonitoredProperty<>(this, "encoderName", encoderName);

    }

    public static VideoFileObject createFromPath(Path path) throws IOException {
        Path vhsFileMetadataFilePath = path.getParent().resolve((path.getFileName() != null ? path.getFileName().toString() : "Illegal_parameters") + ".comments");
        Path tmpMetadataPath = path.getParent().resolve((path.getFileName() != null ? path.getFileName().toString() : "Illegal_parameters") + ".temporary");

        VideoFileObject videoFileObject = null;
        if (Files.exists(vhsFileMetadataFilePath)) {
            byte[] bytes = Files.readAllBytes(vhsFileMetadataFilePath);
            videoFileObject = VideoFileObject.fromJson(new String(bytes,"UTF-8"));

        } else if (Files.exists(tmpMetadataPath)) {
            byte[] bytes = Files.readAllBytes(tmpMetadataPath);
            videoFileObject = VideoFileObject.fromJson(new String(bytes,"UTF-8"));
        } else {
            videoFileObject = new VideoFileObject();
        }

        videoFileObject.videoFilePath.set(path);

        String filename = (path.getFileName() != null) ? path.getFileName().toString() : "";
        videoFileObject.setFilename(filename);

        videoFileObject.setFilesize(path.toFile().length());
        videoFileObject.setVhsFileMetadataFilePath(vhsFileMetadataFilePath);


        return videoFileObject;
    }

    public static VideoFileObject fromJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk7Module());
        VideoFileObject obj = mapper.readValue(json, VideoFileObject.class);
        return obj;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk7Module());

        //Object to JSON in String
        String jsonInString = mapper.writeValueAsString(this);
        return jsonInString;
    }

    @Override
    public boolean equals(Object o) {
        try {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VideoFileObject that = (VideoFileObject) o;

            if (getFilename() != null ? !getFilename().equals(that.getFilename()) : that.getFilename() != null) return false;
            if (getFilesize() != null ? !getFilesize().equals(that.getFilesize()) : that.getFilesize() != null) return false;
            if (getStartDate() != null ? !getStartDate().equals(that.getStartDate()) : that.getStartDate() != null) return false;
            if (getVhsLabel() != null ? !getVhsLabel().equals(that.getVhsLabel()) : that.getVhsLabel() != null)
                return false;
            if (getComment() != null ? !getComment().equals(that.getComment()) : that.getComment() != null) return false;
            if (getQuality() != null ? !getQuality().equals(that.getQuality()) : that.getQuality() != null) return false;
            if (getChannel() != null ? !getChannel().equals(that.getChannel()) : that.getChannel() != null) return false;
            if (getEndDate() != null ? !getEndDate().equals(that.getEndDate()) : that.getEndDate() != null) return false;
            if (getChecksum() != null ? !getChecksum().equals(that.getChecksum()) : that.getChecksum() != null) return false;
            if (getManufacturer() != null ? !getManufacturer().equals(that.getManufacturer()) : that.getManufacturer() != null)
                return false;
            if (getModel() != null ? !getModel().equals(that.getModel()) : that.getModel() != null) return false;
            return getSerialNo() != null ? getSerialNo().equals(that.getSerialNo()) : that.getSerialNo() == null;
        } catch (Exception ex) {
            return false;
        }
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

    public Path getVideoFilePath() {
        return videoFilePath.get();
    }

    public MonitoredProperty<Path> videoFilePathProperty() {
        return videoFilePath;
    }

    public void setVideoFilePath(Path videoFilePath) {
        this.videoFilePath.set(videoFilePath);
    }

    public Path getVhsFileMetadataFilePath() {
        return vhsFileMetadataFilePath.get();
    }

    public MonitoredProperty<Path> vhsFileMetadataFilePathProperty() {
        return vhsFileMetadataFilePath;
    }

    public void setVhsFileMetadataFilePath(Path vhsFileMetadataFilePath) {
        this.vhsFileMetadataFilePath.set(vhsFileMetadataFilePath);
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

    //@ExposeMethodResult("manufacturer")
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
    public Date getLastmodified() {
        FileTime lastModifiedTime;
        try {
            lastModifiedTime = Files.getLastModifiedTime(getVideoFilePath());
        } catch (IOException e) {
            lastModifiedTime = FileTime.fromMillis(0L);
        }
        Date date = new Date();
        date.setTime(lastModifiedTime.toMillis());
        return date;
    }

    /**
     * Filenames look like:
     * dr1_digivid.1425196800-2015-03-01-09.00.00_1425200400-2015-03-01-10.00.00.ts
     * @return The new filename
     */
    private String buildFilename() {
        DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd-HH.mm.ss");
        //TODO replace this with proper error handling, input validation
        if (getStartDate() == null) {
            setStartDate(new Date().getTime());
        }
        if (getEndDate() == null) {
            setEndDate(new Date().getTime());
        }
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
    public void commit() {
        //TODO delete temporary comments file

        Path oldPath = getVideoFilePath().getParent().resolve(Paths.get(getFilename()));
        //Create filename to match metadata
        setFilename(buildFilename());
        //This is the path to where the new file should be
        Path newPath = getVideoFilePath().getParent().resolve(Paths.get(getFilename()));

        //Checksum
        try (InputStream checksumInputStream = Files.newInputStream(getVideoFilePath())) {
            setChecksum(DigestUtils.md5Hex(checksumInputStream));
        } catch (IOException e) {
            log.error("IO exception happened when setting checksum in commit", e);
            Utils.showErrorDialog(Thread.currentThread(), e);
            return;
        }

        //Move
        try {
            if (!(Files.exists(newPath) && Files.isSameFile(oldPath, newPath))) {
                Files.move(getVideoFilePath(), newPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("IO exception happened when moving the file in commit", e);
            Utils.showErrorDialog(Thread.currentThread(), e);
            return;
        }
        //Update VideoFilePath to point to the moved file
        setVideoFilePath(newPath);


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


    }

    private void writeFile(String vhsFileMetadata, Path newVHSFileMetadataPath) {
        try {
            if (Files.exists(getVhsFileMetadataFilePath()))
                Files.delete(getVhsFileMetadataFilePath());
        } catch (IOException e) {
            log.error("IO exception happened when deleting the file in commit", e);
            Utils.showErrorDialog(Thread.currentThread(), e);
            return;
        }

        //Write new comments file
        try {
            Files.write(newVHSFileMetadataPath, vhsFileMetadata.getBytes("UTF-8"));
        } catch (IOException e) {
            log.error("IO exception happened when writing the file in commit", e);
            Utils.showErrorDialog(Thread.currentThread(), e);
            return;
        }
    }

    /**
     * This preprocesses the videofileobject .
     */
    public void preprocess() {
        Path newPath = getVideoFilePath().getParent().resolve(Paths.get(getFilename()));

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

        Path newVHSFileMetadataPath = newPath.getParent().resolve(newPath.getFileName().toString() + temporary);
        writeFile(vhsFileMetadata, newVHSFileMetadataPath);

    }


    //TODO make use of Dirty somewhere
    public class MonitoredProperty<T> extends SimpleObjectProperty<T> {

        SimpleBooleanProperty dirty;

        public MonitoredProperty(Object bean, String name, T initValue) {
            super(bean, name, initValue);
            dirty = new SimpleBooleanProperty(false);
            this.addListener(observable -> dirty.set(true));
        }

        public MonitoredProperty(Object bean, String name) {
            this(bean, name, null);
        }

        public MonitoredProperty(String initialValue) {
            this(null, "");
        }

        public MonitoredProperty() {
            this(null, "", null);
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
               ", videoFilePath=" + getVideoFilePath() +
               ", vhsFileMetadataFilePath=" + getVhsFileMetadataFilePath() +
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
}
