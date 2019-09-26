package com.ericsson.gerrit.plugins.eiffel.events.generators;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Test;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeSubmittedEvent;
import com.google.common.base.Supplier;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gson.Gson;

public class EiffelEventGeneratorTest {
    private ChangeMergedEvent changeMergedEvent;
    private EiffelPluginConfiguration pluginConfig;
    private Supplier<ChangeAttribute> supplierChangeAttribute;
    private Supplier<PatchSetAttribute> supplierPatchSetAttribute;
    private ChangeAttribute changeAttribute;
    private PatchSetAttribute patchSetAttribute;
    private AccountAttribute accountAttribute;
    
    Gson gson = new Gson();

    @SuppressWarnings("unchecked")
    @Test
    public void EiffelSourceChangeSubmittedEventGeneratorTest() {
        changeMergedEvent = mock(ChangeMergedEvent.class);
        pluginConfig = mock(EiffelPluginConfiguration.class);
        supplierChangeAttribute = (Supplier<ChangeAttribute>) mock(Supplier.class);
        changeAttribute = mock(ChangeAttribute.class);
        patchSetAttribute = mock(PatchSetAttribute.class);
        supplierPatchSetAttribute = (Supplier<PatchSetAttribute>) mock(Supplier.class);
        accountAttribute = mock(AccountAttribute.class);
        changeMergedEvent.newRev = "00000000-0000-0000-0000-000000000000";
        changeMergedEvent.change = supplierChangeAttribute;
        when(supplierChangeAttribute.get()).thenReturn(changeAttribute);
        changeAttribute.url = "http://my-url.com";
        changeAttribute.project = "my-project";
        changeAttribute.branch = "my-branch";
        changeMergedEvent.patchSet = supplierPatchSetAttribute;
        when(supplierPatchSetAttribute.get()).thenReturn(patchSetAttribute);
        patchSetAttribute.author = accountAttribute;
        accountAttribute.username = "my-user";
        accountAttribute.email = "my@email.com";

        EiffelSourceChangeSubmittedEvent eiffelEvent = EiffelSourceChangeSubmittedEventGenerator.generate(
                changeMergedEvent, pluginConfig);
        String eiffelEventJson = gson.toJson(eiffelEvent);
        System.out.println(eiffelEventJson);
    }
}
