package com.ericsson.gerrit.plugins.eiffel.state;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.sql.SQLException;

import com.ericsson.gerrit.plugins.eiffel.events.EiffelEvent;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeCreatedEvent;
import com.ericsson.gerrit.plugins.eiffel.handlers.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.handlers.Table;

public class SourceChangeCreatedState extends State {

    public SourceChangeCreatedState(File pluginDir) {
        super(pluginDir);
    }

    @Override
    public String getEventId(String project, String changeId) throws NoSuchElementException, ConnectException, FileNotFoundException {
        return getLastSubmittedEiffelEvent(project, changeId, Table.SCC_TABLE);
    }

    @Override
    public void setState(String eiffelEventId, EiffelEvent eiffelEvent)
            throws NoSuchElementException, ConnectException, SQLException {
        EiffelSourceChangeCreatedEvent eiffelSourceChangeCreatedEvent = (EiffelSourceChangeCreatedEvent) eiffelEvent;
        String projectName = eiffelSourceChangeCreatedEvent.eventParams.data.gitIdentifier.repoName;
        String changeId = eiffelSourceChangeCreatedEvent.eventParams.data.change.id;

        setLastSubmittedEiffelEvent(projectName, changeId, eiffelEventId, Table.SCC_TABLE);
    }
}
