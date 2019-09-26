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

public class EiffelEventGenerator {
    protected static final String META_SOURCE_NAME = "Eiffel Gerrit Plugin";

    private static final int DEFAULT_SSH_PORT = 29418;
    private static final String DEFAULT_GERRIT_BASE_URL = "ssh://gerritmirror:" + DEFAULT_SSH_PORT;

    protected static String determineHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            return "unknown";
        }
    }

    protected static String createRepoURI(String url, String projectName) {
        try {
            URI changeUri = new URI(url);
            String sshBaseUrl = getSshBaseUrl(changeUri.getHost());
            return sshBaseUrl;
        } catch (URISyntaxException e) {
            return "unknown";
        }
    }

    private static String getSshBaseUrl(String host) {
        URI uri;
        try {
            uri = new URI("ssh", null, host, DEFAULT_SSH_PORT, "/", null, null);
            return uri.toString();
        } catch (URISyntaxException e) {
            return DEFAULT_GERRIT_BASE_URL;
        }
    }
}
