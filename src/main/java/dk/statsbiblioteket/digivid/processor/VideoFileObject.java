package dk.statsbiblioteket.digivid.processor;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.*;
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
    private Path videoFilePath;
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
        this.filename = videoFileObject.getFilename();
        this.vhsLabel = videoFileObject.getVhsLabel();
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
        videoFilePath = path;
        filename = (videoFilePath.getFileName() != null) ? videoFilePath.getFileName().toString() : "";
        filesize = getFilesize();
        vhsFileMetadataFilePath = path.getParent().resolve((path.getFileName() != null ? path.getFileName().toString() : "Illegal_parameters") + ".comments");
        if (Files.exists(vhsFileMetadataFilePath)) {
            final byte[] bytes;
            try {
                bytes = Files.readAllBytes(vhsFileMetadataFilePath);
                VideoFileObject videoFileObject = VideoFileObject.fromJson(new String(bytes, "UTF-8"));
                if (videoFileObject != null) {
                    endDate = videoFileObject.getEndDate();
                    startDate = videoFileObject.getStartDate();
                    channel = videoFileObject.getChannel();
                    checksum = videoFileObject.getChecksum();
                    vhsLabel = videoFileObject.getVhsLabel();
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
        return vhsLabel;
    }

    public void setVhsLabel(String vhsLabel) {
        this.vhsLabel = vhsLabel;
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
        Long filesize;
        try {
            FileStore fileStore = Files.getFileStore(videoFilePath);
            filesize = fileStore.getTotalSpace();
        } catch (IOException e) {
            filesize = 0L;
        }
        return filesize;
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
        Path newPath;
        newPath = videoFilePath.getParent().resolve(Paths.get(filename));
        try {
            checksum = DigestUtils.md5Hex(Files.newInputStream(videoFilePath));
        } catch (IOException e) {
            log.error("IO exception happened when setting checksum in commit");
            Utils.showErrorDialog(Thread.currentThread(), e);
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
        Path newVHSFileMetadataPath = newPath.getParent().resolve(newPath.getFileName().toString() + ".comments");
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
}
