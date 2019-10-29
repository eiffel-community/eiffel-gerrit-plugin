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

import org.parboiled.common.StringUtils;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeCreatedEvent;
import com.ericsson.gerrit.plugins.eiffel.events.models.Link;
import com.ericsson.gerrit.plugins.eiffel.handlers.StateHandler;
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
     * @param pluginConfig
     * @return EiffelSourceChangeCreatedEvent
     */
    public static EiffelSourceChangeCreatedEvent generate(PatchSetCreatedEvent patchSetCreatedEvent,
            EiffelPluginConfiguration pluginConfig, File pluginDirectoryPath) {
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
        final Link previousVersionLink = createPreviousVersionLink(pluginDirectoryPath, projectName,
                changeId);
        final Link baseLink = createBaseLink(pluginDirectoryPath, projectName, branch);

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

        if (previousVersionLink != null) {
            eiffelEvent.eventParams.links.add(previousVersionLink);
        }

        if (baseLink != null) {
            eiffelEvent.eventParams.links.add(baseLink);
        }

        // String latestEiffelSourceChangeSubmittedEventId =
        // getLatestEiffelSourceChangeSubmittedEventId();
        // setBaseLink(latestEiffelSourceChangeSubmittedEventId);

        return eiffelEvent;
    }

    private static Link createBaseLink(File pluginDirectoryPath, String projectName, String branch) {
        StateHandler stateHandler = new StateHandler(pluginDirectoryPath);
        String lastSourceChangeSubmitted = getLastSourceChangeSubmittedEiffelEventId(projectName, branch,
                stateHandler);

        if (!StringUtils.isEmpty(lastSourceChangeSubmitted)) {
            Link baseLink = new Link();
            baseLink.type = LINK_TYPE_BASE;
            baseLink.target = lastSourceChangeSubmitted;

            return baseLink;
        }
        return null;
    }

    private static Link createPreviousVersionLink(File pluginDirectoryPath, final String projectName,
            final String changeId) {
        StateHandler stateHandler = new StateHandler(pluginDirectoryPath);
        String lastSourceChangeCreated = getLastSourceChangeCreatedEiffelEvent(projectName, changeId, stateHandler);

        if (!StringUtils.isEmpty(lastSourceChangeCreated)) {
            Link previousVersionLink = new Link();
            previousVersionLink.type = LINK_TYPE_PREVIOUS_VERSION;
            previousVersionLink.target = lastSourceChangeCreated;

            return previousVersionLink;
        }
        return null;
    }
}
