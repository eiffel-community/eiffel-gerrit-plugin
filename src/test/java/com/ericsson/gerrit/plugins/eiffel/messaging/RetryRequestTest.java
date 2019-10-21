package com.ericsson.gerrit.plugins.eiffel.messaging;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ericsson.eiffelcommons.utils.HttpRequest;
import com.ericsson.eiffelcommons.utils.ResponseEntity;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.configuration.RetryConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeCreatedEvent;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RetryConfiguration.class)
public class RetryRequestTest {
    private EiffelPluginConfiguration pluginConfig;
    private HttpRequest httpRequest;
    private ResponseEntity response;
    private MutableInt counter = new MutableInt();

    private static final String EIFFEL_TYPE = "EiffelSourceChangeCreatedEvent";
    private static final int STATUS_ERROR = HttpStatus.SC_INTERNAL_SERVER_ERROR;

    @Autowired
    private RetryTemplate retryTemplate;

    @Before
    public void beforeTest() throws IOException, URISyntaxException {
        setUpMockObjects();
    }

    @Test(expected = RuntimeException.class)
    public void testRetryLogic() throws URISyntaxException, IOException {
        setUpMockActions();
        counter.setValue(0);

        EiffelEventSender sender = new EiffelEventSender(pluginConfig, httpRequest);
        retryTemplate.execute(new RetryCallback<Void, RuntimeException>() {
            @Override
            public Void doWithRetry(RetryContext context) {
                counter.setValue(counter.getValue().intValue() + 1);
                sender.setEiffelEventMessage(new EiffelSourceChangeCreatedEvent());
                sender.setEiffelEventType(EIFFEL_TYPE);
                sender.send();
                return null;
            }
        });

        int expectedValue = 5;
        int actualValue = counter.getValue().intValue();
        String errorMessage = String.format("Expected retry counter to be %d but was %d",
                expectedValue, actualValue);
        assertEquals(errorMessage, expectedValue, actualValue);
    }

    private void setUpMockObjects() throws URISyntaxException, IOException {
        httpRequest = Mockito.mock(HttpRequest.class);
        pluginConfig = Mockito.mock(EiffelPluginConfiguration.class);
        response = Mockito.mock(ResponseEntity.class);
    }

    private void setUpMockActions() throws URISyntaxException, IOException {
        Mockito.when(httpRequest.performRequest()).thenReturn(response);
        Mockito.when(response.getStatusCode()).thenReturn(STATUS_ERROR);
        Mockito.when(response.getBody()).thenReturn("");
        Mockito.when(pluginConfig.getRemremPublishURL()).thenReturn("");
        Mockito.when(pluginConfig.getRemremUsername()).thenReturn("");
        Mockito.when(pluginConfig.getRemremPassword()).thenReturn("");
    }
}
