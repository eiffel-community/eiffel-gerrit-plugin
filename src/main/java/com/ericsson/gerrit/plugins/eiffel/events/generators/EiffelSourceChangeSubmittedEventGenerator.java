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

import java.io.File;
import java.util.List;

import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeSubmittedEvent;
import com.ericsson.gerrit.plugins.eiffel.events.EventType;
import com.ericsson.gerrit.plugins.eiffel.events.models.Link;
import com.ericsson.gerrit.plugins.eiffel.git.CommitInformation;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.ChangeMergedEvent;

public final class EiffelSourceChangeSubmittedEventGenerator extends EiffelEventGenerator {

    private static final String TYPE = "EiffelSourceChangeSubmittedEvent";
    private static final String LINK_TYPE_PREVIOUS_VERSION = "PREVIOUS_VERSION";
    private static final String LINK_TYPE_CHANGE = "CHANGE";

    /**
     * Extracts information from the ChangeMergedEvent and generates an
     * EiffelSourceChangeSubmittedEvent
     *
     * @param changeMergedEvent
     * @param commitInformation
     * @param pluginConfig
     * @return EiffelSourceChangeSubmittedEvent
     */
    public static EiffelSourceChangeSubmittedEvent generate(ChangeMergedEvent changeMergedEvent,
            File pluginDirectoryPath, CommitInformation commitInformation) {
        final ChangeAttribute changeAttribute = changeMergedEvent.change.get();
        final PatchSetAttribute patchSetAttribute = changeMergedEvent.patchSet.get();
        final String commitId = changeMergedEvent.newRev;
        final String projectName = changeAttribute.project;
        final String branch = changeAttribute.branch;
        final String url = changeAttribute.url;
        final String name = patchSetAttribute.author.name;
        final String username = patchSetAttribute.author.username;
        final String email = patchSetAttribute.author.email;
        final String changeId = changeMergedEvent.changeKey.toString();

        EiffelSourceChangeSubmittedEvent eiffelEvent = new EiffelSourceChangeSubmittedEvent();
        eiffelEvent.msgParams.meta.type = TYPE;
        eiffelEvent.msgParams.meta.source.name = META_SOURCE_NAME;
        eiffelEvent.msgParams.meta.source.host = determineHostName();
        eiffelEvent.msgParams.meta.source.uri = url;

        eiffelEvent.eventParams.data.submitter.name = name;
        eiffelEvent.eventParams.data.submitter.id = username;
        eiffelEvent.eventParams.data.submitter.email = email;

        eiffelEvent.eventParams.data.gitIdentifier.commitId = commitId;
        eiffelEvent.eventParams.data.gitIdentifier.repoUri = createRepoURI(url, projectName);
        eiffelEvent.eventParams.data.gitIdentifier.branch = branch;
        eiffelEvent.eventParams.data.gitIdentifier.repoName = projectName;

        String previousSourceChangeCreatedEvent = getPreviousEiffelEventId(EventType.SCC_EVENT,
                projectName, changeId, pluginDirectoryPath);
        final Link changeLink = createLink(LINK_TYPE_CHANGE, previousSourceChangeCreatedEvent);
        if (changeLink != null) {
            eiffelEvent.eventParams.links.add(changeLink);
        }

        List<String> parentsSHAs = commitInformation.getParentsSHAs(commitId, projectName);
        String previousSourceChangeSubmittedEventId = getPreviousEiffelEventId(EventType.SCS_EVENT,
                projectName, parentsSHAs, pluginDirectoryPath);
        final Link previousVersionLink = createLink(LINK_TYPE_PREVIOUS_VERSION,
                previousSourceChangeSubmittedEventId);
        if (previousVersionLink != null) {
            eiffelEvent.eventParams.links.add(previousVersionLink);
        }

        return eiffelEvent;
    }
}
