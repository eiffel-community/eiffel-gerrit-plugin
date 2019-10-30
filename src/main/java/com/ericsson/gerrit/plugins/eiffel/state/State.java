/*
   Copyright 2019 Ericsson AB.
   For a full list of individual contributors, please see the commit history.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.ericsson.gerrit.plugins.eiffel.state;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.events.EiffelEvent;
import com.ericsson.gerrit.plugins.eiffel.handlers.DataBaseHandler;
import com.ericsson.gerrit.plugins.eiffel.handlers.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.handlers.Table;

public abstract class State {
    protected static final Logger LOGGER = LoggerFactory.getLogger(State.class);
    protected static final String FILE_ENDING = "db";

    protected File pluginDir;

    public State(File pluginDir) {
        this.pluginDir = pluginDir;
    }

    public abstract String getEventId(String project, String tableColumnName) throws NoSuchElementException, ConnectException, FileNotFoundException;

    public abstract void setState(String eiffelEventId, EiffelEvent eiffelEvent)
            throws NoSuchElementException, SQLException, ConnectException;

    protected String getLastSubmittedEiffelEvent(String project, String tableColumnName,
            Table tableName)
            throws NoSuchElementException, FileNotFoundException, ConnectException {
        try {
            File parentDir = new File(buildParentFilePath(pluginDir, project));
            if (!(parentDir.exists())) {
                throw new FileNotFoundException(parentDir.getName() + " could not be created.");
            }

            return getEventId(project, tableColumnName, tableName);
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException(
                    "Database did not return any value for this query\n" + "Exception Message:" + e.getMessage());
        }

    }

    protected void setLastSubmittedEiffelEvent(String project, String tableColumnName,
            String eiffelEvent,
            Table tableName) throws NoSuchElementException, ConnectException, SQLException {
        String fileName = String.format("%s.%s", project, FILE_ENDING);
        DataBaseHandler dBHandler = new DataBaseHandler(pluginDir, fileName);
        String parentPath = buildParentFilePath(pluginDir, project);
        createParentDirsIfNecessary(parentPath);

        saveEventToDatabase(project, tableColumnName, eiffelEvent, tableName, dBHandler);
    }

    private void saveEventToDatabase(String project, String tableColumnName, String eiffelEvent,
            Table tableName, DataBaseHandler dBHandler) throws ConnectException, SQLException {
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

    private String getOldEventId(DataBaseHandler dBHandler, Table tableName, String tableColumnName) throws ConnectException {
        try {
            String oldEvent = dBHandler.getEventID(tableName, tableColumnName);
            return oldEvent;
        } catch (NoSuchElementException e) {
            LOGGER.debug("No previous event was saved, creating a new entry");
            return "";
        }
    }

    private String getEventId(String project, String tableColumnName,
            Table tableName) throws NoSuchElementException, ConnectException {
        String fileName = String.format("%s.%s", project, FILE_ENDING);
        DataBaseHandler dBHandler = new DataBaseHandler(pluginDir, fileName);
        String eventId = dBHandler.getEventID(tableName, tableColumnName);
        LOGGER.info("Fetched old event with id '{}', for project '{}', and branch '{}'", eventId, project,
                tableColumnName);
        return eventId;
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
