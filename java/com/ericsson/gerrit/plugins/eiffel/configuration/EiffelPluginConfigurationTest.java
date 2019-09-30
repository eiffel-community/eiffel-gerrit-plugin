package com.ericsson.gerrit.plugins.eiffel.configuration;

import static org.assertj.core.api.Assertions.*;
import static org.powermock.api.mockito.PowerMockito.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;

public class EiffelPluginConfigurationTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final String PLUGIN_NAME = "plugin";
    private final String SERVER = "server";
    private final String EXCHANGE = "exchange";
    private final String DOMAIN = "domain";
    private final String FILTER = null;
    private final String PATTERNS = null;
    private final boolean ENABLED_TRUE = true;
    private final boolean ENABLED_FALSE = false;
    private final String GERRIT_BASE_URL = "ssh://gerritmirror:29418/";

    private NameKey nameKey;
    private PluginConfigFactory pluginConfigFactory;
    private PluginConfig pluginConfig;

    @Before
    public void init() throws NoSuchProjectException {
        nameKey = mock(NameKey.class);
        pluginConfigFactory = mock(PluginConfigFactory.class);
        pluginConfig = mock(PluginConfig.class);

        when(pluginConfigFactory.getFromProjectConfig(nameKey, PLUGIN_NAME)).thenReturn(pluginConfig);
        when(pluginConfig.getString(EiffelPluginConfiguration.SERVER)).thenReturn(SERVER);
        when(pluginConfig.getString(EiffelPluginConfiguration.EXCHANGE)).thenReturn(EXCHANGE);
        when(pluginConfig.getString(EiffelPluginConfiguration.DOMAIN)).thenReturn(DOMAIN);
        when(pluginConfig.getString(EiffelPluginConfiguration.FILTER)).thenReturn(FILTER);
        when(pluginConfig.getString(EiffelPluginConfiguration.PATTERNS)).thenReturn(PATTERNS);
        when(pluginConfig.getBoolean(EiffelPluginConfiguration.ENABLED, false)).thenReturn(ENABLED_TRUE);
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
        when(pluginConfig.getString(EiffelPluginConfiguration.DOMAIN)).thenReturn(null);
        exception.expect(ExceptionInInitializerError.class);

        new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory);
    }

    @Test
    public void test_EiffelPluginConfiguration_Exception3() throws NoSuchProjectException {
        when(pluginConfig.getString(EiffelPluginConfiguration.EXCHANGE)).thenReturn(null);
        exception.expect(ExceptionInInitializerError.class);

        new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory);
    }

    @Test
    public void test_EiffelPluginConfiguration_Exception4() throws NoSuchProjectException {
        when(pluginConfig.getString(EiffelPluginConfiguration.SERVER)).thenReturn(null);
        exception.expect(ExceptionInInitializerError.class);

        new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory);
    }

    @Test
    public void test_EiffelPluginConfiguration_Disabled() throws NoSuchProjectException {
        when(pluginConfig.getBoolean(EiffelPluginConfiguration.ENABLED, false)).thenReturn(ENABLED_FALSE);

        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.isEnabled() == false).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration() throws NoSuchProjectException {
        assertThatCode(() -> new EiffelPluginConfiguration(PLUGIN_NAME, nameKey, pluginConfigFactory))
                .doesNotThrowAnyException();
    }

    @Test
    public void test_EiffelPluginConfiguration_getServer() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getServer() == SERVER).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_getExchange() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getExchange() == EXCHANGE).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_getDomain() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getDomain() == DOMAIN).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_getFilter() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getFilter() == FILTER).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_getPatterns() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getPatterns() == PATTERNS).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_isEnabled() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.isEnabled() == ENABLED_TRUE).isTrue();
    }

    @Test
    public void test_EiffelPluginConfiguration_getGerritBaseUrl() throws NoSuchProjectException {
        EiffelPluginConfiguration pluginConfig = new EiffelPluginConfiguration(PLUGIN_NAME, nameKey,
                pluginConfigFactory);
        assertThat(pluginConfig.getGerritBaseUrl() == GERRIT_BASE_URL).isTrue();
    }
}