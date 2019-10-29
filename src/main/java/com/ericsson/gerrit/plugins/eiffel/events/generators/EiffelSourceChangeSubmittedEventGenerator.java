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
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeSubmittedEvent;
import com.ericsson.gerrit.plugins.eiffel.events.models.Link;
import com.ericsson.gerrit.plugins.eiffel.handlers.StateHandler;
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
     * @param pluginConfig
     * @return EiffelSourceChangeSubmittedEvent
     */
    public static EiffelSourceChangeSubmittedEvent generate(ChangeMergedEvent changeMergedEvent,
            EiffelPluginConfiguration pluginConfigPatchSetCreatedEvent, File pluginDirectoryPath) {
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
        final Link changeLink = createChangeLink(pluginDirectoryPath, projectName, changeId);
        final Link previousVersionLink = createPreviousVersionLink(pluginDirectoryPath, projectName, branch);

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

        if (changeLink != null) {
            eiffelEvent.eventParams.links.add(changeLink);
        }

        if (previousVersionLink != null) {
            eiffelEvent.eventParams.links.add(previousVersionLink);
        }

        return eiffelEvent;
    }

    private static Link createPreviousVersionLink(File pluginDirectoryPath, final String projectName,
            final String branch) {
        StateHandler stateHandler = new StateHandler(pluginDirectoryPath);
        String lastSourceChangeSubmitted = getLastSourceChangeSubmittedEiffelEventId(projectName, branch,
                stateHandler);

        if (!StringUtils.isEmpty(lastSourceChangeSubmitted)) {
            Link previousVersionLink = new Link();
            previousVersionLink.type = LINK_TYPE_PREVIOUS_VERSION;
            previousVersionLink.target = lastSourceChangeSubmitted;

            return previousVersionLink;
        }
        return null;
    }

    private static Link createChangeLink(File pluginDirectoryPath, final String projectName, final String changeId) {
        StateHandler stateHandler = new StateHandler(pluginDirectoryPath);
        String lastSourceChangeCreated = getLastSourceChangeCreatedEiffelEvent(projectName, changeId, stateHandler);

        if (!StringUtils.isEmpty(lastSourceChangeCreated)) {
            Link previousVersionLink = new Link();
            previousVersionLink.type = LINK_TYPE_CHANGE;
            previousVersionLink.target = lastSourceChangeCreated;

            return previousVersionLink;
        }
        return null;
    }
}
