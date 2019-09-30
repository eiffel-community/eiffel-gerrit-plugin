package com.ericsson.gerrit.plugins.eiffel.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.text.ParseException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.duraci.datawrappers.MessageBus;
import com.ericsson.duraci.datawrappers.scm.SCMChangeSet;
import com.ericsson.duraci.datawrappers.scm.identifiers.Git;
import com.ericsson.duraci.eiffelmessage.serialization.Serializer;
import com.ericsson.duraci.eiffelmessage.serialization.printing.Printer;
import com.ericsson.duraci.eiffelmessage.serialization.printing.exceptions.MessagePrintingException;
import com.ericsson.eiffel.utils.parser.message.SCMChangeMessageParser;
import com.ericsson.eiffel.utils.parser.message.WorkItemPattern;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.google.gson.JsonSyntaxException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = { "com.ericsson.gerrit.plugins.eiffel.*", "com.ericsson.eiffel.utils.*" })
public class EiffelMessageBusSenderTest {

    private final String PLUGIN_NAME = "plugin";
    private final String PROJECT_NAME = "project";
    private final boolean ENABLED_TRUE = true;
    private final String FILTER = "";
    private final String PATTERNS = "{patterns}";
    private final String PATTERNS_NO_MATCH_INVALID_PARSE = "patternWithInvalidParse";
    private final String PATTERNS_NO_MATCH_VALID_PARSE = "pattern=withValidParse";
    private final String BRANCH = "branch";
    private final String PRINT = "print";

    private EiffelPluginConfiguration pluginConfiguration;
    private MessageBus messageBus;
    private Git git;
    private SCMChangeSet scmChangeSet;
    private Serializer serializer;
    private Printer printer;

    @Before
    public void init() throws Exception {
        pluginConfiguration = mock(EiffelPluginConfiguration.class);
        messageBus = mock(MessageBus.class);
        git = mock(Git.class);
        scmChangeSet = mock(SCMChangeSet.class);
        serializer = mock(Serializer.class);
        printer = mock(Printer.class);

        whenNew(MessageBus.class).withAnyArguments().thenReturn(messageBus);
        whenNew(Serializer.class).withAnyArguments().thenReturn(serializer);
        when(pluginConfiguration.isEnabled()).thenReturn(ENABLED_TRUE);
        when(pluginConfiguration.getFilter()).thenReturn(FILTER);
        when(pluginConfiguration.getPatterns()).thenReturn(PATTERNS);
        when(git.getBranch()).thenReturn(BRANCH);
        when(serializer.pretty(Mockito.any())).thenReturn(printer);
        when(printer.versionNeutralLatestOnly()).thenReturn(printer);
        when(printer.print()).thenReturn(PRINT);

        PowerMockito.mockStatic(SCMChangeMessageParser.class);
    }

    @Test
    public void test_EiffelMessageBusSender() {
        assertThatCode(() -> new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME,
                pluginConfiguration)).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_EiffelMessageBusSender_createSCMChangedMessageSender_Exception3() throws MessagePrintingException {
        when(printer.print()).thenThrow(MessagePrintingException.class);

        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);
        mbSender.createSCMChangedMessageSender(git, scmChangeSet, null, null);

        assertThatThrownBy(() -> printer.print()).isInstanceOf(MessagePrintingException.class);
    }

    @Test
    public void test_EiffelMessageBusSender_createSCMChangedMessageSender_FilterNULL() {
        when(pluginConfiguration.getFilter()).thenReturn(null);

        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);
        mbSender.createSCMChangedMessageSender(git, scmChangeSet, null, null);
    }

    @Test
    public void test_EiffelMessageBusSender_createSCMChangedMessageSender_FilterMatch() {
        when(pluginConfiguration.getFilter()).thenReturn(BRANCH);

        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);
        mbSender.createSCMChangedMessageSender(git, scmChangeSet, null, null);
    }

    @Test
    public void test_EiffelMessageBusSender_createSCMChangedMessageSender() {
        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);
        mbSender.createSCMChangedMessageSender(git, scmChangeSet, null, null);
    }

    @Test
    public void test_EiffelMessageBusSender_createSCMChangedMessageTask() {
        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);

        assertThatCode(
                () -> mbSender.createSCMChangedMessageTask(git, scmChangeSet, null, null)).doesNotThrowAnyException();
    }

    @Test
    public void test_EiffelMessageBusSender_getCustomPatterns_PatternsNULL() {
        when(pluginConfiguration.getPatterns()).thenReturn(null);

        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);
        List<WorkItemPattern> patternsList = mbSender.getCustomPatterns();
        assertThat(patternsList.size() == 0).isTrue();
    }

    @Test
    public void test_EiffelMessageBusSender_getCustomPatterns_PatternsNoMatchInvalidParse() {
        when(pluginConfiguration.getPatterns()).thenReturn(PATTERNS_NO_MATCH_INVALID_PARSE);

        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);
        List<WorkItemPattern> patternsList = mbSender.getCustomPatterns();
        assertThat(patternsList.size() == 0).isTrue();
    }

    @Test
    public void test_EiffelMessageBusSender_getCustomPatterns_PatternsNoMatchValidParse() {
        when(pluginConfiguration.getPatterns()).thenReturn(PATTERNS_NO_MATCH_VALID_PARSE);

        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);
        List<WorkItemPattern> patternsList = mbSender.getCustomPatterns();
        assertThat(patternsList.size() == 1).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_EiffelMessageBusSender_getCustomPatterns_Exception1() throws JsonSyntaxException, ParseException {
        when(SCMChangeMessageParser.parseCustomPatternsString(Mockito.any())).thenThrow(JsonSyntaxException.class);

        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);
        List<WorkItemPattern> patternsList = mbSender.getCustomPatterns();

        assertThatThrownBy(() -> SCMChangeMessageParser.parseCustomPatternsString(Mockito.any())).isInstanceOf(
                JsonSyntaxException.class);
        assertThat(patternsList.size() == 0).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_EiffelMessageBusSender_getCustomPatterns_Exception2() throws JsonSyntaxException, ParseException {
        when(SCMChangeMessageParser.parseCustomPatternsString(Mockito.any())).thenThrow(ParseException.class);

        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);
        List<WorkItemPattern> patternsList = mbSender.getCustomPatterns();

        assertThatThrownBy(() -> SCMChangeMessageParser.parseCustomPatternsString(Mockito.any())).isInstanceOf(
                ParseException.class);
        assertThat(patternsList.size() == 0).isTrue();
    }

    @Test
    public void test_EiffelMessageBusSender_getCustomPatterns() throws JsonSyntaxException, ParseException {
        EiffelMessageBusSender mbSender = new EiffelMessageBusSender(PLUGIN_NAME, PROJECT_NAME, pluginConfiguration);
        List<WorkItemPattern> patternsList = mbSender.getCustomPatterns();
        assertThat(patternsList.size() == 0).isTrue();
    }
}