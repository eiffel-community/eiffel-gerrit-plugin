package com.ericsson.gerrit.plugins.eiffel.handlers;

import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.Event;

public class EiffelEvent {

    private Event listensTo;

    public EiffelEvent() {
        listensTo = new ChangeMergedEvent(null);

    }
}
