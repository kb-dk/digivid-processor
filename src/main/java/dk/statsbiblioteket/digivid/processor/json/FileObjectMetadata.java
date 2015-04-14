package dk.statsbiblioteket.digivid.processor.json;

import com.google.gson.Gson;
import dk.statsbiblioteket.digivid.processor.FileObject;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Class representing a JSON-serializable set of metadata corresponding to a FileObject
 */
public class FileObjectMetadata {

    public FileObjectMetadata() {

    }

    public FileObjectMetadata(FileObject fileObject) {
        this.filename = fileObject.getFilename();
        this.comments = fileObject.getVhsLabel();
        try {
            this.encoderName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.encoderName = "unknown";
        }
        if (fileObject.getStartDate() != null) {
            this.startDate =  fileObject.getStartDate().getTime();
        }
        if (fileObject.getEndDate() != null) {
            this.endDate = fileObject.getEndDate().getTime();
        }
        this.channelLabel = fileObject.getChannel();
        this.checksum = fileObject.getChecksum();
    }

    private String filename;

    private String comments;

    private String encoderName;

    private Long startDate;

    private Long endDate;

    private String channelLabel;

    private String checksum;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getEncoderName() {
        return encoderName;
    }

    public void setEncoderName(String encoderName) {
        this.encoderName = encoderName;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public String getChannelLabel() {
        return channelLabel;
    }

    public void setChannelLabel(String channelLabel) {
        this.channelLabel = channelLabel;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public String toJson() {
        return (new Gson()).toJson(this);
    }

    public static FileObjectMetadata fromJson(String json) {
        return (new Gson()).fromJson(json, FileObjectMetadata.class);
    }

}