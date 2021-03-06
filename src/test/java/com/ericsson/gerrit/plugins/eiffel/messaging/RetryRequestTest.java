package com.ericsson.gerrit.plugins.eiffel.messaging;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.ericsson.eiffelcommons.utils.HttpRequest;
import com.ericsson.eiffelcommons.utils.ResponseEntity;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.configuration.RetryConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeCreatedEvent;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeSubmittedEvent;
import com.ericsson.gerrit.plugins.eiffel.events.generators.EiffelSourceChangeSubmittedEventGenerator;
import com.ericsson.gerrit.plugins.eiffel.git.CommitInformation;
import com.ericsson.gerrit.plugins.eiffel.listeners.AbstractEventListener;
import com.ericsson.gerrit.plugins.eiffel.listeners.ChangeMergedEventListener;
import com.ericsson.gerrit.plugins.eiffel.loghelper.LogHelper;
import com.google.gerrit.server.events.ChangeMergedEvent;

import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.vavr.control.Try;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ EiffelSourceChangeSubmittedEventGenerator.class,
        ChangeMergedEventListener.class })
public class RetryRequestTest {
    private EiffelPluginConfiguration pluginConfig;
    private HttpRequest httpRequest;
    private ResponseEntity response;
    private ChangeMergedEvent changeMergedEvent;
    private final MutableInt counter = new MutableInt();
    private final EiffelSourceChangeSubmittedEvent eiffelEvent = new EiffelSourceChangeSubmittedEvent();

    private static final String EIFFEL_TYPE = "EiffelSourceChangeCreatedEvent";
    private static final int STATUS_ERROR = HttpStatus.SC_INTERNAL_SERVER_ERROR;
    private static final String PLUGIN_NAME = "Eiffel-Integration";
    private static final File FILE_DIR = new File("");
    private CommitInformation commitInformation;

    private final LogHelper logHelper = new LogHelper();

    @Before
    public void beforeTest() throws IOException, URISyntaxException {
        setUpMockObjects();
        logHelper.setup();
    }

    @After
    public void tearDown() {
        logHelper.tearDown();
    }

    @Test
    public void testRetryLogic() throws URISyntaxException, IOException {
        logHelper.removeStdoutAppenders();
        setUpMockActions();
        counter.setValue(0);

        final RetryConfiguration retryConfiguration = new RetryConfiguration();
        final EiffelEventSender eiffelEventSender = new EiffelEventSender(pluginConfig, httpRequest);
        eiffelEventSender.setEiffelEventMessage(new EiffelSourceChangeCreatedEvent());
        eiffelEventSender.setEiffelEventType(EIFFEL_TYPE);

        final Retry policy = retryConfiguration.getRetryPolicy();
        final Runnable decoratedRunnable = Decorators.ofRunnable(() -> {
            counter.setValue(counter.getValue().intValue() + 1);
            eiffelEventSender.send();
        }).withRetry(policy).decorate();
        Try.runRunnable(decoratedRunnable);

        final int expectedValue = retryConfiguration.getMaxAttempts();
        final int actualValue = counter.getValue().intValue();
        final String errorMessage = String.format("Expected retry counter to be %d but was %d",
                expectedValue, actualValue);
        assertEquals(errorMessage, expectedValue, actualValue);

        logHelper.verifyLoggerCalledTimes(3);
    }

    @Test(expected = Test.None.class)
    public void testPrepareAndSendEiffelEvent() throws Exception {
        logHelper.removeStdoutAppenders();
        setUpMockActions();
        setUpMocksAndActionsForMethodInvoke();

        final RetryConfiguration retryConfiguration = new RetryConfiguration();
        final ChangeMergedEventListener listener = new ChangeMergedEventListener(PLUGIN_NAME, FILE_DIR);
        Whitebox.setInternalState(listener, "retryConfiguration", retryConfiguration);
        Whitebox.invokeMethod(listener, "sendEiffelEvent", eiffelEvent,
                pluginConfig);

        final ThreadPoolExecutor executor = Whitebox.<ThreadPoolExecutor>getInternalState(
                AbstractEventListener.class, "executor");
        int expectedValue = 1;
        int actualValue = executor.getActiveCount();
        String errorMessage = String.format("Expected active jobs to be %d but was %d",
                expectedValue, actualValue);
        assertEquals(errorMessage, expectedValue, actualValue);

        boolean finished = false;
        final long stopTime = System.currentTimeMillis() + 30000;
        while (!finished && stopTime > System.currentTimeMillis()) {
            if (executor.getActiveCount() > 0) {
                Thread.sleep(1000);
            } else {
                finished = true;
            }
        }

        expectedValue = 0;
        actualValue = executor.getActiveCount();
        errorMessage = String.format("Expected active jobs to be %d but was %d",
                expectedValue, actualValue);
        assertEquals(errorMessage, expectedValue, actualValue);

        logHelper.verifyLoggerCalledTimes(3);
    }

    private void setUpMockObjects() throws URISyntaxException, IOException {
        httpRequest = Mockito.mock(HttpRequest.class);
        pluginConfig = Mockito.mock(EiffelPluginConfiguration.class);
        response = Mockito.mock(ResponseEntity.class);
        changeMergedEvent = Mockito.mock(ChangeMergedEvent.class);
        commitInformation = Mockito.mock(CommitInformation.class);
    }

    private void setUpMockActions() throws URISyntaxException, IOException {
        Mockito.when(httpRequest.performRequest()).thenReturn(response);
        Mockito.when(response.getStatusCode()).thenReturn(STATUS_ERROR);
        Mockito.when(response.getBody()).thenReturn("");
        Mockito.when(pluginConfig.getRemremPublishURL()).thenReturn("");
        Mockito.when(pluginConfig.getRemremUsername()).thenReturn("");
        Mockito.when(pluginConfig.getRemremPassword()).thenReturn("");
    }

    private void setUpMocksAndActionsForMethodInvoke() throws Exception {
        PowerMockito.mockStatic(EiffelSourceChangeSubmittedEventGenerator.class);
        Mockito.when(
                EiffelSourceChangeSubmittedEventGenerator.generate(pluginConfig, changeMergedEvent,
                        commitInformation))
               .thenReturn(eiffelEvent);
        final EiffelEventSender sender = new EiffelEventSender(pluginConfig, httpRequest);
        PowerMockito.whenNew(EiffelEventSender.class)
                    .withArguments(pluginConfig)
                    .thenReturn(sender);
    }
}
