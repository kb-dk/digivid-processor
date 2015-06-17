package dk.statsbiblioteket.digivid.processor;

import javafx.stage.Stage;
import org.testfx.framework.junit.ApplicationTest;

public class DigividProcessorTest extends ApplicationTest {

    @Override
    public void start(Stage stage) {

        DigividProcessor main = new DigividProcessor();
        DigividProcessor.recordsDir = "src/test/data/sample";
        DigividProcessor.channelCSV = "src/main/config/channels.csv";
        DigividProcessor.player = "/usr/bin/vlc";
        DigividProcessor.localProperties = "src/main/config/localProperties.csv";

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

