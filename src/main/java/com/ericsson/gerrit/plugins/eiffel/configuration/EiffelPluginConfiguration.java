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

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;

/**
 * Class to keep plugin configuration parameters names and handle plugin configuration
 *
 */
public class EiffelPluginConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EiffelPluginConfiguration.class);

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
    private File pluginDirectoryPath;

    public EiffelPluginConfiguration(final String pluginName, final NameKey project,
            final PluginConfigFactory pluginConfigFactory) {

        PluginConfig pluginConfig;

        try {
            pluginConfig = pluginConfigFactory.getFromProjectConfig(project, pluginName);
        } catch (NoSuchProjectException e) {
            LOGGER.error("Could not initiate, error: {} \n{}", e.getMessage(), e);
            throw new ExceptionInInitializerError(
                    String.format("Can't read %s plugin configuration for project %s: %s",
                            pluginName, project.toString(), e.getMessage()));
        }
        // Read plugin configuration
        this.enabled = pluginConfig.getBoolean(ENABLED, false);
        this.filter = getMultiValueParameters(FILTER, pluginConfig);
        this.remremPublishURL = pluginConfig.getString(REMREM_PUBLISH_URL);
        this.remremUsername = pluginConfig.getString(REMREM_USERNAME);
        this.remremPassword = pluginConfig.getString(REMREM_PASSWORD);
        // flow context is optional
        this.flowContext = getMultiValueParameters(FLOW_CONTEXT, pluginConfig);

        // No point to check other config parameters if plugin is disabled
        if (!this.enabled) {
            return;
        }

        // Make sure that REMReM configuration is defined, otherwise we won't be able to send
        // messages.
        // Present we are not making the username and password mandatory, as REMReM has the
        // capability to not use them.
        if (this.remremPublishURL == null) {
            throw new ExceptionInInitializerError(
                    String.format(
                            "Can't read %s plugin configuration for project %s: REMReM Generate URL is null",
                            pluginName,
                            project.toString()));
        }
        LOGGER.info("Loaded plugin configuration: {}", pluginConfig.toString());
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

    /**
     * The location in the system where the plugin may store plugin specific files.
     *
     * @return File
     */
    public File getPluginDirectoryPath() {
        return this.pluginDirectoryPath;
    }

    /**
     * Set the location in the system where the plugin may store plugin specific files.
     *
     * @param pluginDirectoryPath
     */
    public void setPluginDirectoryPath(File pluginDirectoryPath) {
        this.pluginDirectoryPath = pluginDirectoryPath;
    }

    /**
     * This method reads multiple values set manually by editing the project configuration
     * and then setting it in the GUI so that the person who is having the privileges to the UI is aware of the 
     * parameters set manually.
     * Ex:
     * [eiffel-integration]
     *    filter = branch1, branch2
     *    filter = branch3
     * @param property  the property which can contain multiple values, for example: Filter, FlowContext, etc.
     * @param pluginConfig the configuration for the project read
     * @return String  returns the value for the property read from the plugin configuration.
     */
    private String getMultiValueParameters(final String property, final PluginConfig pluginConfig){
        String propertyValue = "";
        String[] propertyValues = pluginConfig.getStringList(property);
        if (propertyValues!=null && propertyValues.length > 0) {
            propertyValue = Arrays.stream(propertyValues).collect(Collectors.joining(","));
            pluginConfig.setString(property, Arrays.stream(propertyValues).collect(Collectors.joining(",")));
        }
        return propertyValue;
    }
}
