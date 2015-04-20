package dk.statsbiblioteket.digivid.processor;

/**
 * Created by csr on 4/20/15.
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

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
