package com.ericsson.gerrit.plugins.eiffel.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import com.ericsson.gerrit.plugins.eiffel.exceptions.EiffelEventSenderException;

public class EiffelEventSenderTest {

    private EiffelPluginConfiguration pluginConfig;
    private HttpRequest httpRequest;
    private ResponseEntity response;

    private static final String EIFFEL_TYPE = "EiffelSourceChangeCreatedEvent";
    private static final int STATUS_OK = HttpStatus.SC_OK;
    private static final int STATUS_NOT_FOUND = HttpStatus.SC_NOT_FOUND;

    @Before
    public void beforeTest() throws IOException, URISyntaxException {
        setUpMockObjects();
    }

    @Test
    public void testEventSender() throws IOException, URISyntaxException {
        setUpMockActions();

        EiffelEventSender sender = new EiffelEventSender(pluginConfig, httpRequest);
        sender.setMessage(new EiffelSourceChangeCreatedEvent());
        sender.setType(EIFFEL_TYPE);

        try {
            Whitebox.invokeMethod(sender, "generateAndPublish");
        } catch (Exception e) {
            String exceptionType = e.getClass().getSimpleName();
            String assertError = String.format(
                    "No exception should have been thrown but was %s",
                    exceptionType);
            fail(assertError);
        }
    }

    @Test
    public void testEventSenderWithMissingConfiguration() throws IOException, URISyntaxException {
        EiffelEventSender sender = new EiffelEventSender(pluginConfig, httpRequest);
        sender.setMessage(new EiffelSourceChangeCreatedEvent());
        sender.setType("");

        try {
            Whitebox.invokeMethod(sender, "generateAndPublish");
        } catch (Exception e) {
            String exceptionName = e.getClass().getSimpleName();
            String exceptionError = String.format(
                    "Exception should have been of type EventSenderException but was %s",
                    exceptionName);
            String eiffelTypeError = "eiffelType should have been blank";
            String eiffelType = Whitebox.getInternalState(sender, "eiffelType");
            assertTrue(exceptionError, e instanceof EiffelEventSenderException);
            assertEquals(eiffelTypeError, "", eiffelType);
        }
    }

    @Test
    public void testEventSenderWithBadStatus() throws IOException, URISyntaxException {
        setUpMockActionsWithBadStatus();

        EiffelEventSender sender = new EiffelEventSender(pluginConfig, httpRequest);
        sender.setMessage(new EiffelSourceChangeCreatedEvent());
        sender.setType(EIFFEL_TYPE);

        try {
            Whitebox.invokeMethod(sender, "generateAndPublish");
        } catch (Exception e) {
            String exceptionName = e.getClass().getSimpleName();
            String exceptionError = String.format(
                    "Exception should have been of type EventSenderException but was %s",
                    exceptionName);
            String statusError = String.format(
                    "Error message should have contained status code %s",
                    STATUS_NOT_FOUND);
            assertTrue(exceptionError, e instanceof EiffelEventSenderException);
            assertTrue(statusError, e.getMessage().contains(String.valueOf(STATUS_NOT_FOUND)));
        }
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
