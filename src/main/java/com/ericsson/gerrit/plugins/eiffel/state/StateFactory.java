package com.ericsson.gerrit.plugins.eiffel.state;

import java.io.File;

public class StateFactory {
    public static State getStateAccessor(File pluginDir, String eventType) {
        try {
            switch (eventType) {
            case "EiffelSourceChangeCreatedEvent":
                return new SourceChangeCreatedState(pluginDir);
            case "EiffelSourceChangeSubmittedEvent":
                return new SourceChangeSubmittedState(pluginDir);
            }
        } catch (Exception e) {
            // TODO Change this
            return null;
        }
        return null;
    }

}
