package com.ericsson.gerrit.plugins.eiffel.configuration;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.assertj.core.api.Assertions.*;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;

public class EiffelPluginConfigurationTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final String PLUGIN_NAME = "plugin";
    private final String FILTER = null;
    private final String FLOW_CONTEXT = null;
    private final boolean ENABLED_TRUE = true;
    private final boolean ENABLED_FALSE = false;
    private final String REMREM_GENERATE_URL = "https://localhost:8080/generate";
    private final String REMREM_PUBLISH_URL = "https://localhost:8080/publish";
    private final String REMREM_USERNAME = "dummyUser";
    private final String REMREM_PASSWORD = "dummypassword";

    private NameKey nameKey;
    private PluginConfigFactory pluginConfigFactory;
    private PluginConfig pluginConfig;

    @Before
    public void init() throws NoSuchProjectException {
        nameKey = mock(NameKey.class);
        pluginConfigFactory = mock(PluginConfigFactory.class);
        pluginConfig = mock(PluginConfig.class);

        when(pluginConfigFactory.getFromProjectConfig(nameKey, PLUGIN_NAME)).thenReturn(pluginConfig);
        when(pluginConfig.getBoolean(EiffelPluginConfiguration.ENABLED, false)).thenReturn(ENABLED_TRUE);
        when(pluginConfig.getString(EiffelPluginConfiguration.FILTER)).thenReturn(FILTER);
        when(pluginConfig.getString(EiffelPluginConfiguration.FLOW_CONTEXT)).thenReturn(FLOW_CONTEXT);
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_GENERATE_URL)).thenReturn(REMREM_GENERATE_URL);
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_PUBLISH_URL)).thenReturn(REMREM_PUBLISH_URL);
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_USERNAME)).thenReturn(REMREM_USERNAME);
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_PASSWORD)).thenReturn(REMREM_PASSWORD);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_EiffelPluginConfiguration_Exception1() throws NoSuchProjectException {
        when(pluginConfigFactory.getFromProjectConfig(nameKey, PLUGIN_NAME)).thenThrow(NoSuchProjectException.class);
        exception.expect(ExceptionInInitializerError.class);

        new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory);
    }

    @Test
    public void test_EiffelPluginConfiguration_Exception2() throws NoSuchProjectException {
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_GENERATE_URL)).thenReturn(null);
        exception.expect(ExceptionInInitializerError.class);

        new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory);
    }

    @Test
    public void test_EiffelPluginConfiguration_Exception3() throws NoSuchProjectException {
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_PUBLISH_URL)).thenReturn(null);
        exception.expect(ExceptionInInitializerError.class);

        new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory);
    }

    @Test
    public void test_EiffelPluginConfiguration_Exception4() throws NoSuchProjectException {
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_USERNAME)).thenReturn(null);
        exception.expect(ExceptionInInitializerError.class);

        new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory);
    }

    @Test
    public void test_EiffelPluginConfiguration_Exception5() throws NoSuchProjectException {
        when(pluginConfig.getString(EiffelPluginConfiguration.REMREM_PASSWORD)).thenReturn(null);
        exception.expect(ExceptionInInitializerError.class);

        new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory);
    }

    @Test
    public void test_EiffelPluginConfiguration_Disabled() throws NoSuchProjectException {
        when(pluginConfig.getBoolean(EiffelPluginConfiguration.ENABLED, false)).thenReturn(ENABLED_FALSE);

        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.isCfgEnabled() == false).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration() throws NoSuchProjectException {
        assertThatCode(() -> new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory))
                .doesNotThrowAnyException();
    }

    @Test
    public void test_EiffelPluginConfiguration_getRemremGenerateURL() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getCfgRemremGenerateURL() == REMREM_GENERATE_URL).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_getRemremPublishURL() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getCfgRemremPublishURL() == REMREM_PUBLISH_URL).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_getRemremUsername() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getCfgRemremUsername() == REMREM_USERNAME).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_getFilter() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getCfgFilter() == FILTER).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_getFlowContext() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getCfgFlowContext() == FLOW_CONTEXT).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_isEnabled() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.isCfgEnabled() == ENABLED_TRUE).isTrue();
    }

}