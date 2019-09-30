package com.ericsson.gerrit.plugins.eiffel.configuration;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.duraci.datawrappers.Arm;
import com.ericsson.duraci.datawrappers.MessageBus;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelMessageBusConfiguration;

public class EiffelMessageBusConfigurationTest {

    private final int QUEUE_LENGTH = 10;
    private final String DOMAIN_ID = "test-domain";

    private Map<String, Arm> arms;
    private MessageBus messageBus;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        arms = mock(HashMap.class);
        messageBus = mock(MessageBus.class);
    }

    @Test
    public void test_EiffelMessageBusConfiguration_Constructor() {
        assertThatCode(() -> new EiffelMessageBusConfiguration(arms, messageBus, DOMAIN_ID, QUEUE_LENGTH))
                .doesNotThrowAnyException();
    }

    @Test
    public void test_EiffelMessageBusConfiguration_getArms() {
        EiffelMessageBusConfiguration mbConfig = new EiffelMessageBusConfiguration(arms, messageBus, DOMAIN_ID,
                QUEUE_LENGTH);
        assertEquals(mbConfig.getArms(), arms);
    }

    @Test
    public void test_EiffelMessageBusConfiguration_getMessageBus() {
        EiffelMessageBusConfiguration mbConfig = new EiffelMessageBusConfiguration(arms, messageBus, DOMAIN_ID,
                QUEUE_LENGTH);
        assertEquals(mbConfig.getMessageBus().getHostName(), null);
    }

    @Test
    public void test_EiffelMessageBusConfiguration_getMessageSendQueue() {
        EiffelMessageBusConfiguration mbConfig = new EiffelMessageBusConfiguration(arms, messageBus, DOMAIN_ID,
                QUEUE_LENGTH);
        assertEquals(mbConfig.getMessageSendQueue().getQueueLength().intValue(), QUEUE_LENGTH);
    }

    @Test
    public void test_EiffelMessageBusConfiguration_getDomainId() {
        EiffelMessageBusConfiguration mbConfig = new EiffelMessageBusConfiguration(arms, messageBus, DOMAIN_ID,
                QUEUE_LENGTH);
        assertEquals(mbConfig.getDomainId(), DOMAIN_ID);
    }

    @Test
    public void test_EiffelMessageBusConfiguration_getPersistentQueuePath() {
        EiffelMessageBusConfiguration mbConfig = new EiffelMessageBusConfiguration(arms, messageBus, DOMAIN_ID,
                QUEUE_LENGTH);
        assertEquals(mbConfig.getPersistentQueuePath(), null);
    }
}