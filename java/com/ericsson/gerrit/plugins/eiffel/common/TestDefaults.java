/**
 * COPYRIGHT
 * ---------
 * Ericsson AB 2014 All rights reserved.
 *
 * The information in this document is the property of Ericsson.
 * Except as specifically authorized in writing by Ericsson, the receiver of this
 * document shall keep the information contained herein confidential and shall
 * protect the same in whole or in part from disclosure and dissemination to third parties.
 * Disclosure and dissemination's to the receiver's employees shall only be
 * made on a strict need to know basis.
 *
 */

package com.ericsson.gerrit.plugins.eiffel.common;

import com.ericsson.duraci.datawrappers.MessageBus;
import com.ericsson.duraci.datawrappers.scm.SCMChangeSet;
import com.ericsson.duraci.datawrappers.scm.identifiers.Git;
import com.ericsson.duraci.eiffelmessage.messages.EiffelMessage;
import com.ericsson.duraci.eiffelmessage.messages.events.EiffelSCMChangedEvent;
import com.ericsson.duraci.eiffelmessage.sending.MessageSender;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelMessageBusConfiguration;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.net.URI;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.expectNew;

public class TestDefaults {

    // Default constants
    protected final String DEFAULT_PLUGIN = "pluginName";
    protected final String DEFAULT_PROJECT = "project";
    protected final String DEFAULT_SERVER = "server";
    protected final String DEFAULT_DOMAIN = "domain";
    protected final String DEFAULT_EXCHANGE = "exchange";
    protected final String DEFAULT_FILTER = null;
    protected final String DEFAULT_BRANCH = "master";
    protected final String DEFAULT_SHA1 = "0abc5";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    // Default mocks
    protected URI uriMock;
    protected EiffelPluginConfiguration eiffelPluginConfigurationMock;
    protected Project.NameKey nameKeyMock;
    protected PluginConfigFactory pluginConfigFactoryMock;
    protected PluginConfig pluginConfigMock;
    protected MessageBus messageBusMock;
    protected MessageSender messageSenderMock;
    protected Git gitMock;
    protected SCMChangeSet scmChangeSetMock;
    protected EiffelMessage eiffelMessageMock;
    protected MessageSender.Factory messageSenderFactoryMock;

    @Before
    public void setUp() throws Exception{
        // Initialize mocks
        nameKeyMock = createNiceMock(Project.NameKey.class);
        pluginConfigMock = createNiceMock(PluginConfig.class);
        uriMock = createNiceMock(URI.class);
        pluginConfigFactoryMock = createNiceMock(PluginConfigFactory.class);
        eiffelPluginConfigurationMock = createNiceMock(EiffelPluginConfiguration.class);
        messageBusMock = createNiceMock(MessageBus.class);
        gitMock = createNiceMock(Git.class);
        scmChangeSetMock = createNiceMock(SCMChangeSet.class);
        messageSenderMock = createNiceMock(MessageSender.class);
        eiffelMessageMock = createNiceMock(EiffelMessage.class);
        messageSenderFactoryMock = createNiceMock(MessageSender.Factory.class);

        // Mock new calls
        expectNew(Project.NameKey.class, DEFAULT_PROJECT).andStubReturn(nameKeyMock);
        expectNew(EiffelPluginConfiguration.class, DEFAULT_PLUGIN, nameKeyMock, pluginConfigFactoryMock).andStubReturn(eiffelPluginConfigurationMock);
        expectNew(MessageBus.class, DEFAULT_SERVER, DEFAULT_EXCHANGE).andStubReturn(messageBusMock);
        expectNew(MessageSender.Factory.class, anyObject(EiffelMessageBusConfiguration.class)).andStubReturn(messageSenderFactoryMock);

        // Add default return values
        expect(eiffelPluginConfigurationMock.getServer()).andStubReturn(DEFAULT_SERVER);
        expect(eiffelPluginConfigurationMock.getDomain()).andStubReturn(DEFAULT_DOMAIN);
        expect(eiffelPluginConfigurationMock.getExchange()).andStubReturn(DEFAULT_EXCHANGE);
        expect(eiffelPluginConfigurationMock.getFilter()).andStubReturn(DEFAULT_FILTER);
        expect(eiffelPluginConfigurationMock.isEnabled()).andStubReturn(true);
        expect(gitMock.getBranch()).andStubReturn(DEFAULT_BRANCH);
        expect(pluginConfigFactoryMock.getFromProjectConfig(nameKeyMock, DEFAULT_PLUGIN)).andStubReturn(pluginConfigMock);
        expect(pluginConfigMock.getString(EiffelPluginConfiguration.SERVER)).andStubReturn(DEFAULT_SERVER);
        expect(pluginConfigMock.getString(EiffelPluginConfiguration.DOMAIN)).andStubReturn(DEFAULT_DOMAIN);
        expect(pluginConfigMock.getString(EiffelPluginConfiguration.EXCHANGE)).andStubReturn(DEFAULT_EXCHANGE);
        expect(pluginConfigMock.getString(EiffelPluginConfiguration.FILTER)).andStubReturn(DEFAULT_FILTER);
        expect(pluginConfigMock.getBoolean(EiffelPluginConfiguration.ENABLED, false)).andStubReturn(true);
        expect(messageSenderMock.send(anyObject(EiffelSCMChangedEvent.class))).andStubReturn(eiffelMessageMock);
        expect(messageSenderFactoryMock.create()).andStubReturn(messageSenderMock);
    }
}
