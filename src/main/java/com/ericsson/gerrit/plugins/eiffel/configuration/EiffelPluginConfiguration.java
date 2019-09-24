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
    
 // Plugin configuration parameters names
    public static final String ENABLED = "enabled";
    public static final String FILTER = "filter";
    public static final String REMREM_GENERATE_URL = "remrem-generate-url";
    public static final String REMREM_PUBLISH_URL = "remrem-publish-url";
    public static final String REMREM_USERNAME = "remrem-username";
    public static final String REMREM_PASSWORD = "remrem-password";
    public static final String FLOW_CONTEXT = "flow-context";
    
    // Fields to keep actual configuration
    private final String cfgRemremGenerateURL;
    private final String cfgRemremPublishURL;
    private final String cfgRemremUsername;
    private final String cfgRemremPassword;
    private final String cfgFilter;
    private final boolean cfgEnabled;
    private final String cfgFlowContext;

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
        
        // Read plugin configuration
        this.cfgEnabled = pluginConfig.getBoolean(ENABLED, false);
        this.cfgFilter = pluginConfig.getString(FILTER);
        this.cfgRemremGenerateURL = pluginConfig.getString(REMREM_GENERATE_URL);
        this.cfgRemremPublishURL = pluginConfig.getString(REMREM_PUBLISH_URL);
        this.cfgRemremUsername = pluginConfig.getString(REMREM_USERNAME);
        this.cfgRemremPassword = pluginConfig.getString(REMREM_PASSWORD);
        
        //flow context is optional
        this.cfgFlowContext = pluginConfig.getString(FLOW_CONTEXT);
        
        // No point to check other config parameters if plugin is disabled
        if (!this.cfgEnabled) {
            return;
        }
        
        // Make sure that REMReM configuration is defined, otherwise we won't be able to send messages.
        if (this.cfgRemremGenerateURL == null) {
            throw new ExceptionInInitializerError(
                    String.format("Can't read %s plugin configuration for project %s: REMReM Generate URL is null", pluginName,
                            project.toString()));
        } else if (this.cfgRemremPublishURL == null) {
            throw new ExceptionInInitializerError(
                    String.format("Can't read %s plugin configuration for project %s: REMReM Publish URL is null", pluginName,
                            project.toString()));
        } else if (this.cfgRemremUsername == null) {
            throw new ExceptionInInitializerError(
                    String.format("Can't read %s plugin configuration for project %s: REMReM Username is null", pluginName,
                            project.toString()));
        } else if (this.cfgRemremPassword == null) {
            throw new ExceptionInInitializerError(
                    String.format("Can't read %s plugin configuration for project %s: REMReM Password is null", pluginName,
                            project.toString()));
        }

        LOGGER.info("Loaded plugin configuration: {}", pluginConfig.toString());
    }

    public String getCfgRemremGenerateURL() {
        return cfgRemremGenerateURL;
    }

    public String getCfgRemremPublishURL() {
        return cfgRemremPublishURL;
    }

    public String getCfgRemremUsername() {
        return cfgRemremUsername;
    }

    public String getCfgRemremPassword() {
        return cfgRemremPassword;
    }

    public String getCfgFilter() {
        return cfgFilter;
    }

    public boolean isCfgEnabled() {
        return cfgEnabled;
    }

    public String getCfgFlowContext() {
        return cfgFlowContext;
    }
}
