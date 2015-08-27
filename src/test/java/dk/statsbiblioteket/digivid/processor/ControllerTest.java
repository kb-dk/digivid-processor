package dk.statsbiblioteket.digivid.processor;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ControllerTest {

    private DigividProcessor main;

    @BeforeMethod
    public void beforeEachMethod() throws Exception {
        main = new DigividProcessor();
        DigividProcessor.recordsDir = "src/test/data/sample";
        DigividProcessor.channelCSV = "src/test/config/channels.csv";
        DigividProcessor.player = "/usr/bin/vlc";
        DigividProcessor.localProperties = "src/test/config/localProperties.csv";
    }

    @AfterMethod
    public void afterEachMethod() throws Exception {

    }

    @Test
    public void testHandleLocalProperties() throws Exception {
        Path newFilePath = Paths.get(DigividProcessor.localProperties);
        try {
            Path parentDir = newFilePath.getParent();
            if (!Files.exists(parentDir))
                Files.createDirectories(parentDir);
            String msg = String.format("%s,%s,%s", "Panasonic", "VHS-3421", "345123-235412");
            Files.write(newFilePath, msg.getBytes("UTF-8"), StandardOpenOption.WRITE, StandardOpenOption.CREATE);

        } catch (IOException ioe) {
            Utils.showErrorDialog("Caught error while writing local properties\n\n", Thread.currentThread(), ioe);
        }
    }

    @Test
    public void testSetDataPath() throws Exception {

    }

    @Test
    public void testLoadFilenames() throws Exception {

    }

    @Test
    public void testCommit() throws Exception {

    }

    @Test
    public void testPlayCurrentFile() throws Exception {

    }
}