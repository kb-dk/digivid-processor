package dk.statsbiblioteket.digivid.processor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Date;

/**
 * Object representing a digitised file
 */
public class FileObjectImpl implements FileObject {

    private final StringProperty filename = new SimpleStringProperty("");
    private final LongProperty lastmodified = new SimpleLongProperty(0L);

    public FileObjectImpl() {
        this("", 0L);
    }

    public FileObjectImpl(String filename, Long lastmodified) {
        this.filename.setValue(filename);
        this.lastmodified.setValue(lastmodified);
    }


    @Override
    public String getFilename() {
        return filename.get();
    }

    @Override
    public Date getLastmodified() {
        Date date = new Date();
        date.setTime(lastmodified.get());
        return date;
    }

    @Override
    public Boolean isProcessed() {
        return null;
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

    @Override
    public Boolean isProfileCorrect() {
        return null;
    }
}
