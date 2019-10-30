package com.ericsson.gerrit.plugins.eiffel.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.ConnectException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.gerrit.plugins.eiffel.exceptions.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.handlers.DataBaseHandler;
import com.ericsson.gerrit.plugins.eiffel.handlers.Table;
import com.ericsson.gerrit.plugins.eiffel.storage.EventStorageFactory;
import com.ericsson.gerrit.plugins.eiffel.storage.SourceChangeCreatedStorage;
import com.ericsson.gerrit.plugins.eiffel.storage.SourceChangeSubmittedStorage;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.ericsson.gerrit.plugins.eiffel.*")
public class EventStorageTest {

    private static final String FILE_ENDING = "db";
    private static final String PROJECT = "project_test";
    private static final String BRANCH = "branch_test";
    private DataBaseHandler dbHandler;
    private SourceChangeCreatedStorage sourceChangeCreatedState;
    private SourceChangeSubmittedStorage sourceChangeSubmittedState;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private File tmpFolderPath;

    @Rule
    public final ExpectedException exception = ExpectedException.none();


    @Before
    public void init() throws Exception {
        tmpFolderPath = testFolder.newFolder();
        dbHandler = Mockito.mock(DataBaseHandler.class);
        String fileName = String.format("%s.%s", PROJECT, FILE_ENDING);
        PowerMockito.whenNew(DataBaseHandler.class).withParameterTypes(File.class, String.class).withArguments(Mockito.any())
                .thenReturn(dbHandler);
        sourceChangeCreatedState = (SourceChangeCreatedStorage) EventStorageFactory.getEventStorage(tmpFolderPath, "EiffelSourceChangeCreatedEvent");
        sourceChangeSubmittedState = (SourceChangeSubmittedStorage) EventStorageFactory.getEventStorage(tmpFolderPath, "EiffelSourceChangeSubmittedEvent");
    }

    @Test
    public void testBuildParentFilePath() throws Exception {
        String expectedParentPath = tmpFolderPath.toString();

        Mockito.when(dbHandler.getEventID(Table.SCC_TABLE, BRANCH)).thenReturn("");
        sourceChangeCreatedState.setLastSubmittedEiffelEvent(PROJECT, BRANCH, "{eiffel_event}", Table.SCC_TABLE);

        File parentDirectory = new File(expectedParentPath);
        assertTrue(parentDirectory.exists());
    }

    @Test
    public void testgetLastSentEvent() throws Exception {
        Mockito.when(dbHandler.getEventID(Mockito.any(), Mockito.any())).thenReturn("eventID");

        String eventId = sourceChangeSubmittedState.getLastSubmittedEiffelEvent(PROJECT, BRANCH, Table.SCS_TABLE);
        assertEquals("Table.SCS_TABLE tabler key should be", "eventID", eventId);

        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, ""))
                .thenThrow(new NoSuchElementException("Exception thrown by test"));
        exception.expect(NoSuchElementException.class);
        sourceChangeSubmittedState.getLastSubmittedEiffelEvent("", "", Table.SCS_TABLE);
    }

    @Test
    public void testUpdateLastEventCalled() throws Exception {
        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, BRANCH)).thenReturn("old-event-id");

        String eventId = "event-id";
        sourceChangeSubmittedState.setLastSubmittedEiffelEvent(PROJECT, BRANCH, eventId, Table.SCS_TABLE);
        Mockito.verify(dbHandler).updateInto(Table.SCS_TABLE, BRANCH, eventId);
        Mockito.verify(dbHandler, Mockito.never()).insertInto(Mockito.any(), Mockito.any(), Mockito.any());

    }

    @Test
    public void testInsertLastEventCalled() throws Exception {
        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, BRANCH)).thenReturn("");

        String eventId = "event-id";
        sourceChangeSubmittedState.setLastSubmittedEiffelEvent(PROJECT, BRANCH, eventId, Table.SCS_TABLE);
        Mockito.verify(dbHandler, Mockito.never()).updateInto(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dbHandler).insertInto(Table.SCS_TABLE, BRANCH, eventId);

    }

    @Test
    public void testConnectionErrorsHandled() throws Exception {
        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, BRANCH)).thenThrow(new ConnectException("Test Exception"));

        String eventId = "event-id";

        exception.expect(ConnectException.class);
        sourceChangeSubmittedState.setLastSubmittedEiffelEvent(PROJECT, BRANCH, eventId, Table.SCS_TABLE);

        Mockito.verify(dbHandler, Mockito.never()).updateInto(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dbHandler, Mockito.never()).insertInto(Mockito.any(), Mockito.any(), Mockito.any());

        assertEquals("Error occured return empty string.", "",
                sourceChangeSubmittedState.getEventId("", ""));

    }

}
