package dk.statsbiblioteket.digivid.processor;
import dk.statsbiblioteket.digivid.processor.json.FileObjectMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;

/**
 * Object representing a digitised file
 */
public class FileObjectImpl implements FileObject {

    private Path videoFilePath;
    private Path metadataFilePath;

    private String filename;

    private Date startDate;

    private String vhsLabel;

    private String quality;

    private String channel;

    private Date endDate;

    private String checksum;

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
                endDate =  toDate(fileObjectMetadata.getEndDate());
                startDate = toDate(fileObjectMetadata.getStartDate());
                channel= fileObjectMetadata.getChannelLabel();
                checksum = fileObjectMetadata.getChecksum();
                vhsLabel = fileObjectMetadata.getComments();
            } catch (IOException e) {
                //??
            }
        }
    }

    /**
     * This is the heart of the processing functionality. It renames the file to the set filename and either creates or
     * overwrites the corresponding metadata file.
     */
    public void commit() {
        Path newPath = videoFilePath.getParent().resolve(Paths.get(filename));
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

    public void setFilename(String filename) {
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
        return null;
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

    public void setQuality(String quality) {
        this.quality = quality;
    }
}
