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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.configuration.RetryConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelEvent;
import com.ericsson.gerrit.plugins.eiffel.messaging.EiffelEventSender;
import com.google.gerrit.common.EventListener;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;

import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.vavr.control.Try;

/**
 * Abstract class implemented by the Gerrit event listeners, enforces needed listener methods and
 * contains some helper methods to determine if Eiffel event sending is activated for the project
 * the Gerrit event was sent by.
 *
 */
public abstract class AbstractEventListener implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventListener.class);

    @Inject
    private com.google.gerrit.server.config.PluginConfigFactory pluginConfigFactory;

    @Inject
    private RetryConfiguration retryConfiguration;

    @Inject
    @CanonicalWebUrl
    private String gerritUrl;

    protected final String pluginName;
    protected final File pluginDirectoryPath;

    public AbstractEventListener(final String pluginName, final File pluginDirectoryPath) {
        this.pluginName = pluginName;
        this.pluginDirectoryPath = pluginDirectoryPath;
    }

    @Override
    public void onEvent(final Event gerritEvent) {
        if (!isExpectedGerritEvent(gerritEvent)) {
            return;
        }

        final EiffelPluginConfiguration pluginConfig = createPluginConfig(gerritEvent);
        if (!isEiffelEventSendingEnabled(gerritEvent, pluginConfig)) {
            return;
        }

        prepareAndSendEiffelEvent(gerritEvent, pluginConfig);
    }

    /**
     * Based on project name from the gerrit event, creates a project specific
     * EiffelPluginConfiguration.
     *
     * @param gerritEvent
     * @return EiffelPluginConfiguration
     */
    public EiffelPluginConfiguration createPluginConfig(final Event gerritEvent) {
        ChangeEvent changeEvent = (ChangeEvent) gerritEvent;
        final Project.NameKey projectNameKey = changeEvent.getProjectNameKey();
        final EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(pluginName,
                projectNameKey, pluginConfigFactory);
        pluginConfig.setPluginDirectoryPath(pluginDirectoryPath);
        return pluginConfig;
    }

    /**
     * Sends an Eiffel message to the REMReM service.
     * RetryConfiguration contains the retry policy for the send method.
     *
     * @param eiffelEvent
     * @param pluginConfig
     */
    public void sendEiffelEvent(EiffelEvent eiffelEvent, EiffelPluginConfiguration pluginConfig) {
        EiffelEventSender eiffelEventSender = new EiffelEventSender(pluginConfig);
        eiffelEventSender.setEiffelEventType(eiffelEvent.getClass().getSimpleName());
        eiffelEventSender.setEiffelEventMessage(eiffelEvent);

        // TODO Queue send calls as async jobs
        Retry policy = retryConfiguration.getRetryPolicy();
        Runnable decoratedRunnable = Decorators.ofRunnable(() -> eiffelEventSender.send())
                                               .withRetry(policy)
                                               .decorate();
        Try.runRunnable(decoratedRunnable);
    }

    /**
     * Returns a boolean indicating if Eiffel event sending is enabled for this project and branch.
     *
     * @param gerritEvent
     * @param pluginConfig
     * @return boolean
     */
    protected boolean isEiffelEventSendingEnabled(final Event gerritEvent,
            final EiffelPluginConfiguration pluginConfig) {

        final ChangeEvent changeEvent = (ChangeEvent) gerritEvent;
        final String project = changeEvent.change.get().project;
        if (!isPluginEnabled(pluginConfig, project)) {
            return false;
        }

        final String branch = changeEvent.change.get().branch;
        final String filter = pluginConfig.getFilter();
        boolean isFilterSet = !StringUtils.isEmpty(filter);
        boolean isBranchNameInConfiguredFilter = isFilterSet
                && isBranchNameInConfiguredFilter(branch, filter, project);

        boolean isEiffelEventSendingEnabled = !isFilterSet || isBranchNameInConfiguredFilter;
        return isEiffelEventSendingEnabled;
    }

    protected abstract boolean isExpectedGerritEvent(Event gerritEvent);

    protected abstract void prepareAndSendEiffelEvent(Event gerritEvent,
            EiffelPluginConfiguration pluginConfig);

    private boolean isPluginEnabled(final EiffelPluginConfiguration pluginConfig,
            final String project) {
        if (!pluginConfig.isEnabled()) {
            LOGGER.debug("Eiffel plugin is disabled for project '{}'.\n"
                    + "Please refer to Eiffel plugin documentation to find out how to configure and enable plugin\n"
                    + "{}plugins/{}/Documentation/index.html", project, gerritUrl, pluginName);
            return false;
        }

        return true;
    }

    private boolean isBranchNameInConfiguredFilter(final String branch, final String filter,
            final String project) {
        final String[] filterList = filter.split("\\s+");
        for (final String regExString : filterList) {
            if (branch.matches(regExString)) {
                return true;
            }
        }

        LOGGER.debug(
                "Branch '{}' does not match any configured filter for project '{}'.\nFilter: {}",
                branch, project, filter);
        return false;
    }
}
