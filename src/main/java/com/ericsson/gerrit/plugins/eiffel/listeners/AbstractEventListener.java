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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.google.gerrit.common.EventListener;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;

public abstract class AbstractEventListener implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeMergedEventListener.class);

    @Inject
    private com.google.gerrit.server.config.PluginConfigFactory pluginConfigFactory;

    @Inject
    @CanonicalWebUrl
    private String gerritUrl;

    protected final String pluginName;
    protected final File plugindir;

    public AbstractEventListener(final String pluginName, final File pluginDir) {
        this.pluginName = pluginName;
        this.plugindir = pluginDir;
    }

    public boolean isEventSendingEnabled(final Event gerritEvent,
            final EiffelPluginConfiguration pluginConfig) {

        if (!isPluginEnabled(pluginConfig)) {
            return false;
        }

        ChangeEvent changeEvent = (ChangeEvent) gerritEvent;
        final String project = changeEvent.change.get().project;
        final String branch = changeEvent.change.get().branch;
        final String filter = pluginConfig.getFilter();
        if (isFilteredOut(branch, filter, project)) {
            return false;
        }

        return true;
    }

    protected abstract boolean isExpectedGerritEvent(Event gerritEvent);

    protected abstract void prepareAndSendEiffelEvent(Event gerritEvent,
            EiffelPluginConfiguration pluginConfig);

    protected EiffelPluginConfiguration createPluginConfig(Event gerritEvent) {
        ChangeEvent changeEvent = (ChangeEvent) gerritEvent;
        final Project.NameKey projectNameKey = changeEvent.getProjectNameKey();
        final EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(pluginName,
                projectNameKey, pluginConfigFactory);
        return pluginConfig;
    }

    private boolean isPluginEnabled(EiffelPluginConfiguration pluginConfig) {
        if (!pluginConfig.isEnabled()) {
            LOGGER.debug("Eiffel plugin is disabled for this project.\n"
                    + "Please refer to Eiffel plugin documentation to find out how to configure and enable plugin\n"
                    + "{}plugins/{}/Documentation/index.html", gerritUrl, pluginName);
            return false;
        }

        return true;
    }

    private boolean isFilteredOut(String branch, String filter, String project) {
        boolean branchIsFilteredOut = true;
        if (filter == null || filter.isEmpty()) {
            branchIsFilteredOut = false;
        }

        for (final String regExString : filter.split("\\s+")) {
            if (branch.matches(regExString)) {
                branchIsFilteredOut = false;
            }
        }

        LOGGER.debug("Branch {} for project {} doesn't much any of filters '{}', skip sending",
                branch, project, filter);
        return branchIsFilteredOut;
    }
}
