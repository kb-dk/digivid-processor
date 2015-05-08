package dk.statsbiblioteket.digivid.processor.json;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.google.gson.Gson;

import dk.statsbiblioteket.digivid.processor.VideoFileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing a JSON-serializable set of localProperties corresponding to a VideoFileObject
 */
public class VideoFileMetadata {

    private static Logger log = LoggerFactory.getLogger(VideoFileMetadata.class);

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

    public String getQuality() { return quality; }

    public String getComments() {
        return comments;
    }

    public String getVhsLabel() {
        return vhsLabel;
    }

    public Long getStartDate() {
        return startDate;
    }

    public String getChannelLabel() {
        return channelLabel;
    }

    public String getChecksum() {
        return checksum;
    }

    public Long getEndDate() {
        return endDate;
    }

    public String toJson() {
        return (new Gson()).toJson(this);
    }

    public static VideoFileMetadata fromJson(String json) {
        return (new Gson()).fromJson(json, VideoFileMetadata.class);
    }

    public String getManufacturer() { return manufacturer; }

    public String getModel() {
        return model;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public VideoFileMetadata(VideoFileObject videoFileObject) {
        this.filename = videoFileObject.getFilename();
        this.vhsLabel = videoFileObject.getVhsLabel();
        this.comments = videoFileObject.getComment();
        try {
            this.encoderName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.encoderName = "unknown";
        }
        if (videoFileObject.getStartDate() != null) {
            this.startDate =  videoFileObject.getStartDate().getTime();
        }
        if (videoFileObject.getEndDate() != null) {
            this.endDate = videoFileObject.getEndDate().getTime();
        }
        this.channelLabel = videoFileObject.getChannel();
        this.checksum = videoFileObject.getChecksum();
        this.quality = videoFileObject.getQuality();
        this.manufacturer = videoFileObject.getManufacturer();
        this.model = videoFileObject.getModel();
        this.serialNo = videoFileObject.getSerialNo();
    }
}
