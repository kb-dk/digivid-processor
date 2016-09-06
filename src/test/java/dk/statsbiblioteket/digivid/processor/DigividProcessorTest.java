package dk.statsbiblioteket.digivid.processor;

import javafx.stage.Stage;
import org.testfx.framework.junit.ApplicationTest;

import java.nio.file.Paths;

public class DigividProcessorTest extends ApplicationTest {

    @Override
    public void start(Stage stage) {

        DigividProcessor main = new DigividProcessor();
        DigividProcessor.recordsDir = Paths.get("src/test/data/sample");
        DigividProcessor.channelCSV = Paths.get("src/main/config/channels.csv");
        DigividProcessor.player = Paths.get("/usr/bin/vlc");
        DigividProcessor.localProperties = Paths.get("src/main/config/localProperties.csv");

        try {
            main.start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        stage.show();
    }
	
	/*
    public void should_click_on_file() {
        // given:

        // when:
    	clickOn("emptyTestVideoFile.ts");

        // then:
        FxAssert.verifyThat("#txtFilename", NodeMatchers.hasText("emptyTestVideoFile.ts"));
    }
    */
}

