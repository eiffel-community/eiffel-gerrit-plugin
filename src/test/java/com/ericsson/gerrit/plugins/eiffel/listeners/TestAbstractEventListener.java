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
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.ChangeEvent;
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

    @Test
    public void testPrepareAndSendEiffelEventNotCalledWhenPluginDisabled() throws Throwable {
        boolean methodWasCalled;
        when(pluginConfig.isEnabled()).thenReturn(false);

        methodWasCalled = listenerTestMock.isPrepareAndSendEiffelEventMethodCalled();
        assertFalse("Plugin should be enabled when config filter is empty.", methodWasCalled);
    }

    @Test
    public void testPrepareAndSendEiffelEventNotWrongEvent() throws Throwable {
        boolean methodWasCalled;
        listenerTestMock.setIsExpectedGerritEvent(false);

        listenerTestMock.onEvent(changeMergedEvent);
        methodWasCalled = listenerTestMock.isPrepareAndSendEiffelEventMethodCalled();
        assertFalse("Gerrit event listener should not try to send events on wrong gerrit event.",
                methodWasCalled);
    }

    @Test
    public void testPrepareAndSendEiffelEventNotCalledPluginDisabled() throws Throwable {
        boolean methodWasCalled;
        when(pluginConfig.isEnabled()).thenReturn(false);
        listenerTestMock.setIsExpectedGerritEvent(true);
        listenerTestMock.setPluginConfig(pluginConfig);

        listenerTestMock.onEvent(changeMergedEvent);
        methodWasCalled = listenerTestMock.isPrepareAndSendEiffelEventMethodCalled();
        assertFalse("Gerrit event listener should not try to send events when plugin is disabled.",
                methodWasCalled);
    }

    @Test
    public void testPrepareAndSendEiffelEventNotCalledWhenInvalidFilter() throws Throwable {
        boolean methodWasCalled;
        when(pluginConfig.isEnabled()).thenReturn(true);
        when(pluginConfig.getFilter()).thenReturn("not-valid-branch");
        listenerTestMock.setIsExpectedGerritEvent(false);

        listenerTestMock.onEvent(changeMergedEvent);
        methodWasCalled = listenerTestMock.isPrepareAndSendEiffelEventMethodCalled();
        assertFalse(
                "Gerrit event listener should not try to send events when branch does not match filter.",
                methodWasCalled);
    }

    @Test
    public void testPrepareAndSendEiffelEventCalledMultipleFilter() throws Throwable {
        boolean methodWasCalled;
        when(pluginConfig.isEnabled()).thenReturn(true);
        when(pluginConfig.getFilter()).thenReturn("nope nupe (my-).* nepp nupp");
        listenerTestMock.setIsExpectedGerritEvent(true);
        listenerTestMock.setPluginConfig(pluginConfig);

        listenerTestMock.onEvent(changeMergedEvent);
        methodWasCalled = listenerTestMock.isPrepareAndSendEiffelEventMethodCalled();
        assertTrue(
                "Gerrit event listener should send events when plugin is enabled and branch is set in filter.",
                methodWasCalled);
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

    private boolean isPrepareAndSendEiffelEventMethodCalled = false;
    private boolean isExpectedGerritEvent = false;
    private EiffelPluginConfiguration pluginConfig;

    public ListenerTestMock(String pluginName, File pluginDir) {
        super(pluginName, pluginDir);
    }

    public void setPluginConfig(EiffelPluginConfiguration pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public void setIsExpectedGerritEvent(boolean isExpectedGerritEvent) {
        this.isExpectedGerritEvent = isExpectedGerritEvent;
    }

    public boolean isPrepareAndSendEiffelEventMethodCalled() {
        if (isPrepareAndSendEiffelEventMethodCalled) {
            isPrepareAndSendEiffelEventMethodCalled = false;
            return true;
        }
        return false;
    }

    /**
     * Based on project name from the gerrit event, creates a project specific
     * EiffelPluginConfiguration.
     *
     * @param gerritEvent
     * @return EiffelPluginConfiguration
     */
    @Override
    public EiffelPluginConfiguration createPluginConfig(final Event gerritEvent) {
        return this.pluginConfig;
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
    protected boolean isExpectedGerritEvent(Event gerritEvent) {
        if (isExpectedGerritEvent) {
            isExpectedGerritEvent = false;
            return true;
        }
        return false;
    }

    /**
     * This method is enforced by the AbstractEventListener and not used in test.
     */
    @Override
    protected void prepareAndSendEiffelEvent(Event gerritEvent,
            EiffelPluginConfiguration pluginConfig) {
        isPrepareAndSendEiffelEventMethodCalled = true;
    }

}