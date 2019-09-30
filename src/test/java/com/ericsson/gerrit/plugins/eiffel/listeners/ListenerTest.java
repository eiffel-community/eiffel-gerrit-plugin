package com.ericsson.gerrit.plugins.eiffel.listeners;

import java.io.File;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.google.gerrit.server.events.Event;

public class ListenerTest {

}

class ListenerTestMock extends AbstractEventListener {

    public ListenerTestMock(String pluginName, File pluginDir) {
        super(pluginName, pluginDir);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onEvent(Event event) {
        // TODO Auto-generated method stub

    }

    @Override
    protected boolean isExpectedGerritEvent(Event gerritEvent) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void prepareAndSendEiffelEvent(Event gerritEvent,
            EiffelPluginConfiguration pluginConfig) {
        // TODO Auto-generated method stub

    }

}