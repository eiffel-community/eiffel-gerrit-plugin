package com.ericsson.gerrit.plugins.eiffel.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

import com.ericsson.eiffelcommons.utils.HttpRequest;
import com.ericsson.eiffelcommons.utils.ResponseEntity;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeCreatedEvent;
import com.ericsson.gerrit.plugins.eiffel.exceptions.HttpRequestFailedException;
import com.ericsson.gerrit.plugins.eiffel.exceptions.MissingConfigurationException;

public class EiffelEventSenderTest {

    private EiffelPluginConfiguration pluginConfig;
    private HttpRequest httpRequest;
    private ResponseEntity response;

    private static final String EIFFEL_TYPE = "EiffelSourceChangeCreatedEvent";
    private static final int STATUS_OK = HttpStatus.SC_OK;
    private static final int STATUS_NOT_FOUND = HttpStatus.SC_INTERNAL_SERVER_ERROR;

    @Before
    public void beforeTest() throws IOException, URISyntaxException {
        setUpMockObjects();
    }

    @Test
    public void testEventSender() throws Exception {
        setUpMockActions();

        EiffelEventSender sender = new EiffelEventSender(pluginConfig, httpRequest);
        sender.setEiffelEventMessage(new EiffelSourceChangeCreatedEvent());
        sender.setEiffelEventType(EIFFEL_TYPE);

        assertThatCode(() -> { Whitebox.invokeMethod(sender, "generateAndPublish"); }).doesNotThrowAnyException();
    }

    @Test(expected = MissingConfigurationException.class)  
    public void testEventSenderWithMissingConfiguration() throws Exception {
        EiffelEventSender sender = new EiffelEventSender(pluginConfig, httpRequest);
        sender.setEiffelEventMessage(new EiffelSourceChangeCreatedEvent());
        sender.setEiffelEventType("");

        Whitebox.invokeMethod(sender, "verifyConfiguration");
    }

    @Test(expected = HttpRequestFailedException.class)  
    public void testEventSenderWithBadStatus() throws Exception {
        setUpMockActionsWithBadStatus();

        EiffelEventSender sender = new EiffelEventSender(pluginConfig, httpRequest);
        sender.setEiffelEventMessage(new EiffelSourceChangeCreatedEvent());
        sender.setEiffelEventType(EIFFEL_TYPE);

        Whitebox.invokeMethod(sender, "generateAndPublish");
    }

    private void setUpMockObjects() throws URISyntaxException, IOException {
        httpRequest = Mockito.mock(HttpRequest.class);
        pluginConfig = Mockito.mock(EiffelPluginConfiguration.class);
        response = Mockito.mock(ResponseEntity.class);
    }

    private void setUpMockActions() throws URISyntaxException, IOException {
        Mockito.when(httpRequest.performRequest()).thenReturn(response);
        Mockito.when(response.getStatusCode()).thenReturn(STATUS_OK);
        Mockito.when(response.getBody()).thenReturn("");
        Mockito.when(pluginConfig.getRemremPublishURL()).thenReturn("");
        Mockito.when(pluginConfig.getRemremUsername()).thenReturn("");
        Mockito.when(pluginConfig.getRemremPassword()).thenReturn("");
    }

    private void setUpMockActionsWithBadStatus() throws URISyntaxException, IOException {
        Mockito.when(httpRequest.performRequest()).thenReturn(response);
        Mockito.when(response.getStatusCode()).thenReturn(STATUS_NOT_FOUND);
        Mockito.when(response.getBody()).thenReturn("");
    }
}
