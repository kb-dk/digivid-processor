package dk.statsbiblioteket.digivid.processor.json;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.GregorianCalendar;

import org.testng.annotations.Test;

import dk.statsbiblioteket.digivid.processor.FileObjectImpl;

/**
 * Created by csr on 4/14/15.
 */
public class VHSFileMetadataTest {

    @Test
    public void testToJson() throws Exception {
        Path path = Paths.get("/a/b/foobar.ts");
        FileObjectImpl fileObject = new FileObjectImpl(path);
        fileObject.setStartDate(new GregorianCalendar(1992, 01, 23, 18, 00).getTime());
        fileObject.setEndDate(new GregorianCalendar(1992, 01, 23, 21, 30).getTime());
        fileObject.setChannel("tv2");
        fileObject.setQuality("3");
        fileObject.setVhsLabel("This is the finest VHS tape I have ever seen.");
        VHSFileMetadata VHSFileMetadata = new VHSFileMetadata(fileObject);
        System.out.println(VHSFileMetadata.toJson());
    }

    @Test
    public void testFromJson() throws Exception {
        String json = "{\"filename\":\"c\"} ";
        VHSFileMetadata.fromJson(json);
    }
}