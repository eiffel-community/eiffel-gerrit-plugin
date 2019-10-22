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
package com.ericsson.gerrit.plugins.eiffel.messaging;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.eiffelcommons.utils.HttpRequest;
import com.ericsson.eiffelcommons.utils.ResponseEntity;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelEvent;
import com.ericsson.gerrit.plugins.eiffel.exceptions.HttpRequestFailedException;
import com.ericsson.gerrit.plugins.eiffel.exceptions.MissingConfigurationException;
import com.ericsson.eiffelcommons.utils.HttpRequest.HttpMethod;
import com.google.gson.Gson;

public class EiffelEventSender {
    private static final String GENERATE_PUBLISH_ENDPOINT = "/generateAndPublish/";
    private static final String MESSAGE_PROTOCOL = "mp";
    private static final String MESSAGE_TYPE = "msgType";
    private static final String EIFFEL_PROTOCOL = "eiffelsemantics";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private static final Logger LOGGER = LoggerFactory.getLogger(EiffelEventSender.class);

    private final Gson gson = new Gson();
    private String eiffelMessage;
    private String eiffelType;
    private EiffelPluginConfiguration pluginConfig;
    private HttpRequest httpRequest;

    public EiffelEventSender(EiffelPluginConfiguration pluginConfig) {
        this(pluginConfig, new HttpRequest(HttpMethod.POST));
    }

    public EiffelEventSender(EiffelPluginConfiguration pluginConfig, HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
        this.pluginConfig = pluginConfig;
    }

    /**
     * Sends a REMReM Eiffel message to the generateAndPublish endpoint. RuntimeException is thrown
     * when a HttpRequestFailedException occurs so that the retry logic works.
     *
     */
    public void send() {
        try {
            verifyConfiguration();
            generateAndPublish();
        } catch (URISyntaxException | IOException | MissingConfigurationException e1) {
            LOGGER.error("Failed to send eiffel message.", e1);
        } catch (HttpRequestFailedException e2) {
            LOGGER.error("Failed to send eiffel message.", e2);
            throw e2;
        }
    }

    public EiffelEventSender setEiffelEventMessage(final EiffelEvent eiffelMessage) {
        this.eiffelMessage = gson.toJsonTree(eiffelMessage).toString();
        return this;
    }

    public EiffelEventSender setEiffelEventType(final String eiffelType) {
        this.eiffelType = eiffelType;
        return this;
    }

    private void generateAndPublish()
            throws URISyntaxException, IOException, MissingConfigurationException,
            HttpRequestFailedException {
        assembleRequest();
        final ResponseEntity response = httpRequest.performRequest();
        verifyResponse(response);
    }

    private void verifyResponse(ResponseEntity response) throws HttpRequestFailedException {
        final int statusCode = response.getStatusCode();
        final String result = response.getBody();

        if (HttpStatus.SC_OK == statusCode) {
            LOGGER.info("Generated and published eiffel message successfully. \n{}", result);
        } else {
            final String errorMessage = String.format(
                    "Could not generate and publish eiffel message due to server issue or invalid json data, "
                            + "Status Code :: %d\npublishURL :: %s\ninput message :: %s\nError Message  :: %s",
                    statusCode, httpRequest.getBaseUrl(), eiffelMessage, result);
            throw new HttpRequestFailedException(errorMessage);
        }
    }

    private void verifyConfiguration() throws MissingConfigurationException {
        if (!isConfigurationSet()) {
            final String errorMessage = String.format(
                    "Neccessary configuration is missing to send event."
                            + "\neiffelMessage: %s\neiffelType: %s\neiffelProtocol: %s",
                    eiffelMessage, eiffelType, EIFFEL_PROTOCOL);
            throw new MissingConfigurationException(errorMessage);
        }
    }

    private boolean isConfigurationSet() {
        boolean isConfigurationSet = !StringUtils.isEmpty(eiffelMessage)
                && !StringUtils.isEmpty(eiffelType);
        return isConfigurationSet;
    }

    private void assembleRequest() throws UnsupportedEncodingException {
        final String username = pluginConfig.getRemremUsername();
        final String password = pluginConfig.getRemremPassword();
        final String url = pluginConfig.getRemremPublishURL();

        httpRequest.setBasicAuth(username, password);
        httpRequest.setBaseUrl(url);
        httpRequest.setEndpoint(GENERATE_PUBLISH_ENDPOINT);
        httpRequest.addParameter(MESSAGE_PROTOCOL, EIFFEL_PROTOCOL);
        httpRequest.addParameter(MESSAGE_TYPE, eiffelType);
        httpRequest.setHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        httpRequest.setBody(this.eiffelMessage);
    }
}
