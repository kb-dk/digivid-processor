package dk.statsbiblioteket.digivid.processor;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.GregorianCalendar;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import dk.statsbiblioteket.digivid.processor.json.FileObjectMetadata;

/**
 * Created by csr on 4/14/15.
 */
public class FileObjectImplTest {

    Path dir =  Paths.get("src/test/data/testData1");


    @BeforeMethod
    @AfterMethod
    private void cleanup(ITestResult result) throws IOException {
        if (result.isSuccess()) {
            if (Files.isDirectory(dir)) {
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir);
                for (Path file : directoryStream) {
                    Files.delete(file);
                }
            }
            Files.deleteIfExists(dir);
        }
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
        Files.createDirectories(dir);
        Files.createFile(path);
        FileObjectImpl fileObject = new FileObjectImpl(path);
        fileObject.setStartDate(new GregorianCalendar(1993, 3, 17, 20, 05).getTime());
        fileObject.setEndDate(new GregorianCalendar(1993, 3, 17, 20, 55).getTime());
        fileObject.setChannel("dr5");
        fileObject.commit();
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir, "dr5_*.ts");
        Path tsPath = directoryStream.iterator().next();
        Path commentsPath = tsPath.getParent().resolve(tsPath.getFileName().toString() + ".comments");
        assertTrue(Files.exists(commentsPath));
        FileObjectImpl fileObject1 = new FileObjectImpl(tsPath);
        assertEquals(fileObject.getStartDate(), fileObject1.getStartDate(), "Expect to persist startDate.");
        fileObject1.setChannel("tv2");
        fileObject1.setVhsLabel("What a fine tape you are.");
        fileObject1.setQuality("9: amazing!");
        fileObject1.commit();
        Path newPath = tsPath.getParent().resolve(tsPath.getFileName().toString().replace("dr5", "tv2"));
        assertTrue(Files.exists(newPath));
        Path newComments = newPath.getParent().resolve(newPath.getFileName().toString() + ".comments");
        assertTrue(Files.exists(newComments));
        assertFalse(Files.exists(tsPath));
        assertFalse(Files.exists(commentsPath));
        FileObjectMetadata metadata = new FileObjectMetadata(fileObject1);
        System.out.println(metadata.toJson());
        metadata = FileObjectMetadata.fromJson(new String(Files.readAllBytes(newComments), "UTF-8"));
        System.out.println(metadata.toJson());
    }
}