package dk.statsbiblioteket.digivid.processor;

import com.google.gson.Gson;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.testng.Assert.*;

/**
 * Created by csr on 4/14/15.
 */
public class FileObjectImplTest {

    Path dir =  Paths.get("src/test/data/testData1");


    @BeforeMethod
    @AfterMethod
    private void cleanup() throws IOException {
        if (Files.isDirectory(dir)) {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir);
            for (Path file : directoryStream) {
                Files.delete(file);
            }
        }
        Files.deleteIfExists(dir);
    }

    /**
     * This test creates a video file and specifies its metadata, then checks that we can persist it to disk.
     * It then reopens the file and checks that the metadata is correctly read in again.
     * Finally it changes the filename again and checks that the file is moved, new metadata created, and the old
     * metadata deleted.
     * @throws IOException
     */
    @Test
    public void testCommit() throws IOException {
        Path path = dir.resolve("f1.ts");
        Files.createDirectory(dir);
        Files.createFile(path);
        FileObjectImpl fileObject = new FileObjectImpl(path);
        fileObject.setFilename("foobar.ts");
        final Date startDate = new Date(10, 9, 8);
        fileObject.setStartDate(startDate);
        fileObject.commit();
        assertTrue(Files.exists(dir.resolve("foobar.ts.comments")));
        FileObjectImpl fileObject1 = new FileObjectImpl(dir.resolve("foobar.ts"));
        assertEquals(fileObject.getStartDate(), fileObject1.getStartDate(), "Expect to persist startDate.");
        fileObject1.setFilename("barfoo.ts");
        fileObject1.commit();
        assertTrue(Files.exists(dir.resolve("barfoo.ts.comments")));
        assertTrue(Files.exists(dir.resolve("barfoo.ts")));
        assertFalse(Files.exists(dir.resolve("foobar.ts.comments")));
        assertFalse(Files.exists(dir.resolve("foobar.ts")));
    }
}