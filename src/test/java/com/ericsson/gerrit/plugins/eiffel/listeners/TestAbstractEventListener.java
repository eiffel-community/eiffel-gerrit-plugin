package com.ericsson.gerrit.plugins.eiffel.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.google.common.base.Supplier;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.Event;

public class TestAbstractEventListener {

    private ListenerTestMock listenerTestMock;
    private EiffelPluginConfiguration pluginConfig;
    private ChangeMergedEvent changeMergedEvent;
    private Supplier<ChangeAttribute> supplierChangeAttribute;
    private Supplier<PatchSetAttribute> supplierPatchSetAttribute;
    private ChangeAttribute changeAttribute;
    private PatchSetAttribute patchSetAttribute;
    private AccountAttribute accountAttribute;

    @Before
    public void init() {
        listenerTestMock = new ListenerTestMock(null, null);
        pluginConfig = mock(EiffelPluginConfiguration.class);

        setUpMocks();
        populateChangeMergedEvent();
    }

    @Test
    public void testVerifyPluginDisabled() throws Throwable {
        boolean enabled;
        when(pluginConfig.isEnabled()).thenReturn(false);
        enabled = listenerTestMock.verifyPluginEnabled(changeMergedEvent, pluginConfig);
        assertFalse("Plugin should be disabled when config isEnabled is false.", enabled);
    }

    @Test
    public void testVerifyPluginEnabledForMyBranch() throws Throwable {
        boolean enabled;
        when(pluginConfig.isEnabled()).thenReturn(true);
        when(pluginConfig.getFilter()).thenReturn("my-branch");

        enabled = listenerTestMock.verifyPluginEnabled(changeMergedEvent, pluginConfig);
        assertTrue("Plugin should be enabled config filter and branch from GerritEvent match.",
                enabled);
    }

    @Test
    public void testVerifyPluginEnabledForMyBranchUsingMultipleFilter() throws Throwable {
        boolean enabled;
        when(pluginConfig.isEnabled()).thenReturn(true);
        when(pluginConfig.getFilter()).thenReturn("nope nupe my-branch nepp nupp");

        enabled = listenerTestMock.verifyPluginEnabled(changeMergedEvent, pluginConfig);
        assertTrue("Plugin should be enabled config filter and branch from GerritEvent match.",
                enabled);
    }

    @Test
    public void testVerifyPluginEnabledForMyBranchUsingMultipleFilterAndRegex() throws Throwable {
        boolean enabled;
        when(pluginConfig.isEnabled()).thenReturn(true);
        when(pluginConfig.getFilter()).thenReturn("nope nupe (my-).* nepp nupp");

        enabled = listenerTestMock.verifyPluginEnabled(changeMergedEvent, pluginConfig);
        assertTrue("Plugin should be enabled config filter and branch from GerritEvent match.",
                enabled);
    }

    @Test
    public void testVerifyPluginDisableForAnotherBranch() throws Throwable {
        boolean enabled;
        when(pluginConfig.isEnabled()).thenReturn(true);
        when(pluginConfig.getFilter()).thenReturn("another-branch");

        enabled = listenerTestMock.verifyPluginEnabled(changeMergedEvent, pluginConfig);
        assertFalse(
                "Plugin should be disabled when config filter and branch from GerritEvent does not match.",
                enabled);
    }

    @Test
    public void testVerifyPluginEnabledForAnotherBranchNoFilter() throws Throwable {
        boolean enabled;
        when(pluginConfig.isEnabled()).thenReturn(true);
        when(pluginConfig.getFilter()).thenReturn("");

        enabled = listenerTestMock.verifyPluginEnabled(changeMergedEvent, pluginConfig);
        assertTrue("Plugin should be enabled when config filter is empty.", enabled);
    }

    @SuppressWarnings("unchecked")
    private void setUpMocks() {
        pluginConfig = mock(EiffelPluginConfiguration.class);
        changeMergedEvent = mock(ChangeMergedEvent.class);
        supplierChangeAttribute = mock(Supplier.class);
        changeAttribute = mock(ChangeAttribute.class);
        supplierPatchSetAttribute = mock(Supplier.class);
        patchSetAttribute = mock(PatchSetAttribute.class);
        accountAttribute = mock(AccountAttribute.class);

        when(supplierChangeAttribute.get()).thenReturn(changeAttribute);
        when(supplierPatchSetAttribute.get()).thenReturn(patchSetAttribute);
    }

    private void populateChangeMergedEvent() {
        changeMergedEvent.newRev = "00000000-0000-0000-0000-000000000000";
        changeMergedEvent.change = supplierChangeAttribute;
        changeAttribute.project = "my-project";
        changeAttribute.branch = "my-branch";
        changeAttribute.url = "http://my-url.com";
        changeMergedEvent.patchSet = supplierPatchSetAttribute;
        patchSetAttribute.author = accountAttribute;
        accountAttribute.username = "my-user";
        accountAttribute.email = "my@email.com";
    }
}

/**
 * Class to extend AbstractEventListener to enable testing of logic.
 *
 */
class ListenerTestMock extends AbstractEventListener {

    public ListenerTestMock(String pluginName, File pluginDir) {
        super(pluginName, pluginDir);
    }

    /**
     * This method is created to be able to extend the isEventSendingEnabled method to the test
     * class.
     *
     * @param gerritEvent
     * @param pluginConfig
     * @return
     */
    public boolean verifyPluginEnabled(Event gerritEvent, EiffelPluginConfiguration pluginConfig) {
        return isEiffelEventSendingEnabled(gerritEvent, pluginConfig);
    }

    /**
     * This method is enforced by the AbstractEventListener and not used in test.
     */
    @Override
    public void onEvent(Event event) {
        // Not used in test
    }

    /**
     * This method is enforced by the AbstractEventListener and not used in test.
     */
    @Override
    protected boolean isExpectedGerritEvent(Event gerritEvent) {
        // Not used in test
        return false;
    }

    /**
     * This method is enforced by the AbstractEventListener and not used in test.
     */
    @Override
    protected void prepareAndSendEiffelEvent(Event gerritEvent,
            EiffelPluginConfiguration pluginConfig) {
        // Not used in test
    }

}