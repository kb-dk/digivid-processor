package dk.statsbiblioteket.digivid.processor;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
public class VideoFileObject {

    private Path videoFilePath;
    private Path vhsFileMetadataFilePath;

    private String filename;

    private Long startDate;

    private String vhsLabel;

    private String comment;

    private String quality;

    private String channel;

    private Date endDate;

    private String checksum;

    private String manufacturer;

    private String model;

    private String serialNo;

    private String encoderName;

    //private Long startDateLong;

    private Long endDateLong;

    public String toJson() {
        return (new Gson()).toJson(this);
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

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
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
            this.startDate =  videoFileObject.getStartDate();
        }
        if (videoFileObject.getEndDate() != null) {
            this.endDateLong = videoFileObject.getEndDate().getTime();
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
        filename = videoFilePath.getFileName().toString();
        vhsFileMetadataFilePath = path.getParent().resolve(path.getFileName().toString() + ".comments");
        if (Files.exists(vhsFileMetadataFilePath)) {
            final byte[] bytes;
            try {
                bytes = Files.readAllBytes(vhsFileMetadataFilePath);
                VideoFileObject videoFileObject = VideoFileObject.fromJson(new String(bytes, "UTF-8"));
                if (videoFileObject != null) {
                    endDate = toDate(videoFileObject.getEndDateLong());
                    startDate = getStartDate();
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
                //??
            }
            catch (NullPointerException nEx) {
                nEx.printStackTrace();
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

    public Boolean isProcessed() {
        return Files.exists(vhsFileMetadataFilePath);
    }

    /**
     * Filenames look like:
     * dr1_digivid.1425196800-2015-03-01-09.00.00_1425200400-2015-03-01-10.00.00.ts
     * @return
     */
    private String buildFilename() {
        DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd-HH.mm.ss");
        //TODO replace this with proper error handling, input validation
        if (startDate == null) {
            startDate = new Date().getTime();
        }
        if (endDate == null) {
            endDate = new Date();
        }
        String e1 = "" + channel;
        String e2 = "" + startDate/1000L;
        String e3 = dateFormat.format(startDate);
        String e4 = "" + endDate.getTime()/1000L;
        String e5 = dateFormat.format(endDate);
        return e1 + "_digivid_" + e2 + "-" +e3 + "_" + e4 + "-" + e5 + ".ts";

    }

    /**
     * This is the heart of the processing functionality. It renames the file to correspond to the specified localProperties.
     *
     */
    public void commit() {
        setFilename(buildFilename());
        Path newPath = videoFilePath.getParent().resolve(Paths.get(filename));
        try {
            checksum = DigestUtils.md5Hex(Files.newInputStream(videoFilePath));
        } catch (IOException e) {
            //?
        }
        try {
            if (!(Files.exists(newPath) && Files.isSameFile(videoFilePath, newPath))) {
                Files.move(videoFilePath, newPath);
            }
        } catch (IOException e) {
            //??
        }
        try {
            this.encoderName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.encoderName = "unknown";
        }
        String vhsFileMetadata = (new VideoFileObject(this)).toJson();
        Path newVHSFileMetadataPath = newPath.getParent().resolve(newPath.getFileName().toString() + ".comments");
        try {
            Files.delete(vhsFileMetadataFilePath);
        } catch (IOException e) {
            //??
        }
        try {
            Files.write(newVHSFileMetadataPath, vhsFileMetadata.getBytes());
        } catch (IOException e) {
            //?
        }
    }

    /*public Long getStartDateLong() {
        return startDateLong;
    }*/

    public Long getEndDateLong() {
        return endDateLong;
    }

}
