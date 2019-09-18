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

package com.ericsson.gerrit.plugins.eiffel.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;

/**
 * Class to keep plugin configuration parameters names and handle plugin
 * configuration
 *
 */
public class EiffelPluginConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EiffelPluginConfiguration.class);

    public EiffelPluginConfiguration(final String pluginName, final NameKey project,
                                     final PluginConfigFactory pluginConfigFactory) {

        PluginConfig pluginConfig;

        try {
            pluginConfig = pluginConfigFactory.getFromProjectConfig(project, pluginName);
        } catch (NoSuchProjectException e) {
            LOGGER.error("Could not initiate, error: {} \n{}", e.getMessage(), e);
            throw new ExceptionInInitializerError(String.format("Can't read %s plugin configuration for project %s: %s",
                    pluginName, project.toString(), e.getMessage()));
        }

        LOGGER.info("Loaded plugin configuration: {}", pluginConfig.toString());
    }
}
