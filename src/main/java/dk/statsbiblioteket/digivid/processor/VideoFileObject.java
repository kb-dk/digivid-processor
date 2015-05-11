package dk.statsbiblioteket.digivid.processor;

import dk.statsbiblioteket.digivid.processor.json.VideoFileMetadata;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
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

    private Date startDate;

    private String vhsLabel;

    private String comment;

    private String quality;

    private String channel;

    private Date endDate;

    private String checksum;

    private String manufacturer;

    private String model;

    private String serialNo;

    public VideoFileObject(Path path) {
        videoFilePath = path;
        filename = videoFilePath.getFileName().toString();
        vhsFileMetadataFilePath = path.getParent().resolve(path.getFileName().toString() + ".comments");
        if (Files.exists(vhsFileMetadataFilePath)) {
            final byte[] bytes;
            try {
                bytes = Files.readAllBytes(vhsFileMetadataFilePath);
                VideoFileMetadata videoFileMetadata = VideoFileMetadata.fromJson(new String(bytes, "UTF-8"));
                if (videoFileMetadata != null) {
                    endDate = toDate(videoFileMetadata.getEndDate());
                    startDate = toDate(videoFileMetadata.getStartDate());
                    channel = videoFileMetadata.getChannelLabel();
                    checksum = videoFileMetadata.getChecksum();
                    vhsLabel = videoFileMetadata.getVhsLabel();
                    comment = videoFileMetadata.getComments();
                    quality = videoFileMetadata.getQuality();
                    manufacturer = videoFileMetadata.getManufacturer();
                    model = videoFileMetadata.getModel();
                    serialNo = videoFileMetadata.getSerialNo();
                }
            } catch (IOException e) {
                //??
            }
            catch (NullPointerException nEx) {
                nEx.printStackTrace();
            }
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
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
            startDate = new Date();
        }
        if (endDate == null) {
            endDate = new Date();
        }
        String e1 = "" + channel;
        String e2 = "" + startDate.getTime()/1000L;
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
        String vhsFileMetadata = (new VideoFileMetadata(this)).toJson();
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

}
