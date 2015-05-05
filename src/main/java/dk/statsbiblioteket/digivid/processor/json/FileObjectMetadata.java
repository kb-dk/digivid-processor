package dk.statsbiblioteket.digivid.processor.json;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.google.gson.Gson;

import dk.statsbiblioteket.digivid.processor.FileObject;

/**
 * Class representing a JSON-serializable set of metadata corresponding to a FileObject
 */
public class FileObjectMetadata {

    public FileObjectMetadata() {

    }

    public FileObjectMetadata(FileObject fileObject) {
        this.filename = fileObject.getFilename();
        this.vhsLabel = fileObject.getVhsLabel();
        this.comments = fileObject.getComment();
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
        this.quality = fileObject.getQuality();
    }

    private String filename;

    private String comments;

    private String vhsLabel;

    private String encoderName;

    private Long startDate;

    private Long endDate;

    private String channelLabel;

    private String checksum;

    private String quality;

    private String manufacturer;

    private String model;

    private String serialNo;

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

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

    public String getVhsLabel() {
        return vhsLabel;
    }

    public void setVhsLabel(String vhsLabel) {
        this.vhsLabel = vhsLabel;
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
}
