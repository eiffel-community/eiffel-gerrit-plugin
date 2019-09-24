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
    public static final String REMREM_PUBLISH_URL = "remrem-publish-url";
    public static final String REMREM_USERNAME = "remrem-username";
    public static final String REMREM_PASSWORD = "remrem-password";
    public static final String FLOW_CONTEXT = "flow-context";
    
    // Fields to keep actual configuration
    private final String remremPublishURL;
    private final String remremUsername;
    private final String remremPassword;
    private final String filter;
    private final boolean enabled;
    private final String flowContext;

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
        this.enabled = pluginConfig.getBoolean(ENABLED, false);
        this.filter = pluginConfig.getString(FILTER);
        this.remremPublishURL = pluginConfig.getString(REMREM_PUBLISH_URL);
        this.remremUsername = pluginConfig.getString(REMREM_USERNAME);
        this.remremPassword = pluginConfig.getString(REMREM_PASSWORD);
        //flow context is optional
        this.flowContext = pluginConfig.getString(FLOW_CONTEXT);

        // No point to check other config parameters if plugin is disabled
        boolean isValidated = validatePluginConfig(pluginName, project);
        if (!isValidated) {
            return;
        }
        LOGGER.info("Loaded plugin configuration: {}", pluginConfig.toString());
    }

    /**
     * This method validates the Eiffel Configurations.
     * @param pluginName
     * @param project
     */
    private boolean validatePluginConfig(final String pluginName, final NameKey project) {
        if (!this.enabled) {
            return false;
        }
        
        // Make sure that REMReM configuration is defined, otherwise we won't be able to send messages.
        if (this.remremPublishURL == null) {
            throw new ExceptionInInitializerError(
                    String.format("Can't read %s plugin configuration for project %s: REMReM Generate URL is null", pluginName,
                            project.toString()));
        } else if (this.remremUsername == null) {
            throw new ExceptionInInitializerError(
                    String.format("Can't read %s plugin configuration for project %s: REMReM Username is null", pluginName,
                            project.toString()));
        } else if (this.remremPassword == null) {
            throw new ExceptionInInitializerError(
                    String.format("Can't read %s plugin configuration for project %s: REMReM Password is null", pluginName,
                            project.toString()));
        }
        return true;
    }

    public String getRemremPublishURL() {
        return remremPublishURL;
    }

    public String getRemremUsername() {
        return remremUsername;
    }

    public String getRemremPassword() {
        return remremPassword;
    }

    public String getFilter() {
        return filter;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getFlowContext() {
        return flowContext;
    }
}
