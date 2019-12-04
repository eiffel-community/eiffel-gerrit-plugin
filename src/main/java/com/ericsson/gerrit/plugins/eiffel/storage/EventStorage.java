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

package com.ericsson.gerrit.plugins.eiffel.storage;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelEvent;
import com.ericsson.gerrit.plugins.eiffel.exceptions.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.handlers.DatabaseHandler;
import com.ericsson.gerrit.plugins.eiffel.handlers.Table;

public abstract class EventStorage {
    protected static final Logger LOGGER = LoggerFactory.getLogger(EventStorage.class);
    protected EiffelPluginConfiguration pluginConfig;

    public EventStorage(final EiffelPluginConfiguration pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public abstract String getEventId(String project, String searchCriteria)
            throws NoSuchElementException, ConnectException, FileNotFoundException;

    public abstract void saveEventId(String eiffelEventId, EiffelEvent eiffelEvent)
            throws NoSuchElementException, SQLException, ConnectException;

    protected String getLastSavedEiffelEvent(final String project, final String searchCriteria, final Table tableName)
            throws NoSuchElementException, FileNotFoundException, ConnectException {
        return getEventId(project, searchCriteria, tableName);
    }

    protected void saveEiffelEventId(final String searchCriteria, final String eiffelEventId,
            final Table tableName)
            throws NoSuchElementException, ConnectException, SQLException {
        final String project = pluginConfig.getProject();
        final DatabaseHandler dBHandler = new DatabaseHandler(pluginConfig.getPluginDirectoryPath(), project);
        final String oldEventId = getOldEventId(dBHandler, tableName, searchCriteria);

        if (!oldEventId.isEmpty()) {
            dBHandler.updateInto(tableName, searchCriteria, eiffelEventId);
            LOGGER.info(
                    "Replaced old event id '{}' with new event if '{}', for project '{}', and searchCriteria '{}'.",
                    oldEventId, eiffelEventId, project, searchCriteria);
        } else {
            dBHandler.insertInto(tableName, searchCriteria, eiffelEventId);
            LOGGER.info(
                    "Saved eiffel event with id '{}', for project '{}', and searchCriteria '{}'.",
                    eiffelEventId, project,
                    searchCriteria);
        }
    }

    private String getOldEventId(final DatabaseHandler dBHandler, final Table tableName, final String searchCriteria)
            throws ConnectException {
        try {
            final String oldEvent = dBHandler.getEventID(tableName, searchCriteria);
            return oldEvent;
        } catch (final NoSuchElementException e) {
            LOGGER.debug("No old event found");
            return "";
        }
    }

    private String getEventId(final String project, final String searchCriteria, final Table tableName)
            throws NoSuchElementException, ConnectException {
        final DatabaseHandler dBHandler = new DatabaseHandler(pluginConfig.getPluginDirectoryPath(), project);
        final String eventId = dBHandler.getEventID(tableName, searchCriteria);
        LOGGER.info("Fetched old event with id '{}', for project '{}', and searchCritera '{}'",
                eventId, project,
                searchCriteria);
        return eventId;
    }
}
