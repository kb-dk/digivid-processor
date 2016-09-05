package dk.statsbiblioteket.digivid.processor;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
public class ControllerTest {

    private DigividProcessor main;

    @BeforeMethod
    public void beforeEachMethod() throws Exception {
        main = new DigividProcessor();
        DigividProcessor.recordsDir = Paths.get("src/test/data/sample");
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