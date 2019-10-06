package com.ericsson.gerrit.plugins.eiffel.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;

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

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.ericsson.gerrit.plugins.eiffel.*")
public class StateHandlerTest {

    private static final String FILE_ENDING = "db";
    private static final String PROJECT = "project_test";
    private static final String BRANCH = "branch_test";
    private DataBaseHandler dbHandler;
    private StateHandler stateHandler;
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private File tmpFolderPath;

    @Rule
    public final ExpectedException exception = ExpectedException.none();


    @Before
    public void init() throws Exception {
        tmpFolderPath = testFolder.newFolder();
        dbHandler = mock(DataBaseHandler.class);
        String fileName = String.format("%s.%s", PROJECT, FILE_ENDING);
        PowerMockito.whenNew(DataBaseHandler.class).withArguments(tmpFolderPath, fileName)
                .thenReturn(dbHandler);
        stateHandler = new StateHandler(tmpFolderPath);
    }

    @Test
    public void testBuildParentFilePath() throws Exception {
        String projectName = "parent/child";
        String expectedParentPath = tmpFolderPath + "/" + "parent";

        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, BRANCH)).thenReturn("");
        exception.expect(NoSuchElementException.class);
        stateHandler.setLastSourceChangeSubmittedEiffelEvent(projectName, BRANCH, "someID");

        File parentDirectory = new File(expectedParentPath);
        assertTrue(parentDirectory.exists());
    }

    @Test
    public void testgetLastSentEvent() throws Exception {
        Mockito.when(dbHandler.getEventID(Mockito.any(), Mockito.any())).thenReturn("eventID");

        String eventId = stateHandler.getLastSourceChangeSubmittedEiffelEvent(PROJECT, BRANCH);
        assertEquals("Table.SCS_TABLE tabler key should be", "eventID", eventId);

        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, ""))
                .thenThrow(new ConnectException("Exception thrown by test"));
        exception.expect(NoSuchElementException.class);
        stateHandler.getLastSourceChangeSubmittedEiffelEvent("", "");

    }

    @Test
    public void testUpdateLastEventCalled() throws Exception {
        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, BRANCH)).thenReturn("old-event-id");

        String eventId = "event-id";
        stateHandler.setLastSourceChangeSubmittedEiffelEvent(PROJECT, BRANCH, eventId);
        Mockito.verify(dbHandler).updateInto(Table.SCS_TABLE, BRANCH, eventId);
        Mockito.verify(dbHandler, Mockito.never()).insertInto(Mockito.any(), Mockito.any(), Mockito.any());

    }

    @Test
    public void testInsertLastEventCalled() throws Exception {
        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, BRANCH)).thenReturn("");

        String eventId = "event-id";
        stateHandler.setLastSourceChangeSubmittedEiffelEvent(PROJECT, BRANCH, eventId);
        Mockito.verify(dbHandler, Mockito.never()).updateInto(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dbHandler).insertInto(Table.SCS_TABLE, BRANCH, eventId);

    }

    @Test
    public void testConnectionErrorsHandled() throws Exception {
        Mockito.when(dbHandler.getEventID(Table.SCS_TABLE, PROJECT)).thenThrow(new ConnectException("Test Exception"));

        String eventId = "event-id";

        exception.expect(NoSuchElementException.class);
        stateHandler.setLastSourceChangeSubmittedEiffelEvent(PROJECT, BRANCH, eventId);

        Mockito.verify(dbHandler, Mockito.never()).updateInto(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dbHandler, Mockito.never()).insertInto(Mockito.any(), Mockito.any(), Mockito.any());

        assertEquals("Error occured return empty string.", "",
                stateHandler.getLastSourceChangeSubmittedEiffelEvent("", ""));

    }

}
