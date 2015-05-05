package dk.statsbiblioteket.digivid.processor;
import dk.statsbiblioteket.digivid.processor.json.FileObjectMetadata;
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
 * Object representing a digitised file. An instance of the class is created by loading a video file via the constructor.
 * If a metadata (.comments) file is found then this is also loaded.
 *
 * The commit() method saves any changes - renaming the file according to the metadata information and either creating
 * a corresponding metadata file.
 */
public class FileObjectImpl implements FileObject {

    private Path videoFilePath;
    private Path metadataFilePath;

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

    private static Date toDate(Long l) {
        if (l == null) {
            return null;
        }
        Date date = new Date();
        date.setTime(l);
        return date;
    }

    public FileObjectImpl(Path path) {
        videoFilePath = path;
        filename = videoFilePath.getFileName().toString();
        metadataFilePath = path.getParent().resolve(path.getFileName().toString() + ".comments");
        if (Files.exists(metadataFilePath)) {
            final byte[] bytes;
            try {
                bytes = Files.readAllBytes(metadataFilePath);
                FileObjectMetadata fileObjectMetadata = FileObjectMetadata.fromJson(new String(bytes, "UTF-8"));
                if (fileObjectMetadata != null) {
                    endDate = toDate(fileObjectMetadata.getEndDate());
                    startDate = toDate(fileObjectMetadata.getStartDate());
                    channel = fileObjectMetadata.getChannelLabel();
                    checksum = fileObjectMetadata.getChecksum();
                    vhsLabel = fileObjectMetadata.getVhsLabel();
                    comment = fileObjectMetadata.getComments();
                    quality = fileObjectMetadata.getQuality();
                    manufacturer = fileObjectMetadata.getManufacturer();
                    model = fileObjectMetadata.getModel();
                    serialNo = fileObjectMetadata.getSerialNo();
                }
            } catch (IOException e) {
                //??
            }
            catch (NullPointerException nEx) {
                nEx.printStackTrace();
            }
        }
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
     * This is the heart of the processing functionality. It renames the file to correspond to the specified metadata.
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
        String metadata = (new FileObjectMetadata(this)).toJson();
        Path newMetadataPath = newPath.getParent().resolve(newPath.getFileName().toString() + ".comments");
        try {
            Files.delete(metadataFilePath);
        } catch (IOException e) {
            //??
        }
        try {
            Files.write(newMetadataPath, metadata.getBytes());
        } catch (IOException e) {
            //?
        }
    }

    @Override
    public String getFilename() {
        return filename;
    }

    private void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
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

    @Override
    public Boolean isProcessed() {
        return Files.exists(metadataFilePath);
    }

    @Override
    public String getVhsLabel() {
        return vhsLabel;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public String getQuality() {
        return quality;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public String getChecksum() {
        return checksum;
    }

    @Override
    public String getManufacturer() {
        return manufacturer;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public String getSerialNo() {
        return serialNo;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setVhsLabel(String vhsLabel) {
        this.vhsLabel = vhsLabel;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    public void setQuality(String quality) {
        this.quality = quality;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }
}
