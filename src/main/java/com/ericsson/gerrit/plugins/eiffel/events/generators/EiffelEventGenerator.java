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

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.handlers.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.state.SourceChangeCreatedState;
import com.ericsson.gerrit.plugins.eiffel.state.SourceChangeSubmittedState;

/**
 * Base class with common functionality for event generators.
 *
 */
public class EiffelEventGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EiffelEventGenerator.class);

    protected static final String META_SOURCE_NAME = "Eiffel Gerrit Plugin";
    private static final int DEFAULT_SSH_PORT = 29418;

    protected static String determineHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            return null;
        }
    }

    protected static String createRepoURI(String url, String projectName) {
        try {
            URI changeUri = new URI(url);
            String hostName = changeUri.getHost();
            if (hostName == null) {
                return null;
            }
            String sshBaseUrl = getSshBaseUrl(changeUri.getHost());
            return sshBaseUrl;
        } catch (Exception e) {
            return null;
        }
    }

    protected static String getLastSourceChangeSubmittedEiffelEventId(String projectName, String branch,
            SourceChangeSubmittedState stateAccessor) {
        try {
            String latestEiffelSourceChangeSubmittedEventId = stateAccessor.getEventId(projectName, branch);
            return latestEiffelSourceChangeSubmittedEventId;
        } catch (NoSuchElementException e) {
            return null;
        } catch (Exception e) {
            LOGGER.error("Could not get last submitted eiffel event id.", e);
            return null;
        }
    }

    protected static String getLastSourceChangeCreatedEiffelEvent(final String projectName, final String changeId,
            SourceChangeCreatedState stateAccessor) {
        try {
            String latestEiffelSourceChangeCreatedEventId = stateAccessor.getEventId(projectName, changeId);
            return latestEiffelSourceChangeCreatedEventId;
        } catch (NoSuchElementException e) {
            return null;
        } catch (Exception e) {
            LOGGER.error("Could not get last submitted eiffel event id.", e);
            return null;
        }
    }

    private static String getSshBaseUrl(String host) throws URISyntaxException {
        URI uri;
        uri = new URI("ssh", null, host, DEFAULT_SSH_PORT, "/", null, null);
        return uri.toString();
    }
}
