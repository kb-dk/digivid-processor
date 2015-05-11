package dk.statsbiblioteket.digivid.processor;

/**
 * Class for GUI presentation of the channel
 */
public class Channel {
    String channelName;
    String displayName;
    String colour;

    public Channel(String channelName, String displayName, String colour) {
        this.channelName = channelName;
        this.displayName = displayName;
        this.colour = colour;
    }

    public String getChannelName() {
        return channelName;
    }

}
