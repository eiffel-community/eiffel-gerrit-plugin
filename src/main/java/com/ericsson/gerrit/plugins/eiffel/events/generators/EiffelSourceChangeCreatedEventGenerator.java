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
package com.ericsson.gerrit.plugins.eiffel.events.generators;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeCreatedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;

public class EiffelSourceChangeCreatedEventGenerator extends EiffelEventGenerator {
    private static final String TYPE = "EiffelSourceChangeCreatedEvent";
    private static final String VERSION = "4.0.0";

    /**
     * Extracts information from the PatchSetCreatedEvent and generates an
     * EiffelSourceChangeCreatedEvent
     *
     * @param patchSetCreatedEvent
     * @param pluginConfig
     * @return EiffelSourceChangeCreatedEvent
     */
    public static EiffelSourceChangeCreatedEvent generate(PatchSetCreatedEvent patchSetCreatedEvent,
            EiffelPluginConfiguration pluginConfig) {
        final String projectName = patchSetCreatedEvent.change.get().project;
        final String branch = patchSetCreatedEvent.change.get().branch;
        final String url = patchSetCreatedEvent.change.get().url;
        final String commitId = patchSetCreatedEvent.patchSet.get().revision;
        final String username = patchSetCreatedEvent.patchSet.get().author.username;
        final String email = patchSetCreatedEvent.patchSet.get().author.email;

        EiffelSourceChangeCreatedEvent eiffelEvent = new EiffelSourceChangeCreatedEvent();
        eiffelEvent.msgParams.meta.type = TYPE;
        eiffelEvent.msgParams.meta.version = VERSION;
        eiffelEvent.msgParams.meta.source.name = META_SOURCE_NAME;
        eiffelEvent.msgParams.meta.source.host = determineHostName();
        eiffelEvent.msgParams.meta.source.uri = url;

        eiffelEvent.eventParams.data.author.name = username;
        eiffelEvent.eventParams.data.author.email = email;

        eiffelEvent.eventParams.data.gitIdentifier.commitId = commitId;
        eiffelEvent.eventParams.data.gitIdentifier.repoUri = createRepoURI(url, projectName);
        eiffelEvent.eventParams.data.gitIdentifier.branch = branch;
        eiffelEvent.eventParams.data.gitIdentifier.repoName = projectName;

        // TODO
        // String latestEiffelSourceChangeCreatedEventId =
        // getLatestEiffelSourceChangeCreatedEventId();
        // setPreviousVersionLink(latestEiffelSourceChangeCreatedEventId);

        return eiffelEvent;
    }
}
