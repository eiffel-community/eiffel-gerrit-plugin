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

package com.ericsson.gerrit.plugins.eiffel.listeners;

import java.io.File;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.handlers.MessageQueueHandler;
import com.google.gerrit.common.EventListener;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;

/**
 * Class to listen for Gerrit events to forward to an EventHandler.
 *
 */
public class GerritEventListener implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GerritEventListener.class);
    private final String pluginName;
    private final ScheduledThreadPoolExecutor pool;
    private final File pluginDir;

    @Inject
    public GerritEventListener(@PluginName final String pluginName, final MessageQueueHandler queue, final @PluginData File pluginDir) {
        this.pluginName = pluginName;
        this.pool = queue.getPool();
        this.pluginDir = pluginDir;
    }

    @Override
    public void onEvent(final Event event) {
        if (!(event instanceof ChangeMergedEvent)) {
            // Do not proceed if we got something else than ChangeMergedEvent
            return;
        }

        LOGGER.info("pluginDir: {}", pluginDir);
        LOGGER.info("pool: {}", pool);
        LOGGER.info("pluginName: {}", pluginName);

    }
}
