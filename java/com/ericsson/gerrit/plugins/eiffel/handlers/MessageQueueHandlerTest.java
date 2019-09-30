package com.ericsson.gerrit.plugins.eiffel.handlers;

import static org.assertj.core.api.Assertions.*;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.gerrit.plugins.eiffel.handlers.MessageQueueHandler;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gerrit.server.git.WorkQueue.Executor;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.ericsson.gerrit.plugins.eiffel.*")
public class MessageQueueHandlerTest {

    private final String PLUGIN_NAME = "plugin";
    private final String THREADS = "senderThreadPoolSize";
    private final int DEFAULT_POOLSIZE = 1;

    private WorkQueue workQueue;
    private PluginConfigFactory pluginConfigFactory;
    private PluginConfig pluginConfig;
    private Executor pool;

    @Before
    public void init() {
        workQueue = mock(WorkQueue.class);
        pluginConfigFactory = mock(PluginConfigFactory.class);
        pluginConfig = mock(PluginConfig.class);
        pool = mock(Executor.class);
    }

    @Test
    public void test_MessageQueue_Constructor() {
        Mockito.doReturn(pluginConfig).when(pluginConfigFactory).getFromGerritConfig(PLUGIN_NAME, true);
        Mockito.doReturn(DEFAULT_POOLSIZE).when(pluginConfig).getInt(THREADS, DEFAULT_POOLSIZE);

        assertThatCode(() -> new MessageQueueHandler(workQueue, pluginConfigFactory, PLUGIN_NAME)).doesNotThrowAnyException();
    }

    @Test
    public void test_MessageQueue_start() {
        Mockito.doReturn(pluginConfig).when(pluginConfigFactory).getFromGerritConfig(PLUGIN_NAME, true);
        Mockito.doReturn(DEFAULT_POOLSIZE).when(pluginConfig).getInt(THREADS, DEFAULT_POOLSIZE);
        Mockito.doReturn(pool).when(workQueue).createQueue(DEFAULT_POOLSIZE, "Eiffel Message Sender");

        MessageQueueHandler queue = new MessageQueueHandler(workQueue, pluginConfigFactory, PLUGIN_NAME);

        assertThatCode(() -> queue.start()).doesNotThrowAnyException();
    }

    @Test
    public void test_MessageQueue_stop_Null() {
        Mockito.doReturn(pluginConfig).when(pluginConfigFactory).getFromGerritConfig(PLUGIN_NAME, true);
        Mockito.doReturn(DEFAULT_POOLSIZE).when(pluginConfig).getInt(THREADS, DEFAULT_POOLSIZE);
        Mockito.doReturn(pool).when(workQueue).createQueue(DEFAULT_POOLSIZE, "Eiffel Message Sender");

        MessageQueueHandler queue = new MessageQueueHandler(workQueue, pluginConfigFactory, PLUGIN_NAME);

        assertThatCode(() -> queue.stop()).doesNotThrowAnyException();
    }

    @Test
    public void test_MessageQueue_stop_NotNull() {
        Mockito.doReturn(pluginConfig).when(pluginConfigFactory).getFromGerritConfig(PLUGIN_NAME, true);
        Mockito.doReturn(DEFAULT_POOLSIZE).when(pluginConfig).getInt(THREADS, DEFAULT_POOLSIZE);
        Mockito.doReturn(pool).when(workQueue).createQueue(DEFAULT_POOLSIZE, "Eiffel Message Sender");

        MessageQueueHandler queue = new MessageQueueHandler(workQueue, pluginConfigFactory, PLUGIN_NAME);
        queue.start();

        assertThatCode(() -> queue.stop()).doesNotThrowAnyException();
    }

    @Test
    public void test_MessageQueue_getPool() {
        Mockito.doReturn(pluginConfig).when(pluginConfigFactory).getFromGerritConfig(PLUGIN_NAME, true);
        Mockito.doReturn(DEFAULT_POOLSIZE).when(pluginConfig).getInt(THREADS, DEFAULT_POOLSIZE);
        Mockito.doReturn(pool).when(workQueue).createQueue(DEFAULT_POOLSIZE, "Eiffel Message Sender");

        MessageQueueHandler queue = new MessageQueueHandler(workQueue, pluginConfigFactory, PLUGIN_NAME);
        queue.start();
        ScheduledThreadPoolExecutor pool = queue.getPool();

        assertThat(pool instanceof Executor).isTrue();
    }
}