package com.ericsson.gerrit.plugins.eiffel.state;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.sql.SQLException;

import com.ericsson.gerrit.plugins.eiffel.events.EiffelEvent;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeSubmittedEvent;
import com.ericsson.gerrit.plugins.eiffel.handlers.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.handlers.Table;

public class SourceChangeSubmittedState extends State {

    public SourceChangeSubmittedState(File pluginDir) {
        super(pluginDir);
    }

    @Override
    public String getEventId(String project, String branch) throws NoSuchElementException, ConnectException, FileNotFoundException {
        return getLastSubmittedEiffelEvent(project, branch, Table.SCS_TABLE);
    }

    @Override
    public void setState(String eiffelEventId, EiffelEvent eiffelEvent)
            throws NoSuchElementException, ConnectException, SQLException {
        EiffelSourceChangeSubmittedEvent eiffelSourceChangeSubmittedEvent = (EiffelSourceChangeSubmittedEvent) eiffelEvent;
        String projectName = eiffelSourceChangeSubmittedEvent.eventParams.data.gitIdentifier.repoName;
        String branch = eiffelSourceChangeSubmittedEvent.eventParams.data.gitIdentifier.branch;

        setLastSubmittedEiffelEvent(projectName, branch, eiffelEventId, Table.SCS_TABLE);
    }
}
