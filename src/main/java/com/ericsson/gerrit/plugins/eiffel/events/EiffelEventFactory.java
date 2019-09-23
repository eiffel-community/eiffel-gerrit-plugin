package com.ericsson.gerrit.plugins.eiffel.events;

import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.PatchSetCreatedEvent;

public class EiffelEventFactory {

    public static EiffelEvent create(Event gerritEvent) {
        EiffelEvent eiffelEvent = null;
        String gerritEventType = gerritEvent.getClass().getSimpleName();
        switch (gerritEventType) {
        case "PatchSetCreatedEvent":
            eiffelEvent = new EiffelSourceChangeCreatedEvent((PatchSetCreatedEvent) gerritEvent);
            break;
        case "ChangeMergedEvent":
            eiffelEvent = new EiffelSourceChangeSubmittedEvent((ChangeMergedEvent) gerritEvent);
            break;
        }
        return eiffelEvent;
    }
}
