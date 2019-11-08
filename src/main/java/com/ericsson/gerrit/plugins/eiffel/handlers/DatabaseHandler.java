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

package com.ericsson.gerrit.plugins.eiffel.handlers;

import java.io.File;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.exceptions.NoSuchElementException;

/**
 * This is the database handler class. At initiation it checks if the name given
 * exist as a db file, if not it gets created together with required tables. At
 * initiation a file name is given making it possible to store a file per
 * project depending how the classes that uses this handler implements this
 * function.
 *
 */
public class DatabaseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHandler.class);
    private static final String EVENT_ID_KEY = "eventId";
    protected static final String FILE_TYPE_EXTENSION = "db";

    private final String databaseFile;
    private File pluginDir;
    private String project;
    private Connection connection;
    
//    Connection connection;

    public DatabaseHandler(final File pluginDir, final String project) throws ConnectException {
        String fileName = String.format("%s.%s", project, FILE_TYPE_EXTENSION);

        final Path filePath = Paths.get(pluginDir.toString(), fileName);
        this.databaseFile = String.format("jdbc:sqlite:%s", filePath);
        this.pluginDir = pluginDir;
        this.project = project;

        String parentPath = buildParentFilePath();
        createParentDirsIfNecessary(parentPath);

        createNewDatabase();
        createTables();
    }

    /**
     * This function returns an event id if exists for a specific table depending on
     * the searchCriteria
     *
     * @param table
     * @param searchCriteria
     * @return eventId
     * @throws ConnectException
     * @throws NoSuchElementException
     */
    public String getEventID(final Table table, final String searchCriteria) throws ConnectException, NoSuchElementException {
        String eventID = "";

        String sqlSelectStatement = String.format("SELECT %s FROM %s WHERE %s=?", EVENT_ID_KEY, table, table.keyName);
        try (Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sqlSelectStatement)) {

            preparedStatement.setString(1, searchCriteria);
            eventID = executeQuery(preparedStatement);

        } catch (SQLException e) {
            LOGGER.error("Error when trying to fetch values from database: {}\n{}", e.getMessage(), e);
        }

        if (eventID.isEmpty()) {
            throw new NoSuchElementException("Database did not return any value for this query. Table: "
                    + table + ",KeyName: " + table.keyName + ",searchCriteria: " + searchCriteria);
        }

        return eventID;
    }

    /**
     * This function updates value to the given table. The searchCriteria value is
     * different depending on Table (branch name for scs and change-id for scc)
     *
     * @param table
     * @param searchCriteria
     * @param eiffelEvent
     * @throws ConnectException
     * @throws SQLException
     */
    public void updateInto(final Table table, final String searchCriteria, final String eiffelEvent)
            throws ConnectException, SQLException {
        String sqlUpdateStatement = String.format("UPDATE %s SET %s=? WHERE %s=?", table, EVENT_ID_KEY, table.keyName);
        prepareAndExecuteStatement(sqlUpdateStatement, searchCriteria, eiffelEvent);
    }

    /**
     * This function inserts values to the given table. The key value is
     * different depending on Table (branch name for scs and change-id for scc)
     *
     * @param table
     * @param key
     * @param value
     * @throws SQLException
     * @throws ConnectException
     */
    public void insertInto(final Table table, final String key, final String value)
            throws SQLException, ConnectException {
        String sqlInsertStatement = String.format("INSERT INTO %s(%s,%s) VALUES(?,?)", table, EVENT_ID_KEY,
                table.keyName);
        prepareAndExecuteStatement(sqlInsertStatement, key, value);

    }

    /**
     * This function will create a sql database structure in the database file if it
     * does not exist.
     *
     * @throws ConnectException
     */
    private void createNewDatabase() throws ConnectException {
        try (Connection sqlConnection = connect()) {
            if (sqlConnection != null) {
                //When getMetaData is called, it will create the .db file
                sqlConnection.getMetaData();
                sqlConnection.close();
            }

        } catch (SQLException e) {
            LOGGER.error("Error when trying to create new database.", e);
        }
    }

    /**
     * This function loops the values in the Table enum and executes the create
     * table command for each table and creates it in the database if it does not already
     * exist.
     *
     * @throws ConnectException
     */
    private void createTables() throws ConnectException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            for (Table table : Table.values()) {
                createTable(table, statement);
            }
            LOGGER.debug("Created tables successfully");
        } catch (SQLException e) {
            LOGGER.error("Error while creating Tables in database.", e);
        }
    }

    /**
     * This function connects to the database and returns a connection
     *
     * @return Connection to database
     * @throws ConnectException
     */
    private Connection connect() throws ConnectException {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(this.databaseFile);
        } catch (SQLException | ClassNotFoundException e) {
            throw new ConnectException(
                    String.format("Failed to create a database connection.", e));
        }
    }

    /**
     * Executes the prepared statement.
     *
     * @param preparedStatement
     * @return
     * @throws SQLException
     */
    private String executeQuery(final PreparedStatement preparedStatement) throws SQLException {
        try (ResultSet result = preparedStatement.executeQuery();) {
            if (result.next()) {
                return result.getString(EVENT_ID_KEY);
            }
        }
        return "";
    }

    /**
     * Creates a prepared statement and executes an update on the sqlStatement
     * given.
     *
     * @param sqlStatement
     * @param keyValue
     * @param eiffelEvent
     * @throws ConnectException
     * @throws SQLException
     */
    private void prepareAndExecuteStatement(final String sqlStatement, final String searchCriteria, final String eiffelEvent)
            throws ConnectException, SQLException {
        try (PreparedStatement preparedStatement = prepareStatementForResourceBlock(sqlStatement)) {
            preparedStatement.setString(1, eiffelEvent);
            preparedStatement.setString(2, searchCriteria);
            int updateCount = preparedStatement.executeUpdate();

            if (updateCount == 0) {
                throw new SQLException("No changes was saved in the database.");
            }

        } catch (SQLException e) {
            LOGGER.error(
                    "Error when trying to INSERT/ADD/UPDATE value into database using sqlStatement: {} and search criteria {} and eiffel event {}",
                    sqlStatement, searchCriteria, eiffelEvent, e);
            throw e;
        }finally {
            //TODO: make this more robust
            connection.close();
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
    protected String buildParentFilePath() {
        String relativeParentPath = generateRelativeParentPath(project);
        Path absolutePath = Paths.get(pluginDir.getAbsolutePath(), relativeParentPath);
        return absolutePath.toString();
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

    private void createTable(final Table table, final Statement statement) throws ConnectException, SQLException {
        String sqlCreateStatement = String.format("CREATE TABLE IF NOT EXISTS %s (%s text PRIMARY KEY, %s text)", table,
                table.keyName, EVENT_ID_KEY);
        statement.execute(sqlCreateStatement);
    }

    private PreparedStatement prepareStatementForResourceBlock(final String sqlStatement)
            throws ConnectException, SQLException {
        connection = connect();
        return connection.prepareStatement(sqlStatement);
    }
}