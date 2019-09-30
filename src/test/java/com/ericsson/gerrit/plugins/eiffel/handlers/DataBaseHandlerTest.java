package com.ericsson.gerrit.plugins.eiffel.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.gerrit.plugins.eiffel.handlers.DataBaseHandler.Table;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.ericsson.gerrit.plugins.eiffel.*")
public class DataBaseHandlerTest {
    private final String branch = "my_test_branch";
    private final String scmTableKey = "branch";
    private final String scmpTableKey = "changeId";

    private DataBaseHandler dbHandler;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void init() throws Exception {
        File tmpFolderPath = testFolder.newFolder();
        dbHandler = new DataBaseHandler(tmpFolderPath, "test_file_name.db");
    }

    /**
     * An exception should be raised if connection to Db cannot be established.
     *
     * @throws Exception
     */
    @Test
    public void testFailingToConnectToDbThrowsError() throws Exception {
        try {
            dbHandler = new DataBaseHandler("dir_should_not_exist", "test_file_name.db");
            assertEquals("Expected the call to throw and Exception but none was thrown!", true, false);
        } catch (ConnectException e) {
            // test passed!
        }
    }

    /**
     * This test inserts and event id for a branch and fetches it to ensure it both got inserted and
     * fetched correctly.
     *
     * @throws Exception
     */
    @Test
    public void testInsertAndGetEventID() throws Exception {
        String eiffelEventId = generateEiffelEventId();
        dbHandler.insertInto(Table.SCS_TABLE, branch, eiffelEventId);
        String eventId = dbHandler.getEventID(Table.SCS_TABLE, branch);
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

        dbHandler.insertInto(Table.SCS_TABLE, branch, firstEiffelEventId);
        dbHandler.updateInto(Table.SCS_TABLE, branch, secondEiffelEventId);

        String eventId = dbHandler.getEventID(Table.SCS_TABLE, branch);
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
        dbHandler.insertInto(Table.SCS_TABLE, branch, firstEiffelEventId);

        try {
            dbHandler.insertInto(Table.SCS_TABLE, branch, secondEiffelEventId);
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
    @Test
    public void testGetNoneExistingEventIdReturnsNull() throws Exception {
        String eventId = dbHandler.getEventID(Table.SCS_TABLE, branch);
        assertTrue("Expect fetched event ID to be empty", eventId.isEmpty());

    }

    /**
     * Test Table enum
     */
    @Test
    public void testTables() {
        Table scmTable = Table.SCS_TABLE;
        Table scmpTable = Table.SCC_TABLE;

        assertEquals("Table.SCM_TABLE tabler key should be", scmTableKey, scmTable.getKeyName());
        assertEquals("Table.SCMP_TABLE tabler key should be", scmpTableKey, scmpTable.getKeyName());

    }

    /**
     * Test throwing several exceptions and ensure they cause the class to return correct values and or
     * the exception is correctly caught. Exceptions caused by mocks should not leak into the test.
     *
     * @throws Exception
     */
    @Test
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

        // When mocking an exception while executing getEventID the function should return an empty String,
        // exception may be thrown when no values was found and should be empty.
        Mockito.when(result.next()).thenThrow(new SQLException("Exception thrown by test"));
        String fetchedEvent = dbHandler.getEventID(Table.SCS_TABLE, branch);
        assertTrue(fetchedEvent.isEmpty());

        // When we initiate a new DataBaseHandler we throw SQLExceptions, those exceptions should be caught
        // and the class should be created as normal.
        Mockito.when(connection.getMetaData()).thenThrow(new SQLException("Exception thrown by test"));
        Mockito.doReturn(statement).when(connection).createStatement();
        Mockito.when(statement.execute(Mockito.any())).thenThrow(new SQLException("Exception thrown by test"));
        File tmpFolderPath = testFolder.newFolder();
        new DataBaseHandler(tmpFolderPath, "test_file_name.db");

        // When doing updateInto on the database handler we throw some exceptions and ensures they are
        // correctly sent back to the user.
        try {
            Mockito.when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Exception thrown by test"));
            dbHandler.updateInto(Table.SCS_TABLE, branch, "event_id");
            assertEquals("Expected executeUpdate to throw and Exception but none was thrown!", true, false);
        } catch (SQLException e) {
            // test passed!
        }

        try {
            Mockito.when(preparedStatement.executeUpdate()).thenReturn(0);
            dbHandler.updateInto(Table.SCS_TABLE, branch, "event_id");
            assertEquals("Expected executeUpdate to throw and Exception but none was thrown!", true, false);
        } catch (SQLException e) {
            // test passed!
        }

        try {
            Mockito.when(connection.prepareStatement(Mockito.any()))
                    .thenThrow(new SQLException("Exception thrown by test"));
            dbHandler.updateInto(Table.SCS_TABLE, branch, "event_id");
            assertEquals("Expected prepareStatement to throw and Exception but none was thrown!", true, false);
        } catch (SQLException e) {
            // test passed!
        }

    }

    private static String generateEiffelEventId() {
        return UUID.randomUUID().toString();
    }

}
