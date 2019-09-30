package com.ericsson.gerrit.plugins.eiffel.messaging;

import static org.assertj.core.api.Assertions.*;
import static org.powermock.api.mockito.PowerMockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ericsson.duraci.eiffelmessage.sending.exceptions.EiffelMessageSenderException;
import com.ericsson.gerrit.plugins.eiffel.messaging.SCMChangedMessageSender;
import com.ericsson.gerrit.plugins.eiffel.messaging.SendSCMChangedMessageTask;

public class SendSCMChangedMessageTaskTest {

    private final String SHA1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    private SCMChangedMessageSender scmChangedMessageSender;

    @Before
    public void init() {
        scmChangedMessageSender = mock(SCMChangedMessageSender.class);
    }

    @Test
    public void test_SendSCMChangedMessageTask_Constructor() {
        assertThatCode(() -> new SendSCMChangedMessageTask(scmChangedMessageSender, SHA1)).doesNotThrowAnyException();
    }

    @Test
    public void test_SendSCMChangedMessageTask_run() throws EiffelMessageSenderException {
        Mockito.doNothing().when(scmChangedMessageSender).sendMessage();

        SendSCMChangedMessageTask messageTask = new SendSCMChangedMessageTask(scmChangedMessageSender, SHA1);

        assertThatCode(() -> messageTask.run()).doesNotThrowAnyException();
    }

    @Test
    public void test_SendSCMChangedMessageTask_run_Exception() throws EiffelMessageSenderException {
        Mockito.doThrow(EiffelMessageSenderException.class).when(scmChangedMessageSender).sendMessage();

        SendSCMChangedMessageTask messageTask = new SendSCMChangedMessageTask(scmChangedMessageSender, SHA1);
        messageTask.run();

        assertThatThrownBy(() -> scmChangedMessageSender.sendMessage())
                .isInstanceOf(EiffelMessageSenderException.class);
    }

    @Test
    public void test_SendSCMChangedMessageTask_toString() {
        SendSCMChangedMessageTask messageTask = new SendSCMChangedMessageTask(scmChangedMessageSender, SHA1);
        String result = messageTask.toString();

        assertThat(result.contains(SHA1)).isTrue();
    }
}