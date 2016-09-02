package dk.statsbiblioteket.digivid.processor;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.GregorianCalendar;

import static org.testng.Assert.*;

//import dk.statsbiblioteket.digivid.processor.json.VideoFileMetadata;

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

        //Create test file
        Path testFile = dir.resolve("f1.ts");
        if (!Files.exists(testFile)) {
            Files.createDirectories(dir);
            Files.createFile(testFile);
        }

        //Create videoFile videoFileObject and commit it, so it should rename
        VideoFileObject videoFileObject = VideoFileObject.createFromTS(testFile);
        videoFileObject.setStartDate(new GregorianCalendar(1993, 3, 17, 20, 05).getTime().getTime());
        videoFileObject.setEndDate(new GregorianCalendar(1993, 3, 17, 20, 55).getTime().getTime());
        videoFileObject.setChannel("dr5");
        videoFileObject.commit();

        //Assert that the new filename exists and it has a comments file
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir, "dr5_*.ts");
        Path tsPath = directoryStream.iterator().next();
        Path commentsPath = dir.resolve(tsPath.getFileName().toString() + ".comments");
        assertTrue(Files.exists(commentsPath));

        //Create a new videoObject from the new path, to test that the metadata is read correctly
        VideoFileObject reParsedVideoFileObject = VideoFileObject.createFromTS(tsPath);
        assertEquals(videoFileObject.getStartDate(), reParsedVideoFileObject.getStartDate(), "Expected startDate to be saved");

        //Set new metadata on new reparsedVideoFileObject
        reParsedVideoFileObject.setChannel("tv2");
        reParsedVideoFileObject.setVhsLabel("What a fine tape you are.");
        reParsedVideoFileObject.setQuality("9: amazing!");
        //And commit it, so it renames. The only metadata that changes the filename was the channel set
        reParsedVideoFileObject.commit();


        //This is the new name for the reparsedVideoFileObject
        Path newPath = dir.resolve(tsPath.getFileName().toString().replace("dr5", "tv2"));
        assertTrue(Files.exists(newPath));

        //And the associated comments file
        Path newComments = dir.resolve(newPath.getFileName().toString() + ".comments");
        assertTrue(Files.exists(newComments));

        //The old names should no longer exist
        assertFalse(Files.exists(tsPath));
        assertFalse(Files.exists(commentsPath));

        System.out.println(videoFileObject.toJson());
//        videoFileObject = VideoFileObject.fromJson(new String(Files.readAllBytes(newComments), "UTF-8"));
//        System.out.println(videoFileObject.toJson());
    }
}