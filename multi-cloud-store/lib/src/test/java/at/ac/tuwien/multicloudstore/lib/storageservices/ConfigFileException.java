package at.ac.tuwien.multicloudstore.lib.storageservices;

import java.io.IOException;

public class ConfigFileException extends Exception {
    public ConfigFileException(String message, IOException cause) {
        super(message, cause);
    }

    public ConfigFileException(String message) {
        super(message);
    }
}
