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

import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeCreatedEvent;
import com.ericsson.gerrit.plugins.eiffel.events.EventType;
import com.ericsson.gerrit.plugins.eiffel.events.models.Link;
import com.ericsson.gerrit.plugins.eiffel.git.CommitInformation;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.PatchSetCreatedEvent;

public final class EiffelSourceChangeCreatedEventGenerator extends EiffelEventGenerator {

    private static final String TYPE = "EiffelSourceChangeCreatedEvent";
    private static final String TRACKER = "Gerrit";
    private static final String LINK_TYPE_PREVIOUS_VERSION = "PREVIOUS_VERSION";
    private static final String LINK_TYPE_BASE = "BASE";

    /**
     * Extracts information from the PatchSetCreatedEvent and generates an
     * EiffelSourceChangeCreatedEvent
     *
     * @param patchSetCreatedEvent
     * @param pluginDirectoryPath
     * @param commitInformation
     * @return EiffelSourceChangeCreatedEvent
     */
    public static EiffelSourceChangeCreatedEvent generate(PatchSetCreatedEvent patchSetCreatedEvent,
            File pluginDirectoryPath, CommitInformation commitInformation) {
        final ChangeAttribute changeAttribute = patchSetCreatedEvent.change.get();
        final PatchSetAttribute patchSetAttribute = patchSetCreatedEvent.patchSet.get();
        final String projectName = changeAttribute.project;
        final String branch = changeAttribute.branch;
        final String url = changeAttribute.url;
        final String commitId = patchSetAttribute.revision;
        final String name = patchSetAttribute.author.name;
        final String username = patchSetAttribute.author.username;
        final String email = patchSetAttribute.author.email;
        final int insertions = patchSetAttribute.sizeInsertions;
        final int deletions = patchSetAttribute.sizeDeletions;
        final String changeId = patchSetCreatedEvent.changeKey.toString();

        EiffelSourceChangeCreatedEvent eiffelEvent = new EiffelSourceChangeCreatedEvent();
        eiffelEvent.msgParams.meta.type = TYPE;
        eiffelEvent.msgParams.meta.source.name = META_SOURCE_NAME;
        eiffelEvent.msgParams.meta.source.host = determineHostName();
        eiffelEvent.msgParams.meta.source.uri = url;

        eiffelEvent.eventParams.data.author.name = name;
        eiffelEvent.eventParams.data.author.id = username;
        eiffelEvent.eventParams.data.author.email = email;

        eiffelEvent.eventParams.data.change.id = changeId;
        eiffelEvent.eventParams.data.change.tracker = TRACKER;
        eiffelEvent.eventParams.data.change.details = url;
        eiffelEvent.eventParams.data.change.deletions = deletions;
        eiffelEvent.eventParams.data.change.insertions = insertions;

        eiffelEvent.eventParams.data.gitIdentifier.commitId = commitId;
        eiffelEvent.eventParams.data.gitIdentifier.repoUri = createRepoURI(url, projectName);
        eiffelEvent.eventParams.data.gitIdentifier.branch = branch;
        eiffelEvent.eventParams.data.gitIdentifier.repoName = projectName;

        String previousSourceChangeCreatedEvent = getPreviousEiffelEvent(EventType.SCC_EVENT,
                projectName, changeId, pluginDirectoryPath);
        final Link previousVersionLink = createLink(LINK_TYPE_PREVIOUS_VERSION,
                previousSourceChangeCreatedEvent);
        if (previousVersionLink != null) {
            eiffelEvent.eventParams.links.add(previousVersionLink);
        }

        List<String> parentsSHAs = commitInformation.getParentsSHAs(commitId, projectName);
        String previousSourceChangeSubmittedEvent = getPreviousEiffelEvent(EventType.SCS_EVENT,
                projectName, parentsSHAs, pluginDirectoryPath);
        final Link baseLink = createLink(LINK_TYPE_BASE, previousSourceChangeSubmittedEvent);
        if (baseLink != null) {
            eiffelEvent.eventParams.links.add(baseLink);
        }

        return eiffelEvent;
    }
}
