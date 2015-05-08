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

import dk.statsbiblioteket.digivid.processor.json.VideoFileMetadata;

/**
 * Created by csr on 4/14/15.
 */
public class VideoFileObjectTest {

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
     * This test creates a video file and specifies its localProperties, then checks that we can persist it to disk.
     * It then reopens the file and checks that the localProperties is correctly read in again.
     * Finally it changes the filename again and checks that the file is moved, new localProperties created, and the old
     * localProperties deleted.
     * @throws IOException
     */
    @Test
    public void testCommit() throws IOException {
        Path path = dir.resolve("f1.ts");
        Files.createDirectories(dir);
        Files.createFile(path);
        VideoFileObject videoFileObject = new VideoFileObject(path);
        videoFileObject.setStartDate(new GregorianCalendar(1993, 3, 17, 20, 05).getTime());
        videoFileObject.setEndDate(new GregorianCalendar(1993, 3, 17, 20, 55).getTime());
        videoFileObject.setChannel("dr5");
        videoFileObject.commit();
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir, "dr5_*.ts");
        Path tsPath = directoryStream.iterator().next();
        Path commentsPath = tsPath.getParent().resolve(tsPath.getFileName().toString() + ".comments");
        assertTrue(Files.exists(commentsPath));
        VideoFileObject videoFileObject1 = new VideoFileObject(tsPath);
        assertEquals(videoFileObject.getStartDate(), videoFileObject1.getStartDate(), "Expect to persist startDate.");
        videoFileObject1.setChannel("tv2");
        videoFileObject1.setVhsLabel("What a fine tape you are.");
        videoFileObject1.setQuality("9: amazing!");
        videoFileObject1.commit();
        Path newPath = tsPath.getParent().resolve(tsPath.getFileName().toString().replace("dr5", "tv2"));
        assertTrue(Files.exists(newPath));
        Path newComments = newPath.getParent().resolve(newPath.getFileName().toString() + ".comments");
        assertTrue(Files.exists(newComments));
        assertFalse(Files.exists(tsPath));
        assertFalse(Files.exists(commentsPath));
        VideoFileMetadata videoFileMetadata = new VideoFileMetadata(videoFileObject1);
        System.out.println(videoFileMetadata.toJson());
        videoFileMetadata = VideoFileMetadata.fromJson(new String(Files.readAllBytes(newComments), "UTF-8"));
        System.out.println(videoFileMetadata.toJson());
    }
}