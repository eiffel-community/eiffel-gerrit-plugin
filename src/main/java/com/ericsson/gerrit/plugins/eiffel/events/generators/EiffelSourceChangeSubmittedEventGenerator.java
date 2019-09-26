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
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeSubmittedEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;

public class EiffelSourceChangeSubmittedEventGenerator extends EiffelEventGenerator {
    private static final String TYPE = "EiffelSourceChangeSubmittedEvent";
    private static final String VERSION = "3.0.0";

    /**
     * Extracts information from the ChangeMergedEvent and generates an
     * EiffelSourceChangeSubmittedEvent
     *
     * @param changeMergedEvent
     * @param pluginConfig
     * @return EiffelSourceChangeSubmittedEvent
     */
    public static EiffelSourceChangeSubmittedEvent generate(ChangeMergedEvent changeMergedEvent,
            EiffelPluginConfiguration pluginConfig) {
        final String projectName = changeMergedEvent.change.get().project;
        final String commitId = changeMergedEvent.newRev;
        final String branch = changeMergedEvent.change.get().branch;
        final String username = changeMergedEvent.patchSet.get().author.username;
        final String email = changeMergedEvent.patchSet.get().author.email;
        final String url = changeMergedEvent.change.get().url;

        EiffelSourceChangeSubmittedEvent eiffelEvent = new EiffelSourceChangeSubmittedEvent();
        eiffelEvent.msgParams.meta.type = TYPE;
        eiffelEvent.msgParams.meta.version = VERSION;
        eiffelEvent.msgParams.meta.source.name = META_SOURCE_NAME;
        eiffelEvent.msgParams.meta.source.host = determineHostName();
        eiffelEvent.msgParams.meta.source.uri = url;

        eiffelEvent.eventParams.data.submitter.name = username;
        eiffelEvent.eventParams.data.submitter.email = email;

        eiffelEvent.eventParams.data.gitIdentifier.commitId = commitId;
        eiffelEvent.eventParams.data.gitIdentifier.repoUri = createRepoURI(url, projectName);
        eiffelEvent.eventParams.data.gitIdentifier.branch = branch;
        eiffelEvent.eventParams.data.gitIdentifier.repoName = projectName;

        // TODO
        // String latestEiffelSourceChangeSubmittedEventId =
        // getLatestEiffelSourceChangeSubmittedEventId();
        // setPreviousVersionLink(latestEiffelSourceChangeSubmittedEventId);

        // String coheringEiffelSourceChangeCreatedEventId =
        // getCoheringEiffelSourceChangeSubmittedEventId(commitId?);
        // setChangeLink(coheringEiffelSourceChangeCreatedEventId);

        return eiffelEvent;
    }
}
