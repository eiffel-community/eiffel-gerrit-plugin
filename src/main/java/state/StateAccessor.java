package state;

import java.io.File;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.handlers.DataBaseHandler;
import com.ericsson.gerrit.plugins.eiffel.handlers.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.handlers.StateHandler;
import com.ericsson.gerrit.plugins.eiffel.handlers.Table;

public abstract class StateAccessor {
    protected static final Logger LOGGER = LoggerFactory.getLogger(StateHandler.class);
    protected static final String FILE_ENDING = "db";

    protected File pluginDir;
    protected String project;
    protected String branch;
    protected String changeId;

    public StateAccessor(File pluginDir, String project, String branch, String changeId) {
        this(pluginDir, project, branch);
        this.changeId = changeId;
    }

    public StateAccessor(File pluginDir, String project, String branch) {
        this.pluginDir = pluginDir;
        this.project = project;
        this.branch = branch;
    }

    public abstract String getEventId() throws NoSuchElementException;

    public abstract void setState(String eiffelEventId)
            throws NoSuchElementException, ConnectException, SQLException;

    protected String getLastSubmittedEiffelEvent(File pluginDir, String project, String tableColumnName,
            Table tableName)
            throws NoSuchElementException {
        File parentDir = new File(buildParentFilePath(pluginDir, project));
        if (!(parentDir.exists())) {
            return "";
        }

        try {
            String fileName = String.format("%s.%s", project, FILE_ENDING);
            DataBaseHandler dBHandler = new DataBaseHandler(pluginDir, fileName);
            String eventId = dBHandler.getEventID(tableName, tableColumnName);
            LOGGER.info("Fetched old event with id '{}', for project '{}', and branch '{}'", eventId, project,
                    tableColumnName);
            return eventId;
        } catch (Exception e) {
            LOGGER.error("Error while trying to get eiffel event id from database: {}\n{}", e.getMessage(), e);
            // return "";
            throw new NoSuchElementException(
                    "Database did not return any value for this query\n" + "Exception Message:" + e.getMessage());
        }

    }

    protected void setLastSubmittedEiffelEvent(File pluginDir, String project, String tableColumnName,
            String eiffelEvent,
            Table tableName) throws NoSuchElementException, ConnectException, SQLException {
        String fileName = String.format("%s.%s", project, FILE_ENDING);
        DataBaseHandler dBHandler = new DataBaseHandler(pluginDir, fileName);
        String parentPath = buildParentFilePath(pluginDir, project);
        createParentDirsIfNecessary(parentPath);

        String oldEvent = getOldEventId(dBHandler, tableName, tableColumnName);

        if (!oldEvent.isEmpty()) {
            dBHandler.updateInto(tableName, tableColumnName, eiffelEvent);
            LOGGER.info("Replaced old event id '{}' with new event if '{}', for project '{}', and branch '{}'.",
                    oldEvent, eiffelEvent, project, tableColumnName);
        } else {
            dBHandler.insertInto(tableName, tableColumnName, eiffelEvent);
            LOGGER.info("Saved eiffel event with id '{}', for project '{}', and branch '{}'.", eiffelEvent, project,
                    tableColumnName);
        }
    }

    /**
     * Creates parent directories of a project if they don't exist and is included
     * in the project name.
     *
     * @param path
     */
    protected void createParentDirsIfNecessary(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Builds the absolute file path to the parent of a project
     *
     * @param project
     * @return
     */
    protected String buildParentFilePath(File pluginDir, String project) {
        String relativeParentPath = generateRelativeParentPath(project);
        Path absolutePath = Paths.get(pluginDir.getAbsolutePath(), relativeParentPath);
        return absolutePath.toString();
    }

    private String getOldEventId(DataBaseHandler dBHandler, Table tableName, String tableColumnName) {
        try {
            String oldEvent = dBHandler.getEventID(tableName, tableColumnName);
            return oldEvent;
        } catch (Exception e) {
            LOGGER.debug("No previous event was saved, creating a new entry");
            return "";
        }
    }

    private String generateRelativeParentPath(String project) {
        int lastIndexOfSlash = project.lastIndexOf("/");

        String relativeParentPath = "";
        boolean projectContainsParent = lastIndexOfSlash != -1;
        if (projectContainsParent) {
            relativeParentPath = project.substring(0, lastIndexOfSlash);
        }
        return relativeParentPath;
    }
}
