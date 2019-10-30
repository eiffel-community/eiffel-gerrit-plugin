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
public class DataBaseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataBaseHandler.class);
    private static final String EVENT_ID_KEY = "eventId";
    private final String databaseFile;

    public DataBaseHandler(final String pluginDir, final String fileName) throws ConnectException {
        final Path filePath = Paths.get(pluginDir, fileName);
        this.databaseFile = String.format("jdbc:sqlite:%s", filePath);
        createNewDatabase();
        createTables();
    }

    public DataBaseHandler(final File pluginDir, final String fileName) throws ConnectException {
        this(pluginDir.toString(), fileName);
    }

    /**
     * This function returns an event id if exists for a specific table depending on
     * the keyValue
     *
     * @param table
     * @param keyValue
     * @return eventId
     * @throws ConnectException
     * @throws NoSuchElementException
     */
    public String getEventID(final Table table, final String keyValue) throws ConnectException, NoSuchElementException {
        String eventID = "";

        String sqlSelectStatement = String.format("SELECT * FROM %s WHERE %s=?", table, table.keyName);
        try (Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sqlSelectStatement)) {

            preparedStatement.setString(1, keyValue);
            eventID = executeQuery(preparedStatement);

        } catch (SQLException e) {
            LOGGER.error("Error when trying to fetch values from database: {}\n{}", e.getMessage(), e);
        }

        if (eventID.isEmpty()) {
            throw new NoSuchElementException("Database did not return any value for this query");
        }

        return eventID;
    }

    /**
     * This function updates value to the given table. The keyValue value is
     * different depending on Table (branch name for scs and change-id for scc)
     *
     * @param table
     * @param keyValue
     * @param eiffelEvent
     * @throws ConnectException
     * @throws SQLException
     */
    public void updateInto(final Table table, final String keyValue, final String eiffelEvent)
            throws ConnectException, SQLException {
        String sqlUpdateStatement = String.format("UPDATE %s SET %s=? WHERE %s=?", table, EVENT_ID_KEY, table.keyName);
        executeUpdate(sqlUpdateStatement, keyValue, eiffelEvent);
    }

    /**
     * This function inserts values to the given table. The keyValue value is
     * different depending on Table (branch name for scs and change-id for scc)
     *
     * @param table
     * @param keyValue
     * @param value
     * @throws SQLException
     * @throws ConnectException
     */
    public void insertInto(final Table table, final String keyValue, final String value)
            throws SQLException, ConnectException {
        String sqlInsertStatement = String.format("INSERT INTO %s(%s,%s) VALUES(?,?)", table, EVENT_ID_KEY,
                table.keyName);
        executeUpdate(sqlInsertStatement, keyValue, value);

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
                sqlConnection.getMetaData();
                sqlConnection.close();
            }

        } catch (SQLException e) {
            LOGGER.error("Error when trying to create new database: {}\n{}", e.getMessage(), e);
        }
    }

    /**
     * This function loops the values in the Table enum and executes the create
     * table command for each table and creates it in the database if it not already
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
            LOGGER.error("Error while creating Tables in database: {}\n{}", e.getMessage(), e);
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
                    String.format("Failed to create a database connection. %s\n%s", e.getMessage(), e));
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
    private void executeUpdate(final String sqlStatement, final String keyValue, final String eiffelEvent)
            throws ConnectException, SQLException {
        try (PreparedStatement preparedStatement = prepareStatmentForResourceBlock(sqlStatement)) {
            preparedStatement.setString(1, eiffelEvent);
            preparedStatement.setString(2, keyValue);
            int updateCount = preparedStatement.executeUpdate();

            if (updateCount == 0) {
                throw new SQLException("No changes was saved in the database.");
            }

        } catch (SQLException e) {
            LOGGER.error("Error when trying to add value into database: {}\n{}", e.getMessage(), e);
            throw e;
        }
    }

    public void createTable(final Table table, final Statement statement) throws ConnectException, SQLException {
        String sqlCreateStatement = String.format("CREATE TABLE IF NOT EXISTS %s (%s text PRIMARY KEY, %s text)", table,
                table.keyName, EVENT_ID_KEY);
        statement.execute(sqlCreateStatement);
    }

    private PreparedStatement prepareStatmentForResourceBlock(final String sqlStatement)
            throws ConnectException, SQLException {
        Connection connection = connect();
        return connection.prepareStatement(sqlStatement);
    }
}
