package state;

import java.io.File;
import java.net.ConnectException;
import java.sql.SQLException;

import com.ericsson.gerrit.plugins.eiffel.handlers.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.handlers.Table;

public class SourceChangeCreatedStateAccessor extends StateAccessor {

    public SourceChangeCreatedStateAccessor(File pluginDir, String project, String branch, String changeId) {
        super(pluginDir, project, branch, changeId);
    }

    @Override
    public String getEventId() throws NoSuchElementException {
        return getLastSubmittedEiffelEvent(pluginDir, project, branch, Table.SCS_TABLE);
    }

    @Override
    public void setState(String eiffelEventId)
            throws NoSuchElementException, ConnectException, SQLException {
        setLastSubmittedEiffelEvent(pluginDir, project, changeId, eiffelEventId, Table.SCC_TABLE);
    }
}
