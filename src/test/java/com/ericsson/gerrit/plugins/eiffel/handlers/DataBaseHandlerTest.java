package com.ericsson.gerrit.plugins.eiffel.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.gerrit.plugins.eiffel.exceptions.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.logHelper.LogHelper;

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
    private File tmpFolderPath;

    private LogHelper logHelper = new LogHelper();

    @Before
    public void init() throws Exception {
        tmpFolderPath = testFolder.newFolder();
        dbHandler = new DatabaseHandler(tmpFolderPath, "project_name");
        logHelper.setup();
    }

    @After
    public void tearDown() throws IOException {
        logHelper.tearDown();

        // By trying to delete the file we know if we have any open connections
        Path dbfile = tmpFolderPath.toPath().resolve("project_name.db");
        Files.delete(dbfile);

        // By trying to delete the folder we know that we only have one file there
        Files.delete(tmpFolderPath.toPath());
    }

    /**
     * Tests that the parent file path is created as expected
     *
     * @throws Exception
     */
    @Test
    public void testBuildParentFilePath() throws Exception {
        String projectName = "parent_project/project_name";
        File tmpFolderPath = testFolder.newFolder();
        File file = new File(tmpFolderPath + "/parent_project");
        ArgumentCaptor<String> argumentCapture = ArgumentCaptor.forClass(String.class);

        PowerMockito.whenNew(File.class).withParameterTypes(String.class).withArguments(argumentCapture.capture()).thenReturn(file);

        new DatabaseHandler(tmpFolderPath, projectName);
        List<String> arguments = argumentCapture.getAllValues();

        String expectedFilePath = tmpFolderPath.getPath().replace("\\", "/") + "/parent_project";
        String actualFilePath = arguments.get(0).replace("\\", "/");
        assertEquals("Incorrect parent file path.", expectedFilePath, actualFilePath);
        logHelper.verifyLoggerCalledTimes(0);
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
        logHelper.verifyLoggerCalledTimes(0);
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
        logHelper.verifyLoggerCalledTimes(0);
    }

    /**
     * Tries to update but no rows updated, should throw SQLException
     *
     * @throws Exception
     */
    @Test(expected = SQLException.class)
    public void testNoneUpdatedRows() throws Exception {
        logHelper.removeStdoutAppenders();

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        PowerMockito.mockStatic(DriverManager.class);

        Mockito.when(DriverManager.getConnection(Mockito.any())).thenReturn(connection);
        Mockito.when(connection.prepareStatement(Mockito.any())).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeUpdate()).thenReturn(0);

        dbHandler.updateInto(Table.SCS_TABLE, BRANCH, "my-eiffel-event");
        logHelper.verifyLoggerCalledTimes(1);
    }

    /**
     * Insert should fail if value and key already exist
     *
     * @throws Exception
     */
    @Test
    public void testInsertIntoExistingFails() throws Exception {
        logHelper.removeStdoutAppenders();

        String firstEiffelEventId = generateEiffelEventId();
        String secondEiffelEventId = generateEiffelEventId();
        dbHandler.insertInto(Table.SCS_TABLE, BRANCH, firstEiffelEventId);

        try {
            dbHandler.insertInto(Table.SCS_TABLE, BRANCH, secondEiffelEventId);
            assertEquals("Expected the call to throw and Exception but none was thrown!", true, false);
        } catch (SQLException e) {
            // test passed!
        }
        logHelper.verifyLoggerCalledTimes(1);
    }

    /**
     * Empty db should return empty string
     *
     * @throws Exception
     */
    @Test(expected = NoSuchElementException.class)
    public void testGetNoneExistingEventIdReturnsEmpty() throws Exception {
        logHelper.expectLoggerCalledTimes(0);
        dbHandler.getEventID(Table.SCS_TABLE, FAULTY_BRANCH);
    }

    /**
     * If sqlException is thrown within GetEventId, it should be caught and rethrown as NoSuchElementException
     *
     * @throws Exception
     */
    @Test(expected = NoSuchElementException.class)
    public void testGetEventIdSqlError() throws Exception {
        logHelper.removeStdoutAppenders();
        Connection connection = mock(Connection.class);

        PowerMockito.mockStatic(DriverManager.class);
        BDDMockito.given(DriverManager.getConnection(Mockito.any())).willReturn(connection);
        Mockito.when(connection.prepareStatement(Mockito.any())).thenThrow(new SQLException());

        logHelper.expectLoggerCalledTimes(1);
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
        logHelper.verifyLoggerCalledTimes(0);
    }
    /**
     * Test throwing several exceptions and ensure they cause the class to return
     * correct values and or the exception is correctly caught. Exceptions caused by
     * mocks should not leak into the test.
     *
     * @throws Exception
     */
    @Test
    public void testExceptionsIsThrown() throws Exception {
        logHelper.removeStdoutAppenders();

        // Prepare mocks
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        Statement statement = mock(Statement.class);
        ResultSet result = mock(ResultSet.class);

        PowerMockito.mockStatic(DriverManager.class);
        Mockito.when(DriverManager.getConnection(Mockito.any())).thenReturn(connection);

        Mockito.doReturn(preparedStatement).when(connection).prepareStatement(Mockito.any());
        Mockito.doReturn(result).when(preparedStatement).executeQuery();

        // When mocking an exception while executing getEventID the function should
        // return an empty String,
        // exception may be thrown when no values was found and should be empty.

        try {
            dbHandler.getEventID(Table.SCS_TABLE, BRANCH);
            assertEquals("Expected getEventId to throw an Exception but none was thrown!", true, false);
        } catch (NoSuchElementException e) {
            // test passed!
        }

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
        logHelper.verifyLoggerCalledTimes(4);
    }

    private static String generateEiffelEventId() {
        return UUID.randomUUID().toString();
    }

}
