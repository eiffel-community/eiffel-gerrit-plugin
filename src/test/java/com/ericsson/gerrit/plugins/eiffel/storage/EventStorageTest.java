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

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.exceptions.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.handlers.DatabaseHandler;
import com.ericsson.gerrit.plugins.eiffel.handlers.Table;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.ericsson.gerrit.plugins.eiffel.*")
public class EventStorageTest {

    private static final String PROJECT = "project_test";
    private static final String BRANCH = "branch_test";
    private DatabaseHandler dbHandler;
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

        final EiffelPluginConfiguration pluginConfig = Mockito.mock(EiffelPluginConfiguration.class);
        PowerMockito.when(pluginConfig.getProject()).thenReturn(PROJECT);
        PowerMockito.when(pluginConfig.getPluginDirectoryPath()).thenReturn(tmpFolderPath);

        dbHandler = Mockito.mock(DatabaseHandler.class);
        PowerMockito.whenNew(DatabaseHandler.class).withParameterTypes(File.class, String.class).withArguments(Mockito.any())
                .thenReturn(dbHandler);
        sourceChangeCreatedState = (SourceChangeCreatedStorage) EventStorageFactory.getEventStorage(
                pluginConfig, "EiffelSourceChangeCreatedEvent");
        sourceChangeSubmittedState = (SourceChangeSubmittedStorage) EventStorageFactory.getEventStorage(
                pluginConfig, "EiffelSourceChangeSubmittedEvent");
    }

    @Test
    public void testBuildParentFilePath() throws Exception {
        final String expectedParentPath = tmpFolderPath.toString();

        Mockito.when(dbHandler.getEventID(Table.SCC_TABLE, BRANCH)).thenReturn("");
        sourceChangeCreatedState.saveEiffelEventId(BRANCH, "{eiffel_event}", Table.SCC_TABLE);

        final File parentDirectory = new File(expectedParentPath);
        assertTrue(parentDirectory.exists());
    }

    @Test
    public void testgetLastSentEvent() throws Exception {
        Mockito.when(dbHandler.getEventID(Mockito.any(), Mockito.any())).thenReturn("eventID");

        final String eventId = sourceChangeSubmittedState.getLastSavedEiffelEvent(PROJECT, BRANCH, Table.SCS_TABLE);
        assertEquals("Table.SCS_TABLE tabler key should be", "eventID", eventId);

        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, ""))
                .thenThrow(new NoSuchElementException("Exception thrown by test"));
        exception.expect(NoSuchElementException.class);
        sourceChangeSubmittedState.getLastSavedEiffelEvent("", "", Table.SCS_TABLE);
    }

    @Test
    public void testUpdateLastEventCalled() throws Exception {
        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, BRANCH)).thenReturn("old-event-id");

        final String eventId = "event-id";
        sourceChangeSubmittedState.saveEiffelEventId(BRANCH, eventId, Table.SCS_TABLE);
        Mockito.verify(dbHandler).updateInto(Table.SCS_TABLE, BRANCH, eventId);
        Mockito.verify(dbHandler, Mockito.never()).insertInto(Mockito.any(), Mockito.any(), Mockito.any());

    }

    @Test
    public void testInsertLastEventCalled() throws Exception {
        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, BRANCH)).thenReturn("");

        final String eventId = "event-id";
        sourceChangeSubmittedState.saveEiffelEventId(BRANCH, eventId, Table.SCS_TABLE);
        Mockito.verify(dbHandler, Mockito.never()).updateInto(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dbHandler).insertInto(Table.SCS_TABLE, BRANCH, eventId);

    }

    @Test
    public void testConnectionErrorsHandled() throws Exception {
        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, BRANCH)).thenThrow(new ConnectException("Test Exception"));

        final String eventId = "event-id";

        exception.expect(ConnectException.class);
        sourceChangeSubmittedState.saveEiffelEventId(BRANCH, eventId, Table.SCS_TABLE);

        Mockito.verify(dbHandler, Mockito.never()).updateInto(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dbHandler, Mockito.never()).insertInto(Mockito.any(), Mockito.any(), Mockito.any());

        assertEquals("Error occured return empty string.", "",
                sourceChangeSubmittedState.getEventId("", ""));

    }

}
