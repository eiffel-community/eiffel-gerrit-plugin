package com.ericsson.gerrit.plugins.eiffel.handlers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateHandler.class);
    private static final String DISABLED_RESPONSE = "No plugin dir given. Saving last event id is disabled for tests.";
    private static final String FILE_ENDING = "db";
    private final File pluginDir;

    /**
     * Constructor for the StateHandler class. in case the stateHandler is initiated
     * with empty string or null the StateHandeling will be disabled.
     *
     * @param pluginDir
     */
    public StateHandler(final File pluginDir) {
        this.pluginDir = pluginDir;
    }

    /**
     * This function returns the last set scm change eiffel event id stored based on
     * the branch and specific project.
     *
     * @param project
     * @param branch
     * @return
     */
    public String getLastSourceChangeSubmittedEiffelEvent(String project, String branch) {
        return getLastCreatedEiffelEvent(project, branch, Table.SCS_TABLE);
    }

    /**
     * This function sets an scm change eiffel event id for a specific project and
     * branch.
     *
     * @param project
     * @param branch
     * @param eiffelEvent
     */
    public void setLastSourceChangeSubmittedEiffelEvent(String project, String branch, String eiffelEvent) {
        setLastSubmittedEiffelEvent(project, branch, eiffelEvent, Table.SCS_TABLE);

    }

    /**
     * This function sets an SCC eiffel event id for a specific project and branch.
     *
     * @param project
     * @param changeId
     * @param eiffelEvent
     */
    public void setLastSourceChangeCreatedEiffelEvent(String project, String changeId, String eiffelEvent) {
        setLastSubmittedEiffelEvent(project, changeId, eiffelEvent, Table.SCC_TABLE);
    }

    /**
     * This function returns the last set SCC eiffel event id stored based on the
     * changeId and specific project.
     *
     * @param project
     * @param changeId
     * @return
     */
    public String getLastSourceChangeCreatedEiffelEvent(String project, String changeId) {
        return getLastCreatedEiffelEvent(project, changeId, Table.SCC_TABLE);
    }

    /**
     * Builds the absolute file path to the parent of a project
     *
     * @param project
     * @return
     */
    private String buildParentFilePath(String project) {
        String relativeParentPath = generateRelativeParentPath(project);
        Path absolutePath = Paths.get(pluginDir.getAbsolutePath(), relativeParentPath);
        return absolutePath.toString();
    }

    /**
     * Creates parent directories of a project if they don't exist and is included
     * in the project name.
     *
     * @param path
     */
    private void createParentDirsIfNecessary(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    // /**
    // * returns true or false depending if the StateHandler is disabled or not.
    // *
    // * @return
    // */
    // private boolean isDisabled() {
    // if (pluginDir == null || pluginDir.toString().isEmpty()) {
    // LOGGER.info(DISABLED_RESPONSE);
    // return true;
    // }
    // return false;
    // }

    private String getLastCreatedEiffelEvent(String project, String tableColumnName, Table tableName) {
        // if (isDisabled()) {
        // return "";
        // }

        File parentDir = new File(buildParentFilePath(project));
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
            throw new RuntimeException("Database did not return any value for this query");
        }

    }

    private void setLastSubmittedEiffelEvent(String project, String tableColumnName, String eiffelEvent,
            Table tableName) {
        // if (isDisabled()) {
        // return;
        // }

        DataBaseHandler dBHandler;
        try {
            String parentPath = buildParentFilePath(project);
            createParentDirsIfNecessary(parentPath);

            String fileName = String.format("%s.%s", project, FILE_ENDING);
            dBHandler = new DataBaseHandler(pluginDir, fileName);
            String oldEvent = dBHandler.getEventID(tableName, tableColumnName);
            if (!oldEvent.isEmpty()) {
                dBHandler.updateInto(Table.SCS_TABLE, tableColumnName, eiffelEvent);
                LOGGER.info("Replaced old event id '{}' with new event if '{}', for project '{}', and branch '{}'.",
                        oldEvent, eiffelEvent, project, tableColumnName);
            } else {
                dBHandler.insertInto(tableName, tableColumnName, eiffelEvent);
                LOGGER.info("Saved eiffel event with id '{}', for project '{}', and branch '{}'.", eiffelEvent, project,
                        tableColumnName);
            }
        } catch (Exception e) {
            LOGGER.error("Error while trying to insert eiffel event id into database: {}\n{}", e.getMessage(), e);
            throw new RuntimeException("Database did not return any value for this query");
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
