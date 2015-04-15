package dk.statsbiblioteket.digivid.processor.json;

import com.google.gson.Gson;
import dk.statsbiblioteket.digivid.processor.FileObject;
import dk.statsbiblioteket.digivid.processor.FileObjectImpl;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.testng.Assert.*;

/**
 * Created by csr on 4/14/15.
 */
public class FileObjectMetadataTest {

    @Test
    public void testToJson() throws Exception {
        Path path = Paths.get("/a/b/foobar.ts");
        FileObjectImpl fileObject = new FileObjectImpl(path);
        fileObject.setStartDate(new GregorianCalendar(1992, 01, 23, 18, 00).getTime());
        fileObject.setEndDate(new GregorianCalendar(1992, 01, 23, 21, 30).getTime());
        fileObject.setChannel("tv2");
        fileObject.setQuality("3");
        fileObject.setVhsLabel("This is the finest VHS tape I have ever seen.");
        FileObjectMetadata fileObjectMetadata = new FileObjectMetadata(fileObject);
        System.out.println(fileObjectMetadata.toJson());
    }

    @Test
    public void testFromJson() throws Exception {
        String json = "{\"filename\":\"c\"} ";
        FileObjectMetadata.fromJson(json);
    }
}