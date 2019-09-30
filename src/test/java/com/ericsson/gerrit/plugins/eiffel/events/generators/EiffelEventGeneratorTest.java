package com.ericsson.gerrit.plugins.eiffel.events.generators;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeCreatedEvent;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeSubmittedEvent;
import com.google.common.base.Supplier;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ EiffelEventGenerator.class, InetAddress.class })
public class EiffelEventGeneratorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EiffelEventGeneratorTest.class);

    private static final String SCS_EVENT = "EiffelSourceChangeSubmittedEvent";
    private static final String SCS_VERSION = "3.0.0";
    private static final String SCC_EVENT = "EiffelSourceChangeCreatedEvent";
    private static final String SCC_VERSION = "4.0.0";
    private static final String COMMIT_ID = "00000000-0000-0000-0000-000000000000";
    private static final String SOURCE_NAME = "Eiffel Gerrit Plugin";
    private static final String PROJECT = "my-project";
    private static final String BRANCH = "my-branch";
    private static final String URL = "http://my-url.com";
    private static final String USERNAME = "my-user";
    private static final String EMAIL = "my@email.com";
    private static final String UNKNOWN = "unknown";
    private static final int DEFAULT_PORT = 29418;
    private static final String DEFAULT_HOST = "ssh://gerritmirror:" + DEFAULT_PORT;

    private Gson gson = new Gson();

    private PatchSetCreatedEvent patchSetCreatedEvent;
    private ChangeMergedEvent changeMergedEvent;
    private EiffelPluginConfiguration pluginConfig;
    private Supplier<ChangeAttribute> supplierChangeAttribute;
    private Supplier<PatchSetAttribute> supplierPatchSetAttribute;
    private ChangeAttribute changeAttribute;
    private PatchSetAttribute patchSetAttribute;
    private AccountAttribute accountAttribute;

    @Test
    public void eiffelSourceChangeSubmittedEventGeneratorTest() {
        setUpMocks();
        populateChangeMergedEvent();

        EiffelSourceChangeSubmittedEvent eiffelEvent = EiffelSourceChangeSubmittedEventGenerator.generate(
                changeMergedEvent, pluginConfig);

        verifyEiffelSourceChangeSubmittedEvent(eiffelEvent);
    }

    @Test
    public void eiffelSourceChangeCreatedEventGeneratorTest() {
        setUpMocks();
        populatePatchSetCreatedEvent();

        EiffelSourceChangeCreatedEvent eiffelEvent = EiffelSourceChangeCreatedEventGenerator.generate(
                patchSetCreatedEvent, pluginConfig);

        verifyEiffelSourceChangeCreatedEvent(eiffelEvent);
    }

    @Test
    public void eiffelEventGeneratorHostNameExceptionTest() {
        setUpHostNameExceptionMock();

        String hostName = EiffelEventGenerator.determineHostName();
        assertTrue(hostName.equals(UNKNOWN));
    }

    @Test
    public void eiffelEventGeneratorRepoUriExceptionTest() {
        setUpRepoUriExceptionMocks();

        String repoURI = EiffelEventGenerator.createRepoURI(URL, PROJECT);
        assertTrue(repoURI.equals(UNKNOWN));
    }

    @Test
    public void eiffelEventGeneratorSshBaseUrlExceptionTest() {
        setUpSshBaseUrlExceptionMocks();

        String repoURI = EiffelEventGenerator.createRepoURI(URL, PROJECT);
        assertTrue(repoURI.equals(DEFAULT_HOST));
    }

    @SuppressWarnings("unchecked")
    private void setUpMocks() {
        pluginConfig = mock(EiffelPluginConfiguration.class);
        changeMergedEvent = mock(ChangeMergedEvent.class);
        patchSetCreatedEvent = mock(PatchSetCreatedEvent.class);
        supplierChangeAttribute = (Supplier<ChangeAttribute>) mock(Supplier.class);
        changeAttribute = mock(ChangeAttribute.class);
        supplierPatchSetAttribute = (Supplier<PatchSetAttribute>) mock(Supplier.class);
        patchSetAttribute = mock(PatchSetAttribute.class);
        accountAttribute = mock(AccountAttribute.class);

        when(supplierChangeAttribute.get()).thenReturn(changeAttribute);
        when(supplierPatchSetAttribute.get()).thenReturn(patchSetAttribute);
    }

    private void setUpHostNameExceptionMock() {
        mockStatic(InetAddress.class);
        try {
            when(InetAddress.getLocalHost()).thenThrow(UnknownHostException.class);
        } catch (Exception ignore) {
        }
    }

    private void setUpRepoUriExceptionMocks() {
        try {
            whenNew(URI.class).withArguments(URL).thenThrow(URISyntaxException.class);
        } catch (Exception ignore) {
        }
    }

    private void setUpSshBaseUrlExceptionMocks() {
        try {
            URI uri = new URI(URL);
            String host = uri.getHost();
            whenNew(URI.class).withArguments(URL).thenReturn(uri);
            whenNew(URI.class).withArguments("ssh", null, host, DEFAULT_PORT, "/", null, null)
                              .thenThrow(URISyntaxException.class);
        } catch (Exception ignore) {
        }
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

    private void populatePatchSetCreatedEvent() {
        patchSetCreatedEvent.change = supplierChangeAttribute;
        changeAttribute.project = "my-project";
        changeAttribute.branch = "my-branch";
        changeAttribute.url = "http://my-url.com";
        patchSetCreatedEvent.patchSet = supplierPatchSetAttribute;
        patchSetAttribute.revision = "00000000-0000-0000-0000-000000000000";
        patchSetAttribute.author = accountAttribute;
        accountAttribute.username = "my-user";
        accountAttribute.email = "my@email.com";
    }

    private void verifyEiffelSourceChangeSubmittedEvent(
            EiffelSourceChangeSubmittedEvent eiffelEvent) {
        JsonObject eiffelEventJson = gson.toJsonTree(eiffelEvent).getAsJsonObject();
        LOGGER.debug("EiffelSourceChangeSubmittedEvent : \n{}", eiffelEventJson.toString());

        JsonObject meta = eiffelEventJson.getAsJsonObject("msgParams").getAsJsonObject("meta");
        JsonObject source = meta.getAsJsonObject("source");
        JsonObject data = eiffelEventJson.getAsJsonObject("eventParams").getAsJsonObject("data");
        JsonObject submitter = data.getAsJsonObject("submitter");
        JsonObject gitIdentifier = data.getAsJsonObject("gitIdentifier");

        assertTrue(meta.get("type").getAsString().equals(SCS_EVENT));
        assertTrue(meta.get("version").getAsString().equals(SCS_VERSION));
        assertTrue(source.get("name").getAsString().equals(SOURCE_NAME));
        assertTrue(source.get("uri").getAsString().equals(URL));
        assertTrue(submitter.get("name").getAsString().equals(USERNAME));
        assertTrue(submitter.get("email").getAsString().equals(EMAIL));
        assertTrue(gitIdentifier.get("commitId").getAsString().equals(COMMIT_ID));
        assertTrue(gitIdentifier.get("branch").getAsString().equals(BRANCH));
        assertTrue(gitIdentifier.get("repoName").getAsString().equals(PROJECT));
    }

    private void verifyEiffelSourceChangeCreatedEvent(
            EiffelSourceChangeCreatedEvent eiffelEvent) {
        JsonObject eiffelEventJson = gson.toJsonTree(eiffelEvent).getAsJsonObject();
        LOGGER.debug("EiffelSourceChangeCreatedEvent : \n{}", eiffelEventJson.toString());

        JsonObject meta = eiffelEventJson.getAsJsonObject("msgParams").getAsJsonObject("meta");
        JsonObject source = meta.getAsJsonObject("source");
        JsonObject data = eiffelEventJson.getAsJsonObject("eventParams").getAsJsonObject("data");
        JsonObject author = data.getAsJsonObject("author");
        JsonObject gitIdentifier = data.getAsJsonObject("gitIdentifier");

        assertTrue(meta.get("type").getAsString().equals(SCC_EVENT));
        assertTrue(meta.get("version").getAsString().equals(SCC_VERSION));
        assertTrue(source.get("name").getAsString().equals(SOURCE_NAME));
        assertTrue(source.get("uri").getAsString().equals(URL));
        assertTrue(author.get("name").getAsString().equals(USERNAME));
        assertTrue(author.get("email").getAsString().equals(EMAIL));
        assertTrue(gitIdentifier.get("commitId").getAsString().equals(COMMIT_ID));
        assertTrue(gitIdentifier.get("branch").getAsString().equals(BRANCH));
        assertTrue(gitIdentifier.get("repoName").getAsString().equals(PROJECT));
    }
}
