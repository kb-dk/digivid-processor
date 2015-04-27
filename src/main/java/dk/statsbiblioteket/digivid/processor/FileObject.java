package dk.statsbiblioteket.digivid.processor;

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
