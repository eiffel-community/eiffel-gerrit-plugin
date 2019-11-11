package com.ericsson.gerrit.plugins.eiffel.logHelper;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.LoggingEvent;
import org.assertj.core.util.Lists;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class LogHelper {

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;
    private List<Appender> otherAppenders = Lists.emptyList();
    private boolean checkVerifyAtTeardown;
    private int verifyTimesAtTeardwon;

    public LogHelper() {
        mockAppender = mock(Appender.class);
        captorLoggingEvent = ArgumentCaptor.forClass(LoggingEvent.class);
    }

    public void setup() {
        checkVerifyAtTeardown = false;
        verifyTimesAtTeardwon = 0;

        org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();
        rootLogger.addAppender(mockAppender);
    }

    public void tearDown() {
        if (checkVerifyAtTeardown) {
            verifyLoggerCalledTimes(verifyTimesAtTeardwon);
        }

        org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();
        rootLogger.removeAppender(mockAppender);

        restoreStdoutAppenders(rootLogger);
    }

    public void removeStdoutAppenders() {
        org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();

        @SuppressWarnings("unchecked")
        Enumeration<Appender> enumeration = rootLogger.getAllAppenders();
        /*
         * A note on mockito and equals: Testing equality with mock objects depends on the context.
         * When removing a the mock from the root logger mockito will call an '=='. Here mockito
         * interprets it as another context and thus does not call '=='.
         */
        otherAppenders = Collections.list(enumeration)
                                    .stream()
                                    .filter(appender -> appender != mockAppender)
                                    .collect(Collectors.toList());

        otherAppenders.forEach(appender -> rootLogger.removeAppender(appender));
    }

    public void verifyLoggerCalledTimes(int times) {
        verify(mockAppender, times(times)).doAppend(captorLoggingEvent.capture());
    }

    public void expectLoggerCalledTimes(int times) {
        checkVerifyAtTeardown = true;
        verifyTimesAtTeardwon = times;
    }

    private void restoreStdoutAppenders(org.apache.log4j.Logger rootLogger) {
        for (Appender appender : otherAppenders) {
            rootLogger.addAppender((Appender) appender);
        }
        otherAppenders.clear();
    }

}
