package com.ericsson.gerrit.plugins.eiffel.events.generators;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EventType;
import com.ericsson.gerrit.plugins.eiffel.events.models.Link;
import com.ericsson.gerrit.plugins.eiffel.git.CommitInformation;
import com.ericsson.gerrit.plugins.eiffel.storage.EventStorage;
import com.ericsson.gerrit.plugins.eiffel.storage.EventStorageFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ EventStorageFactory.class })
public class LinkGeneratorTest {

    private static final String PROJECT_NAME = "project";
    private EventStorage eventStorage;
    private LinkGenerator linkGenerator;
    private EiffelPluginConfiguration pluginConfiguration;
    private CommitInformation commitInformation;

    @Before
    public void setUp() throws Throwable {

        pluginConfiguration = mock(EiffelPluginConfiguration.class);
        when(pluginConfiguration.getProject()).thenReturn(PROJECT_NAME);
        mockStatic(EventStorageFactory.class);
        eventStorage = mock(EventStorage.class);
        when(eventStorage.getEventId(Mockito.any(), Mockito.any())).thenReturn("my_event_id");

        commitInformation = mock(CommitInformation.class);
        linkGenerator = new LinkGenerator(pluginConfiguration, commitInformation);

    }

    @Test
    public void generateChangeForScs() {
        when(EventStorageFactory.getEventStorage(pluginConfiguration,
                EventType.SCC_EVENT)).thenReturn(eventStorage);

        linkGenerator.addScsChange("changeId");
        final ArrayList<Link> links = linkGenerator.generateLinks();

        assertOnlyLinkIs(links, "CHANGE");
    }

    @Test
    public void generatePreviousVersionForScs() {
        when(EventStorageFactory.getEventStorage(pluginConfiguration,
                EventType.SCS_EVENT)).thenReturn(eventStorage);
        when(commitInformation.getParentsSHAs("commit-id", PROJECT_NAME)).thenReturn(
                Arrays.asList("sha1", "sha2"));

        linkGenerator.addScsPreviousVersion("commit-id");
        final ArrayList<Link> links = linkGenerator.generateLinks();

        assertOnlyLinkIs(links, "PREVIOUS_VERSION");
    }

    @Test
    public void generatePreviousVersionForScc() {
        when(EventStorageFactory.getEventStorage(pluginConfiguration,
                EventType.SCC_EVENT)).thenReturn(eventStorage);

        linkGenerator.addSccPreviousVersion("changeId");
        final ArrayList<Link> links = linkGenerator.generateLinks();

        assertOnlyLinkIs(links, "PREVIOUS_VERSION");
    }

    @Test
    public void generateBaseForScc() {
        when(EventStorageFactory.getEventStorage(pluginConfiguration,
                EventType.SCS_EVENT)).thenReturn(eventStorage);
        when(commitInformation.getParentsSHAs("commit-id", PROJECT_NAME)).thenReturn(
                Arrays.asList("sha1", "sha2"));

        linkGenerator.addSccBase("commit-id");
        final ArrayList<Link> links = linkGenerator.generateLinks();

        assertOnlyLinkIs(links, "BASE");
    }

    @Test
    public void generateBaseAndPreviousForScc() {
        when(EventStorageFactory.getEventStorage(pluginConfiguration,
                EventType.SCC_EVENT)).thenReturn(eventStorage);
        when(EventStorageFactory.getEventStorage(pluginConfiguration,
                EventType.SCS_EVENT)).thenReturn(eventStorage);
        when(commitInformation.getParentsSHAs("commit-id", PROJECT_NAME)).thenReturn(
                Arrays.asList("sha1", "sha2"));

        linkGenerator.addSccBase("commit-id");
        linkGenerator.addSccPreviousVersion("changeId");
        final ArrayList<Link> links = linkGenerator.generateLinks();

        assertEquals("Not correct amount of links", 2, links.size());
        assertEquals("Did not generate the correct link", "BASE", links.get(0).type);
        assertEquals("Did not generate the correct link", "PREVIOUS_VERSION", links.get(1).type);

    }

    private void assertOnlyLinkIs(final ArrayList<Link> links, final String linkType) {
        assertEquals("Not correct amount of links", 1, links.size());
        assertEquals("Did not generate the correct link", linkType, links.get(0).type);
    }

}
