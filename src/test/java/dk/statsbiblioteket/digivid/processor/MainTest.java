package dk.statsbiblioteket.digivid.processor;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 *
 */
public class MainTest {

    public void testMain() throws Exception {
        System.setProperty("digivid.config", "src/test/config/digivid-processor.properties");
        DigividProcessor.main(null);
    }
}