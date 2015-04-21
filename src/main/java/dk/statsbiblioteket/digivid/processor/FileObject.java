package dk.statsbiblioteket.digivid.processor;

import com.google.gson.annotations.Expose;

import java.util.Date;

/**
 *
 */
public interface FileObject {

    String getFilename();

    Date getLastmodified();

    Boolean isProcessed();

    String getVhsLabel();

    String getComment();

    String getQuality();

    String getChannel();

    Date getStartDate();

    Date getEndDate();

    String getChecksum();

}
