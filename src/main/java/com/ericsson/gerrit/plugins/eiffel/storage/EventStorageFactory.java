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

package com.ericsson.gerrit.plugins.eiffel.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EventType;

public class EventStorageFactory {
    protected static final Logger LOGGER = LoggerFactory.getLogger(EventStorageFactory.class);

    public static EventStorage getEventStorage(final EiffelPluginConfiguration pluginConfig, final String eventType) {
        switch (eventType) {
            case EventType.SCC_EVENT:
                return new SourceChangeCreatedStorage(pluginConfig);
            case EventType.SCS_EVENT:
                return new SourceChangeSubmittedStorage(pluginConfig);
            default:
                throw new IllegalArgumentException("The event type does not exist " + eventType + ".");
        }
    }

}
