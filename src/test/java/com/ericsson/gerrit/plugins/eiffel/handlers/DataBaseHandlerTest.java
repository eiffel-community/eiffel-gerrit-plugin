package com.ericsson.gerrit.plugins.eiffel.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.io.File;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.gerrit.plugins.eiffel.exceptions.NoSuchElementException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.ericsson.gerrit.plugins.eiffel.*")
public class DataBaseHandlerTest {
    private static final String BRANCH = "my_test_branch";
    private static final String FAULTY_BRANCH = "faulty_branch";
    private static final String SCS_TABLE_KEY = "branch";
    private static final String SCC_TABLE_KEY = "changeId";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private DatabaseHandler dbHandler;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void init() throws Exception {
        File tmpFolderPath = testFolder.newFolder();
        dbHandler = new DatabaseHandler(tmpFolderPath, "project_name");
    }

    /**
     * An exception should be raised if connection to Db cannot be established.
     *
     * @throws Exception
     */
    @Test(expected = ConnectException.class)
    public void testFailingToConnectToDbThrowsError() throws Exception {
        final File pluginDir = new File("dir_should_not_exist");
        new DatabaseHandler(pluginDir, "project_name");
    }

    /**
     * This test inserts an event id for a branch and fetches it to ensure it both
     * got inserted and fetched correctly.
     *
     * @throws Exception
     */
    @Test
    public void testInsertAndGetEventID() throws Exception {
        String eiffelEventId = generateEiffelEventId();
        dbHandler.insertInto(Table.SCS_TABLE, BRANCH, eiffelEventId);
        String eventId = dbHandler.getEventID(Table.SCS_TABLE, BRANCH);
        assertEquals("Expect fetched event ID", eiffelEventId, eventId);
    }

    /**
     * Updating id should be updated, test ensure the id gets updated correctly
     *
     * @throws Exception
     */
    @Test
    public void testInsertAndUpdateEventID() throws Exception {
        String firstEiffelEventId = generateEiffelEventId();
        String secondEiffelEventId = generateEiffelEventId();

        dbHandler.insertInto(Table.SCS_TABLE, BRANCH, firstEiffelEventId);
        dbHandler.updateInto(Table.SCS_TABLE, BRANCH, secondEiffelEventId);

        String eventId = dbHandler.getEventID(Table.SCS_TABLE, BRANCH);
        assertFalse(firstEiffelEventId.equals(eventId));
        assertEquals("Expect fetched event ID", secondEiffelEventId, eventId);
    }

    /**
     * Insert should fail if value and key already exist
     *
     * @throws Exception
     */
    @Test
    public void testInsertIntoExistingFails() throws Exception {
        String firstEiffelEventId = generateEiffelEventId();
        String secondEiffelEventId = generateEiffelEventId();
        dbHandler.insertInto(Table.SCS_TABLE, BRANCH, firstEiffelEventId);

        try {
            dbHandler.insertInto(Table.SCS_TABLE, BRANCH, secondEiffelEventId);
            assertEquals("Expected the call to throw and Exception but none was thrown!", true, false);
        } catch (SQLException e) {
            // test passed!
        }

    }

    /**
     * Empty db should return empty string
     *
     * @throws Exception
     */
    @Test(expected = NoSuchElementException.class)
    public void testGetNoneExistingEventIdReturnsNull() throws Exception {
        dbHandler.getEventID(Table.SCS_TABLE, FAULTY_BRANCH);
    }

    /**
     * Test Table enum
     */
    @Test
    public void testTables() {
        Table scsTable = Table.SCS_TABLE;
        Table sccTable = Table.SCC_TABLE;

        assertEquals("Table.SCS_TABLE tabler key should be", SCS_TABLE_KEY, scsTable.getKeyName());
        assertEquals("Table.SCC_TABLE tabler key should be", SCC_TABLE_KEY, sccTable.getKeyName());

    }

    /**
     * Test throwing several exceptions and ensure they cause the class to return
     * correct values and or the exception is correctly caught. Exceptions caused by
     * mocks should not leak into the test.
     *
     * @throws Exception
     */
    @Test(expected = NoSuchElementException.class)
    public void testExceptionsIsThrown() throws Exception {
        // Prepare mocks
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        Statement statement = mock(Statement.class);
        ResultSet result = mock(ResultSet.class);

        PowerMockito.mockStatic(DriverManager.class);
        BDDMockito.given(DriverManager.getConnection(Mockito.any())).willReturn(connection);

        Mockito.doReturn(preparedStatement).when(connection).prepareStatement(Mockito.any());
        Mockito.doReturn(result).when(preparedStatement).executeQuery();

        // When mocking an exception while executing getEventID the function should
        // return an empty String,
        // exception may be thrown when no values was found and should be empty.
        Mockito.when(result.next()).thenThrow(new SQLException("Exception thrown by test"));

        exception.expect(RuntimeException.class);
        dbHandler.getEventID(Table.SCS_TABLE, BRANCH);


        // When we initiate a new DataBaseHandler we throw SQLExceptions, those
        // exceptions should be caught
        // and the class should be created as normal.
        Mockito.when(connection.getMetaData()).thenThrow(new SQLException("Exception thrown by test"));
        Mockito.doReturn(statement).when(connection).createStatement();
        Mockito.when(statement.execute(Mockito.any())).thenThrow(new SQLException("Exception thrown by test"));
        File tmpFolderPath = testFolder.newFolder();
        new DatabaseHandler(tmpFolderPath, "project_name");

        // When doing updateInto on the database handler we throw some exceptions and
        // ensures they are
        // correctly sent back to the user.
        try {
            Mockito.when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Exception thrown by test"));
            dbHandler.updateInto(Table.SCS_TABLE, BRANCH, "event_id");
            assertEquals("Expected executeUpdate to throw an Exception but none was thrown!", true, false);
        } catch (SQLException e) {
            // test passed!
        }

        try {
            Mockito.when(preparedStatement.executeUpdate()).thenReturn(0);
            dbHandler.updateInto(Table.SCS_TABLE, BRANCH, "event_id");
            assertEquals("Expected executeUpdate to throw an Exception but none was thrown!", true, false);
        } catch (SQLException e) {
            // test passed!
        }

        try {
            Mockito.when(connection.prepareStatement(Mockito.any()))
                    .thenThrow(new SQLException("Exception thrown by test"));
            dbHandler.updateInto(Table.SCS_TABLE, BRANCH, "event_id");
            assertEquals("Expected prepareStatement to throw an Exception but none was thrown!", true, false);
        } catch (SQLException e) {
            // test passed!
        }

    }

    private static String generateEiffelEventId() {
        return UUID.randomUUID().toString();
    }

}
