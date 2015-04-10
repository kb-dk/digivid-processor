package dk.statsbiblioteket.digivid.processor;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Object representing a digitised file
 */
public class FileObject {

    private final SimpleStringProperty filename = new SimpleStringProperty("");
    private final LongProperty lastmodified = new SimpleLongProperty(0L);

    public FileObject() {
        this("", 0L);
    }

    public FileObject(String filename, Long lastmodified) {
        this.filename.setValue(filename);
        this.lastmodified.setValue(lastmodified);
    }

    public String getFilename() {
        return filename.get();
    }

    public SimpleStringProperty filenameProperty() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename.set(filename);
    }

    public long getLastmodified() {
        return lastmodified.get();
    }

    public LongProperty lastmodifiedProperty() {
        return lastmodified;
    }

    public void setLastmodified(long lastmodified) {
        this.lastmodified.set(lastmodified);
    }
}
