package dk.statsbiblioteket.digivid.processor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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

    public FileObjectImpl(Path path) {
        videoFilePath = path;
        metadataFilePath = path.getParent().resolve(path.getFileName().toString() + ".comments");
    }

    @Override
    public String getFilename() {
        return videoFilePath.getFileName().toString();
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
        return null;
    }

    @Override
    public String getQuality() {
        return null;
    }

    @Override
    public String getChannel() {
        return null;
    }

    @Override
    public Date getStartDate() {
        return null;
    }

    @Override
    public Date getEndDate() {
        return null;
    }

    @Override
    public String getChecksum() {
        return null;
    }


}
