package com.ericsson.gerrit.plugins.eiffel.configuration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.junit.Assert.assertEquals;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;

public class EiffelPluginConfigurationTest {

    private static final String PROJECT_NAME = "project";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final String PLUGIN_NAME = "plugin";
    private final String[] FILTER = null;
    private final String[] FLOW_CONTEXT = null;
    private final boolean ENABLED_TRUE = true;
    private final boolean ENABLED_FALSE = false;
    private final String REMREM_PUBLISH_URL = "https://localhost:8080/publish";
    private final String REMREM_USERNAME = "dummyUser";
    private final String REMREM_PASSWORD = "dummypassword";

    private NameKey nameKey;
    private PluginConfigFactory pluginConfigFactory;
    private PluginConfig pluginConfig;

    @Before
    public void init() throws NoSuchProjectException {
        nameKey = new NameKey(PROJECT_NAME);
        pluginConfigFactory = mock(PluginConfigFactory.class);
        pluginConfig = mock(PluginConfig.class);

        when(pluginConfigFactory.getFromProjectConfig(nameKey, PLUGIN_NAME)).thenReturn(
                pluginConfig);
        when(pluginConfig.getBoolean(EiffelPluginConfiguration.ENABLED, false)).thenReturn(
                ENABLED_TRUE);
        when(pluginConfig.getStringList(EiffelPluginConfiguration.FILTER)).thenReturn(FILTER);
        when(pluginConfig.getStringList(EiffelPluginConfiguration.FLOW_CONTEXT)).thenReturn(
                FLOW_CONTEXT);
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_PUBLISH_URL)).thenReturn(
                REMREM_PUBLISH_URL);
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_USERNAME)).thenReturn(
                REMREM_USERNAME);
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_PASSWORD)).thenReturn(
                REMREM_PASSWORD);
    }

    @Test
    public void testEiffelPluginConfigurationException1() throws NoSuchProjectException {
        when(pluginConfigFactory.getFromProjectConfig(nameKey, PLUGIN_NAME)).thenThrow(
                NoSuchProjectException.class);
        exception.expect(ExceptionInInitializerError.class);

        new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory);
    }

    @Test
    public void testEiffelPluginConfigurationException2() throws NoSuchProjectException {
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_PUBLISH_URL)).thenReturn(null);
        exception.expect(ExceptionInInitializerError.class);

        new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory);
    }

    @Test
    public void testEiffelPluginConfigurationDisabled() throws NoSuchProjectException {
        when(pluginConfig.getBoolean(EiffelPluginConfiguration.ENABLED, false)).thenReturn(
                ENABLED_FALSE);

        final EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME,
                nameKey,
                pluginConfigFactory);
        assertEquals(false, pluginConfig.isEnabled());
    }

    @Test
    public void testEiffelPluginConfigurationRemremGenerateURL() throws NoSuchProjectException {
        final EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME,
                nameKey,
                pluginConfigFactory);
        assertEquals(REMREM_PUBLISH_URL, pluginConfig.getRemremPublishURL());
    }

    @Test
    public void testEiffelPluginConfigurationRemremUsername() throws NoSuchProjectException {
        final EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME,
                nameKey,
                pluginConfigFactory);
        assertEquals(REMREM_USERNAME, pluginConfig.getRemremUsername());
    }

    @Test
    public void testEiffelPluginConfigurationFilter() throws NoSuchProjectException {
        final EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME,
                nameKey,
                pluginConfigFactory);
        assertEquals("", pluginConfig.getFilter());
    }

    @Test
    public void testEiffelPluginConfigurationtFlowContext() throws NoSuchProjectException {
        final EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME,
                nameKey,
                pluginConfigFactory);
        assertEquals("", pluginConfig.getFlowContext());
    }

    @Test
    public void testEiffelPluginConfigurationEnabled() throws NoSuchProjectException {
        final EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME,
                nameKey,
                pluginConfigFactory);
        assertEquals(ENABLED_TRUE, pluginConfig.isEnabled());
    }

    @Test
    public void testEiffelPluginConfigurationGetProject() throws NoSuchProjectException {
        final EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME,
                nameKey,
                pluginConfigFactory);
        assertEquals(PROJECT_NAME, pluginConfig.getProject());
    }

}