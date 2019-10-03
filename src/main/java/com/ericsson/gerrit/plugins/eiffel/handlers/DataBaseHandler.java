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

/**
 * This is the database handler class. at initiation it checks if the name given exist as a db file,
 * if not it gets created together with required tables. At initiation a file name is given making
 * it possible to store a file per project depending how the classes that uses this handler
 * implements this function.
 *
 */
public class DataBaseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataBaseHandler.class);
    private static final String EVENT_ID_KEY = "eventId";
    private final String databaseFile;

    /**
     * Constructor that takes plugin directory path as String and plugin name as String.
     *
     * @param pluginDir
     * @param filename
     * @throws ConnectException
     */
    public DataBaseHandler(final String pluginDir, final String filename) throws ConnectException {
        final Path filePath = Paths.get(pluginDir, filename);
        this.databaseFile = String.format("jdbc:sqlite:%s", filePath);
        createNewDatabase();
        createTables();
    }

    /**
     * Constructor that takes plugin directory path as File and plugin name as String.
     *
     * @param pluginDir
     * @param filename
     * @throws ConnectException
     */
    public DataBaseHandler(final File pluginDir, final String filename) throws ConnectException {
        this(pluginDir.toString(), filename);
    }

    /**
     * This function returns an event id if exists for a specific table depending on
     * the keyValue
     *
     * @param table
     * @param keyValue
     * @return eventId
     * @throws ConnectException
     * @throws SomeRuntimeException
     */
    public String getEventID(final Table table, final String keyValue) throws ConnectException, SomeRuntimeException {
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
            throw new SomeRuntimeException("Database did not return any value for this query");
        }

        return eventID;
    }

    /**
     * This function updates value to the given table. The keyValue value is
     * different depending on Table (branch name for scs and change-id for scc)
     *
     * @param table
     * @param keyValue
     * @param eiffelevent
     * @throws ConnectException
     * @throws SQLException
     */
    public void updateInto(final Table table, final String keyValue, final String eiffelevent)
            throws ConnectException, SQLException {
        String sqlUpdateStatement = String.format("UPDATE %s SET %s=? WHERE %s=?", table, EVENT_ID_KEY, table.keyName);
        executeUpdate(sqlUpdateStatement, keyValue, eiffelevent);
    }

    /**
     * This function inserts values to the given table. The keyValue value is
     * different depending on Table (branch name for scs and change-id for scc)
     *
     * @param table
     * @param keyValue
     * @param eiffelevent
     * @throws SQLException
     * @throws ConnectException
     */
    public void insertInto(final Table table, final String keyValue, final String eiffelevent)
            throws SQLException, ConnectException {
        String sqlInsertStatement = String.format("INSERT INTO %s(%s,%s) VALUES(?,?)", table, EVENT_ID_KEY,
                table.keyName);
        executeUpdate(sqlInsertStatement, keyValue, eiffelevent);

    }

    /**
     * This function will create a database structure in the database file if it does not exist.
     *
     * @throws ConnectException
     */
    private void createNewDatabase() throws ConnectException {
        try (Connection connection = connect()) {
            if (connection != null) {
                connection.getMetaData();
                connection.close();
            }

        } catch (SQLException e) {
            LOGGER.error("Error when trying to create new database: {}\n{}", e.getMessage(), e);
        }
    }

    /**
     * This function loops the values in the Table enum and executes the create table command for each
     * table and creates it in the database if it not already exist.
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
     * executes the prepared statement.
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
        } catch (SQLException e) {
            throw e;
        }
        return "";
    }

    /**
     * Creates a prepared statement and executes an update on the sqlStatement given.
     *
     * @param sqlStatement
     * @param keyValue
     * @param eiffelevent
     * @throws ConnectException
     * @throws SQLException
     */
    private void executeUpdate(final String sqlStatement, final String keyValue, final String eiffelevent)
            throws ConnectException, SQLException {
        try (Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
            preparedStatement.setString(1, eiffelevent);
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

    public void createTable(Table table, Statement statement) throws ConnectException, SQLException {
        String sqlCreateStatement = String.format("CREATE TABLE IF NOT EXISTS %s (%s text PRIMARY KEY, %s text)", table,
                table.keyName, EVENT_ID_KEY);
        statement.execute(sqlCreateStatement);
    }

}
