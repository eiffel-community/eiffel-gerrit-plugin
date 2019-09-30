package com.ericsson.gerrit.plugins.eiffel.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.duraci.eiffelmessage.sending.exceptions.EiffelMessageSenderException;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.messaging.EiffelMessageBusSender;
import com.ericsson.gerrit.plugins.eiffel.messaging.SCMChangedMessageSender;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Provider;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.ericsson.gerrit.plugins.eiffel.*")
public class EiffelTestMessageSenderTest {

    private final String PLUGIN_NAME = "plugin";
    private final String USER_NAME = "username";
    private final String SUCCESS = "Message successfully sent";

    private Provider<CurrentUser> user;
    private ProjectResource resource;
    private CurrentUser currentUser;
    private EiffelMessageBusSender eiffelMessageBusSender;
    private SCMChangedMessageSender scmChangedMessageSender;
    private EiffelPluginConfiguration eiffelPluginConfigurationMock;

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws Exception {
        user = mock(Provider.class);
        resource = mock(ProjectResource.class);
        currentUser = mock(CurrentUser.class);
        eiffelMessageBusSender = mock(EiffelMessageBusSender.class);
        scmChangedMessageSender = mock(SCMChangedMessageSender.class);
        eiffelPluginConfigurationMock = createNiceMock(EiffelPluginConfiguration.class);

        when(user.get()).thenReturn(currentUser);
        when(currentUser.getUserName()).thenReturn(USER_NAME);
        whenNew(EiffelMessageBusSender.class).withAnyArguments().thenReturn(eiffelMessageBusSender);
        whenNew(EiffelPluginConfiguration.class).withAnyArguments().thenReturn(eiffelPluginConfigurationMock);
    }

    @Test
    public void test_TestMessageAction_Constructor() {
        assertThatCode(() -> new EiffelTestMessageSender(user, PLUGIN_NAME)).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_TestMessageAction_apply_Exception1() throws Exception {
        whenNew(URI.class).withAnyArguments().thenThrow(URISyntaxException.class);

        EiffelTestMessageSender messageAction = new EiffelTestMessageSender(user, PLUGIN_NAME);
        String result = messageAction.apply(resource, null);

        assertThatThrownBy(() -> new URI(Mockito.any())).isInstanceOf(URISyntaxException.class);
        Mockito.verifyZeroInteractions(eiffelMessageBusSender);
        assertThat(result != SUCCESS).isTrue();
    }

    @Test
    public void test_TestMessageAction_apply_Exception4() throws Exception {
        Mockito.doReturn(scmChangedMessageSender)
               .when(eiffelMessageBusSender)
               .createSCMChangedMessageSender(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doThrow(EiffelMessageSenderException.class).when(scmChangedMessageSender).sendMessage();

        EiffelTestMessageSender messageAction = new EiffelTestMessageSender(user, PLUGIN_NAME);
        String result = messageAction.apply(resource, null);

        assertThatThrownBy(() -> scmChangedMessageSender.sendMessage()).isInstanceOf(
                EiffelMessageSenderException.class);
        assertThat(result != SUCCESS).isTrue();
    }

    @Test
    public void test_TestMessageAction_apply() throws Exception {
        Mockito.doReturn(scmChangedMessageSender)
               .when(eiffelMessageBusSender)
               .createSCMChangedMessageSender(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(scmChangedMessageSender).sendMessage();

        EiffelTestMessageSender messageAction = new EiffelTestMessageSender(user, PLUGIN_NAME);
        String result = messageAction.apply(resource, null);

        Mockito.verify(scmChangedMessageSender, Mockito.times(1)).sendMessage();
        assertThat(result == SUCCESS).isTrue();
    }

    @Test
    public void test_TestMessageAction_getDescription() {
        EiffelTestMessageSender messageAction = new EiffelTestMessageSender(user, PLUGIN_NAME);
        assertThatCode(() -> messageAction.getDescription(resource)).doesNotThrowAnyException();
    }
}
