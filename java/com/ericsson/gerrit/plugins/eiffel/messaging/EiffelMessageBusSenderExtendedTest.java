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

package com.ericsson.gerrit.plugins.eiffel.messaging;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.duraci.datawrappers.MessageBus;
import com.ericsson.duraci.datawrappers.scm.identifiers.Git;
import com.ericsson.duraci.eiffelmessage.sending.MessageSender;
import com.ericsson.duraci.eiffelmessage.sending.exceptions.EiffelMessageSenderException;
import com.ericsson.eiffel.utils.parser.message.WorkItemPattern;
import com.ericsson.gerrit.plugins.eiffel.common.TestDefaults;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.google.gerrit.reviewdb.client.Project.NameKey;

@RunWith(PowerMockRunner.class)
@PrepareForTest(EiffelMessageBusSender.class)
public class EiffelMessageBusSenderExtendedTest extends TestDefaults {

    @Test
    public void testSendMessage() throws EiffelMessageSenderException {
        // Verify that message is sent as expected
        PowerMock.replay(nameKeyMock, NameKey.class, pluginConfigFactoryMock, eiffelPluginConfigurationMock,
                EiffelPluginConfiguration.class, messageBusMock, MessageBus.class, gitMock, scmChangeSetMock,
                messageSenderMock, messageSenderFactoryMock, MessageSender.Factory.class, eiffelMessageMock);
        Git gitId = new Git(DEFAULT_SHA1, DEFAULT_BRANCH, DEFAULT_PROJECT, uriMock);
        EiffelMessageBusSender sender = new EiffelMessageBusSender(DEFAULT_PLUGIN, DEFAULT_PROJECT,
                eiffelPluginConfigurationMock);
        sender.createSCMChangedMessageSender(gitId, scmChangeSetMock, null, null).sendMessage();
        PowerMock.verify(nameKeyMock, pluginConfigFactoryMock, eiffelPluginConfigurationMock, messageBusMock, gitMock,
                scmChangeSetMock, messageSenderMock, messageSenderFactoryMock, eiffelMessageMock);
    }

    @Test
    public void testPatternsNewFormat1() {
        // Verify that new format of custom patterns is parsed as expected
        String patterns = "{\"type\": \"OSS Jira\", \"pattern\": \"[A-Za-z0-9]+\\\\s*\\\\:\\\\s*[A-Z][A-Z_0-9]+-[0-9]+\", \"baseURL\": \"http://jira-oss.lmera.ericsson.se/browse/\"}";
        expect(eiffelPluginConfigurationMock.getPatterns()).andReturn(patterns);
        PowerMock.replay(nameKeyMock, NameKey.class, pluginConfigFactoryMock, eiffelPluginConfigurationMock,
                EiffelPluginConfiguration.class, messageBusMock, MessageBus.class);
        EiffelMessageBusSender sender = new EiffelMessageBusSender(DEFAULT_PLUGIN, DEFAULT_PROJECT,
                eiffelPluginConfigurationMock);
        List<WorkItemPattern> patternsList = sender.getCustomPatterns();
        assertTrue(patternsList.size() == 1);
        assertTrue(patternsList.get(0).getClass() == WorkItemPattern.class);
        assertEquals("OSS Jira", patternsList.get(0).getType());
        assertEquals("[A-Za-z0-9]+\\s*\\:\\s*[A-Z][A-Z_0-9]+-[0-9]+", patternsList.get(0).getPattern().pattern());
        assertEquals("http://jira-oss.lmera.ericsson.se/browse/", patternsList.get(0).getBaseURL());
        PowerMock.verify(nameKeyMock, pluginConfigFactoryMock, eiffelPluginConfigurationMock, messageBusMock);
    }

    @Test
    public void testPatternsNewFormat2() {
        // Verify that new format of custom patterns is parsed as expected if we have
        // more than one pattern provided
        String patterns = "{\"type\": \"type1\", \"pattern\": \"pattern1\", \"baseURL\": \"url1\"} {\"type\": \"type2\", \"pattern\": \"pattern2\", \"baseURL\": \"url2\"}";
        expect(eiffelPluginConfigurationMock.getPatterns()).andReturn(patterns);
        PowerMock.replay(nameKeyMock, NameKey.class, pluginConfigFactoryMock, eiffelPluginConfigurationMock,
                EiffelPluginConfiguration.class, messageBusMock, MessageBus.class);
        EiffelMessageBusSender sender = new EiffelMessageBusSender(DEFAULT_PLUGIN, DEFAULT_PROJECT,
                eiffelPluginConfigurationMock);
        List<WorkItemPattern> patternsList = sender.getCustomPatterns();
        assertTrue(patternsList.size() == 2);
        assertTrue(patternsList.get(0).getClass() == WorkItemPattern.class);
        assertTrue(patternsList.get(1).getClass() == WorkItemPattern.class);
        assertEquals("type1", patternsList.get(0).getType());
        assertEquals("pattern1", patternsList.get(0).getPattern().pattern());
        assertEquals("url1", patternsList.get(0).getBaseURL());
        assertEquals("type2", patternsList.get(1).getType());
        assertEquals("pattern2", patternsList.get(1).getPattern().pattern());
        assertEquals("url2", patternsList.get(1).getBaseURL());
        PowerMock.verify(nameKeyMock, pluginConfigFactoryMock, eiffelPluginConfigurationMock, messageBusMock);
    }

    @Test
    public void testPatternsNewFormat3() {
        // Verify that old format is still recognized
        String patterns = "Jira=[A-Za-z0-9]+\\\\s*\\\\:\\\\s*[A-Z][A-Z_0-9]+-[0-9]+ Hansoft=[A-Za-z0-9]+\\\\s*\\\\:\\\\s*Hansoft-[0-9]+";
        expect(eiffelPluginConfigurationMock.getPatterns()).andReturn(patterns);
        PowerMock.replay(nameKeyMock, NameKey.class, pluginConfigFactoryMock, eiffelPluginConfigurationMock,
                EiffelPluginConfiguration.class, messageBusMock, MessageBus.class);
        EiffelMessageBusSender sender = new EiffelMessageBusSender(DEFAULT_PLUGIN, DEFAULT_PROJECT,
                eiffelPluginConfigurationMock);
        List<WorkItemPattern> patternsList = sender.getCustomPatterns();
        assertTrue(patternsList.size() == 2);
        assertTrue(patternsList.get(0).getClass() == WorkItemPattern.class);
        assertTrue(patternsList.get(1).getClass() == WorkItemPattern.class);
        assertEquals("Jira", patternsList.get(0).getType());
        assertEquals("[A-Za-z0-9]+\\\\s*\\\\:\\\\s*[A-Z][A-Z_0-9]+-[0-9]+", patternsList.get(0).getPattern().pattern());
        assertEquals("", patternsList.get(0).getBaseURL());
        assertEquals("Hansoft", patternsList.get(1).getType());
        assertEquals("[A-Za-z0-9]+\\\\s*\\\\:\\\\s*Hansoft-[0-9]+", patternsList.get(1).getPattern().pattern());
        assertEquals("", patternsList.get(1).getBaseURL());
        PowerMock.verify(nameKeyMock, pluginConfigFactoryMock, eiffelPluginConfigurationMock, messageBusMock);
    }
}
