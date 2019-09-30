package com.ericsson.gerrit.plugins.eiffel.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.duraci.datawrappers.scm.identifiers.Git;
import com.ericsson.duraci.eiffelmessage.messages.events.EiffelSCMChangedEvent;
import com.ericsson.duraci.eiffelmessage.sending.MessageSender;
import com.ericsson.duraci.eiffelmessage.sending.exceptions.EiffelMessageSenderException;
import com.ericsson.gerrit.plugins.eiffel.handlers.StateHandler;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.ericsson.gerrit.plugins.eiffel.*")
public class SCMChangedMessageSenderTest {

    private MessageSender messageSender;
    private EiffelSCMChangedEvent eiffelSCMChangedEvent;
    private StateHandler stateHandler;
    String sha1 = "sha1";
    String branch = "branch";
    String project = "project";
    URI uri;
    Git scmId;

    @Before
    public void init() throws Exception {
        messageSender = mock(MessageSender.class);
        eiffelSCMChangedEvent = mock(EiffelSCMChangedEvent.class);
        stateHandler = mock(StateHandler.class);

        PowerMockito.whenNew(StateHandler.class).withAnyArguments().thenReturn(stateHandler);

        when(stateHandler.getLastSentChangeMergedEiffelEventId(Mockito.any(), Mockito.any())).thenReturn("");

        uri = new URI("uri");
        scmId = new Git(sha1, branch, project, uri);
    }

    @Test
    public void test_getLastSentChangeMergedEiffelEventId_returns_event_id() throws EiffelMessageSenderException {
        when(stateHandler.getLastSentChangeMergedEiffelEventId(Mockito.any(), Mockito.any())).thenReturn("event-id");

        new SCMChangedMessageSender(messageSender, eiffelSCMChangedEvent, scmId, null, null).sendMessage();

        Mockito.verify(eiffelSCMChangedEvent).setPreviousEvents(Mockito.any());
    }

    @Test
    public void test_SCMChangedMessageSender_Constructor() throws URISyntaxException {
        assertThatCode(() -> new SCMChangedMessageSender(messageSender, eiffelSCMChangedEvent, scmId, null,
                null)).doesNotThrowAnyException();
    }

    @Test
    public void test_SCMChangedMessageSender_sendMessage() throws EiffelMessageSenderException {

        when(messageSender.send(Mockito.any())).thenReturn(null);
        Mockito.doNothing().when(messageSender).dispose();

        SCMChangedMessageSender changedMessageSender = new SCMChangedMessageSender(messageSender, eiffelSCMChangedEvent,
                scmId, null, null);
        assertThatCode(() -> changedMessageSender.sendMessage()).doesNotThrowAnyException();
    }

    @Test
    public void test_SCMChangedMessageSender_sendMessage_Exception() throws EiffelMessageSenderException {
        Mockito.doThrow(EiffelMessageSenderException.class).when(messageSender).send(Mockito.any());

        SCMChangedMessageSender changedMessageSender = new SCMChangedMessageSender(messageSender, eiffelSCMChangedEvent,
                scmId, null, null);
        assertThatCode(() -> changedMessageSender.sendMessage()).doesNotThrowAnyException();
    }
}