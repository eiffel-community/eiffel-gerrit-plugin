package com.ericsson.gerrit.plugins.eiffel.listeners;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.duraci.datawrappers.scm.SCMWorkItem;
import com.ericsson.duraci.datawrappers.scm.identifiers.Git;
import com.ericsson.eiffel.utils.parser.message.SCMChangeMessageParser;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.handlers.MessageQueueHandler;
import com.ericsson.gerrit.plugins.eiffel.messaging.EiffelMessageBusSender;
import com.google.common.base.Supplier;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.ericsson.gerrit.plugins.eiffel.*")
public class ChangeMergedListenerTest {
    // Default constants
    private final String PLUGIN_NAME = "plugin";
    private final String BRANCH = "branch";
    private final String REPOSITORY = "repo_test";
    private final String CHANGE_URL = "http://localhost:8080/13";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private MessageQueueHandler messageQueueMock;
    private ChangeEvent changeEventMock;
    private ChangeMergedEvent changeMergeEventMock;
    private Supplier<ChangeAttribute> supplierChangeAttributeMock;
    private ChangeAttribute changeAttributeMock;
    private Supplier<PatchSetAttribute> supplierPatchSetAttributeMock;
    private PatchSetAttribute patchSetAttributeMock;
    private EiffelMessageBusSender eiffelMessageBusSenderMock;
    private SCMChangeMessageParser scmChangeMessageParserMock;
    private List<SCMWorkItem> listSCMWorkItemsMock;
    private AccountAttribute accountAttribute;
    private ScheduledThreadPoolExecutor threadPoolExecutorMock;
    private Git gitMock;
    private EiffelPluginConfiguration eiffelPluginConfigurationMock;
    private ChangeMergedListener changeMergedListener;

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws Exception {
        gitMock = mock(Git.class);
        messageQueueMock = mock(MessageQueueHandler.class);
        changeMergeEventMock = mock(ChangeMergedEvent.class);
        supplierChangeAttributeMock = mock(Supplier.class);
        changeMergeEventMock.change = supplierChangeAttributeMock;
        changeAttributeMock = mock(ChangeAttribute.class);
        changeAttributeMock.url = CHANGE_URL;
        supplierPatchSetAttributeMock = mock(Supplier.class);
        changeMergeEventMock.patchSet = supplierPatchSetAttributeMock;
        patchSetAttributeMock = mock(PatchSetAttribute.class);
        eiffelMessageBusSenderMock = mock(EiffelMessageBusSender.class);
        scmChangeMessageParserMock = mock(SCMChangeMessageParser.class);
        listSCMWorkItemsMock = mock(List.class);
        accountAttribute = mock(AccountAttribute.class);
        patchSetAttributeMock.author = accountAttribute;
        threadPoolExecutorMock = mock(ScheduledThreadPoolExecutor.class);
        eiffelPluginConfigurationMock = mock(EiffelPluginConfiguration.class);

        // Add default return values
        whenNew(EiffelPluginConfiguration.class).withAnyArguments().thenReturn(eiffelPluginConfigurationMock);
        when(eiffelPluginConfigurationMock.isEnabled()).thenReturn(true);
        when(eiffelPluginConfigurationMock.getFilter()).thenReturn("");

        changeMergedListener = new ChangeMergedListener(PLUGIN_NAME, messageQueueMock, null);
    }

    // Test AbstractListener functions **

    @Test
    public void testPluginDisabledNoActions() throws Exception {
        when(eiffelPluginConfigurationMock.isEnabled()).thenReturn(false);

        assertFalse(changeMergedListener.messageShouldBeSent(eiffelPluginConfigurationMock, BRANCH, REPOSITORY));
    }

    @Test
    public void testPluginEnabled() throws Exception {
        assertTrue(changeMergedListener.messageShouldBeSent(eiffelPluginConfigurationMock, BRANCH, REPOSITORY));
    }

