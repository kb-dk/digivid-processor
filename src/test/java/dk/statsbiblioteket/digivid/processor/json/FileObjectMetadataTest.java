package dk.statsbiblioteket.digivid.processor.json;

import com.google.gson.Gson;
import dk.statsbiblioteket.digivid.processor.FileObject;
import dk.statsbiblioteket.digivid.processor.FileObjectImpl;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.testng.Assert.*;

/**
 * Created by csr on 4/14/15.
 */
public class FileObjectMetadataTest {

    @Test
    public void testToJson() throws Exception {
        Path path = Paths.get("/a/b/c");
        FileObjectImpl fileObject = new FileObjectImpl(path);
        fileObject.setStartDate(new Date());
        FileObjectMetadata fileObjectMetadata = new FileObjectMetadata(fileObject);
        System.out.println(fileObjectMetadata.toJson());
    }

    @Test
    public void testFromJson() throws Exception {
        String json = "{\"filename\":\"c\"} ";
        FileObjectMetadata.fromJson(json);
    }
}