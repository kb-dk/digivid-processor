package dk.statsbiblioteket.digivid.processor;

import com.google.gson.Gson;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
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
    private final String processed = ".comments";
    private final String temporary = ".temporary";
    private final MonitoredSimpleStringProperty vhsLabelProperty;
    public Path videoFilePath;
    private Path vhsFileMetadataFilePath;
    private String filename;
    private Long filesize;
    private Long startDate;
    private String vhsLabel;
    private String comment;
    private String quality;
    private String channel;
    private Long endDate;
    private String checksum;
    private String manufacturer;
    private String model;
    private String serialNo;
    private String encoderName;

    public VideoFileObject(VideoFileObject videoFileObject) {
        vhsLabelProperty = new MonitoredSimpleStringProperty(this, "title");
        this.filename = videoFileObject.getFilename();
        this.filesize = videoFileObject.getFilesize();
        this.vhsLabelProperty.setValue(videoFileObject.getVhsLabel());
        this.comment = videoFileObject.getComment();
        try {
            this.encoderName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.encoderName = "unknown";
        }
        if (videoFileObject.getStartDate() != null) {
            this.startDate = videoFileObject.getStartDate();
        }
        if (videoFileObject.getEndDate() != null) {
            this.endDate = videoFileObject.getEndDate();
        }
        this.channel = videoFileObject.getChannel();
        this.checksum = videoFileObject.getChecksum();
        this.quality = videoFileObject.getQuality();
        this.manufacturer = videoFileObject.getManufacturer();
        this.model = videoFileObject.getModel();
        this.serialNo = videoFileObject.getSerialNo();
    }

    public VideoFileObject(Path path) {
        vhsLabelProperty = new MonitoredSimpleStringProperty(this, "title");
        videoFilePath = path;
        filename = (videoFilePath.getFileName() != null) ? videoFilePath.getFileName().toString() : "";
        filesize = (this.getFilesize() != null) ? this.getFilesize() : 0L;
        vhsFileMetadataFilePath = path.getParent().resolve((path.getFileName() != null ? path.getFileName().toString() : "Illegal_parameters") + ".comments");
        Path tmpMetadataPath = path.getParent().resolve((path.getFileName() != null ? path.getFileName().toString() : "Illegal_parameters") + ".temporary");
        if (Files.exists(vhsFileMetadataFilePath)) {
            assignMetadata(vhsFileMetadataFilePath);
        } else if (Files.exists(tmpMetadataPath)) {
            assignMetadata(tmpMetadataPath);
        }
    }

    private static Date toDate(Long l) {
        if (l == null) {
            return null;
        }
        Date date = new Date();
        date.setTime(l);
        return date;
    }

    public static VideoFileObject fromJson(String json) {
        return (new Gson()).fromJson(json, VideoFileObject.class);
    }

    @Override
    public boolean equals(Object o) {
        try {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VideoFileObject that = (VideoFileObject) o;

            if (temporary != null ? !temporary.equals(that.temporary) : that.temporary != null) return false;
            if (filename != null ? !filename.equals(that.filename) : that.filename != null) return false;
            if (filesize != null ? !filesize.equals(that.filesize) : that.filesize != null) return false;
            if (startDate != null ? !startDate.equals(that.startDate) : that.startDate != null) return false;
            if (getVhsLabel() != null ? !getVhsLabel().equals(that.getVhsLabel()) : that.getVhsLabel() != null)
                return false;
            if (comment != null ? !comment.equals(that.comment) : that.comment != null) return false;
            if (quality != null ? !quality.equals(that.quality) : that.quality != null) return false;
            if (channel != null ? !channel.equals(that.channel) : that.channel != null) return false;
            if (endDate != null ? !endDate.equals(that.endDate) : that.endDate != null) return false;
            if (checksum != null ? !checksum.equals(that.checksum) : that.checksum != null) return false;
            if (manufacturer != null ? !manufacturer.equals(that.manufacturer) : that.manufacturer != null)
                return false;
            if (model != null ? !model.equals(that.model) : that.model != null) return false;
            return serialNo != null ? serialNo.equals(that.serialNo) : that.serialNo == null;
        } catch (Exception ex) {
            return false;
        }
    }

    private void assignMetadata(Path metadataPath) {
        final byte[] bytes;
        try {
            bytes = Files.readAllBytes(metadataPath);
            VideoFileObject videoFileObject = VideoFileObject.fromJson(new String(bytes, "UTF-8"));
            if (videoFileObject != null) {
                endDate = videoFileObject.getEndDate();
                startDate = videoFileObject.getStartDate();
                channel = videoFileObject.getChannel();
                checksum = videoFileObject.getChecksum();
                vhsLabelProperty.setValue(videoFileObject.vhsLabel);
                comment = videoFileObject.getComment();
                quality = videoFileObject.getQuality();
                manufacturer = videoFileObject.getManufacturer();
                model = videoFileObject.getModel();
                serialNo = videoFileObject.getSerialNo();
            }
        } catch (IOException e) {
            log.error("IO exception happened in VideoFileObject(Path path)");
            Utils.showErrorDialog(Thread.currentThread(), e);
        } catch (NullPointerException nEx) {
            log.error("Null pointer exception happened in VideoFileObject(Path path))");
            Utils.showErrorDialog(Thread.currentThread(), nEx);
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public String getVhsLabel() {
        return vhsLabelProperty.getValue();
    }

    public void setVhsLabel(String vhsLabel) {
        this.vhsLabelProperty.set(vhsLabel);
    }

    public MonitoredSimpleStringProperty vhsLabelProperty() {
        return this.vhsLabelProperty;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    //Even though the compiler tells that it can be removed, it cannot because when removing it the processed marks
    //in the file list overview disappears
    public Boolean isProcessed() {
        return Files.exists(vhsFileMetadataFilePath);
    }

    public String toJson() {
        return (new Gson()).toJson(this);
    }

    public Date getLastmodified() {
        FileTime lastModifiedTime;
        try {
            lastModifiedTime = Files.getLastModifiedTime(videoFilePath);
        } catch (IOException e) {
            lastModifiedTime = FileTime.fromMillis(0L);
        }
        Date date = new Date();
        date.setTime(lastModifiedTime.toMillis());
        return date;
    }

    public Long getFilesize() {
        final long K = 1024;
        try {
            return videoFilePath.toFile().length() / K;
        } catch (Exception e) {
            return 0L;
        }
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
        setFilename(buildFilename());
        generateJson(processed);
    }

    /**
     * This preprocesses the videofileobject .
     */
    public void preprocess() {
        generateJson(temporary);
    }

    private void generateJson(String jsonType) {
        Path newPath;
        newPath = videoFilePath.getParent().resolve(Paths.get(filename));
        if (jsonType.equals(processed)) {
            InputStream checksumInputStream = null;
            try {
                checksumInputStream = Files.newInputStream(videoFilePath);
                checksum = DigestUtils.md5Hex(checksumInputStream);
            } catch (IOException e) {
                log.error("IO exception happened when setting checksum in commit");
                Utils.showErrorDialog(Thread.currentThread(), e);
            } finally {
                try {
                    if (checksumInputStream != null)
                        checksumInputStream.close();
                } catch (Exception ex) {
                    Utils.showWarning("An error happened while attempting to find the checksum for " + filename);
                }
            }
        }
        try {
            if (!(Files.exists(newPath) && Files.isSameFile(videoFilePath, newPath))) {
                Files.move(videoFilePath, newPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("IO exception happened when moving the file in commit");
            Utils.showErrorDialog(Thread.currentThread(), e);
        }
        try {
            this.encoderName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.encoderName = "unknown";
        }
        String vhsFileMetadata = new VideoFileObject(this).toJson();
        Path newVHSFileMetadataPath = newPath.getParent().resolve(newPath.getFileName().toString() + jsonType);
        try {
            if (Files.exists(vhsFileMetadataFilePath))
                Files.delete(vhsFileMetadataFilePath);
        } catch (IOException e) {
            log.error("IO exception happened when deleting the file in commit");
            Utils.showErrorDialog(Thread.currentThread(), e);
        }
        try {
            Files.write(newVHSFileMetadataPath, vhsFileMetadata.getBytes("UTF-8"));
        } catch (IOException e) {
            log.error("IO exception happened when writing the file in commit");
            Utils.showErrorDialog(Thread.currentThread(), e);
        }
    }

    public class MonitoredSimpleStringProperty extends SimpleStringProperty {

        SimpleBooleanProperty dirty;

        public MonitoredSimpleStringProperty(Object bean, String name, String initValue) {
            super(bean, name, initValue);
            dirty = new SimpleBooleanProperty(false);
            this.addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    dirty.set(true);
                }
            });
        }

        public MonitoredSimpleStringProperty(Object bean, String name) {
            this(bean, name, "");
        }

        public MonitoredSimpleStringProperty(String initialValue) {
            this(null, "");
        }

        public MonitoredSimpleStringProperty() {
            this(null, "", "");
        }

        public boolean isDirty() {
            return dirty.get();
        }

        public void setDirty(boolean newValue) {
            dirty.set(newValue);
        }
    }
}
