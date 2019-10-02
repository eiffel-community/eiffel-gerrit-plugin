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
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeSubmittedEvent;
import com.ericsson.gerrit.plugins.eiffel.events.generators.EiffelSourceChangeSubmittedEventGenerator;
import com.ericsson.gerrit.plugins.eiffel.sender.EiffelEventSender;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;

/**
 * Class to listen for Gerrit events. Creates specific Eiffel event for given Gerrit event. Trigger
 * sending of created Eiffel event.
 *
 */
public class ChangeMergedEventListener extends AbstractEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeMergedEventListener.class);

    @Inject
    public ChangeMergedEventListener(@PluginName final String pluginName,
            final @PluginData File pluginDirectoryPath) {
        super(pluginName, pluginDirectoryPath);
    }

    @Override
    protected boolean isExpectedGerritEvent(Event gerritEvent) {
        return gerritEvent instanceof ChangeMergedEvent;
    }

    @Override
    protected void prepareAndSendEiffelEvent(Event gerritEvent,
            EiffelPluginConfiguration pluginConfig) {
        ChangeMergedEvent changeMergedEvent = (ChangeMergedEvent) gerritEvent;
        LOGGER.info("ChangeMergedEvent recieved from Gerrit, "
                + "preparing to send a SourceChangeSubmitted eiffel event.\n{}",
                changeMergedEvent);
        EiffelSourceChangeSubmittedEvent eiffelEvent = EiffelSourceChangeSubmittedEventGenerator.generate(
                changeMergedEvent, pluginConfig);
        EiffelEventSender.send(eiffelEvent);
    }
}