    @Test
    public void testPluginFilterIsNull() throws Exception {
        when(eiffelPluginConfigurationMock.getFilter()).thenReturn(null);

        assertTrue(changeMergedListener.messageShouldBeSent(eiffelPluginConfigurationMock, BRANCH, REPOSITORY));
    }

    @Test
    public void testPluginFilterIsCorrect() throws Exception {
        when(eiffelPluginConfigurationMock.getFilter()).thenReturn(BRANCH);

        assertTrue(changeMergedListener.messageShouldBeSent(eiffelPluginConfigurationMock, BRANCH, REPOSITORY));
    }

    @Test
    public void testPluginFilterIsSomethingelse() throws Exception {
        when(eiffelPluginConfigurationMock.getFilter()).thenReturn("something_else");

        assertFalse(changeMergedListener.messageShouldBeSent(eiffelPluginConfigurationMock, BRANCH, REPOSITORY));
    }

    // Test ChangeMergeListener functions **

    @Test
    public void testChangeMergedListenerConstructor() {
        assertThatCode(() -> new ChangeMergedListener(PLUGIN_NAME, messageQueueMock, null)).doesNotThrowAnyException();
    }

    @Test
    public void testChangeMergedListenerOnEventWrongEvent() {
        changeMergedListener.onEvent(changeEventMock);

        Mockito.verifyZeroInteractions(supplierChangeAttributeMock);
    }

    @Test
    public void testChangeMergedListenerOnEventShouldNotSend() throws Exception {
        when(supplierChangeAttributeMock.get()).thenReturn(changeAttributeMock);
        when(supplierPatchSetAttributeMock.get()).thenReturn(patchSetAttributeMock);

        whenNew(EiffelMessageBusSender.class).withAnyArguments().thenReturn(eiffelMessageBusSenderMock);

        when(eiffelPluginConfigurationMock.isEnabled()).thenReturn(false);

        changeMergedListener.onEvent(changeMergeEventMock);

        // verify that function after try catch was not called
        Mockito.verifyZeroInteractions(gitMock);
        Mockito.verify(eiffelMessageBusSenderMock, Mockito.times(0)).getCustomPatterns();
    }

    @Test
    public void testChangeMergedListenerOnEventUriException() throws Exception {
        when(supplierChangeAttributeMock.get()).thenReturn(changeAttributeMock);
        when(supplierPatchSetAttributeMock.get()).thenReturn(patchSetAttributeMock);

        whenNew(EiffelMessageBusSender.class).withAnyArguments().thenReturn(eiffelMessageBusSenderMock);
        whenNew(URI.class).withAnyArguments().thenThrow(new URISyntaxException("Test exception", "test"));

        changeMergedListener.onEvent(changeMergeEventMock);

        // verify that function after try catch was not called
        Mockito.verifyZeroInteractions(gitMock);
        Mockito.verify(eiffelMessageBusSenderMock, Mockito.times(0)).getCustomPatterns();
    }

    @Test
    public void test_ChangeMergedListener_onEvent() throws Exception {
        when(supplierChangeAttributeMock.get()).thenReturn(changeAttributeMock);
        when(supplierPatchSetAttributeMock.get()).thenReturn(patchSetAttributeMock);

        whenNew(EiffelMessageBusSender.class).withAnyArguments().thenReturn(eiffelMessageBusSenderMock);
        whenNew(SCMChangeMessageParser.class).withAnyArguments().thenReturn(scmChangeMessageParserMock);

        when(scmChangeMessageParserMock.getWorkItemsList()).thenReturn(listSCMWorkItemsMock);
        when(messageQueueMock.getPool()).thenReturn(threadPoolExecutorMock);

        ChangeMergedListener listener = new ChangeMergedListener(PLUGIN_NAME, messageQueueMock, null);
        listener.onEvent(changeMergeEventMock);

        // Verify that send message was called
        Mockito.verify(eiffelMessageBusSenderMock, Mockito.times(1))
               .createSCMChangedMessageTask(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

    }
}