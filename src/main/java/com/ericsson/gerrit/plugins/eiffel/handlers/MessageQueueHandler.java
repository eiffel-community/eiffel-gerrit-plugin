/*
   Copyright 2019 Ericsson AB.
   For a full list of individual contributors, please see the commit history.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.ericsson.gerrit.plugins.eiffel.handlers;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;

import java.util.concurrent.ScheduledThreadPoolExecutor;

public class MessageQueueHandler implements LifecycleListener {
    private static final String THREADS = "senderThreadPoolSize";
    private static final int DEFAULT_POOLSIZE = 1;

    private final WorkQueue workQueue;
    private final int poolSize;
    private ScheduledThreadPoolExecutor pool;

    @Inject
    public MessageQueueHandler(final WorkQueue workQueue, final PluginConfigFactory config, @PluginName final String pluginName) {
        final PluginConfig pluginConfig = config.getFromGerritConfig(pluginName, true);
        this.poolSize = pluginConfig.getInt(THREADS, DEFAULT_POOLSIZE);
        this.workQueue = workQueue;
    }

    @Override
    public void start() {
        pool = workQueue.createQueue(poolSize, "Eiffel Message Sender");
    }

    @Override
    public void stop() {
        if (pool != null) {
            pool = null;
        }
    }

    public ScheduledThreadPoolExecutor getPool() {
        return this.pool;
    }
}
