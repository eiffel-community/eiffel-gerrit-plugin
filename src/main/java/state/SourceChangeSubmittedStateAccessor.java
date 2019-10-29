package state;

import java.io.File;
import java.net.ConnectException;
import java.sql.SQLException;

import com.ericsson.gerrit.plugins.eiffel.handlers.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.handlers.Table;

public class SourceChangeSubmittedStateAccessor extends StateAccessor {

    public SourceChangeSubmittedStateAccessor(File pluginDir, String project, String branch) {
        super(pluginDir, project, branch);
    }

    @Override
    public String getEventId() throws NoSuchElementException {
        return getLastSubmittedEiffelEvent(pluginDir, project, branch, Table.SCS_TABLE);
    }

    @Override
    public void setState(String eiffelEventId)
            throws NoSuchElementException, ConnectException, SQLException {
        setLastSubmittedEiffelEvent(pluginDir, project, branch, eiffelEventId, Table.SCS_TABLE);
    }
}
