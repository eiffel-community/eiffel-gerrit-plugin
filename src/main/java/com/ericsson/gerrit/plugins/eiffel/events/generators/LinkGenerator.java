package com.ericsson.gerrit.plugins.eiffel.events.generators;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EventType;
import com.ericsson.gerrit.plugins.eiffel.events.models.Link;
import com.ericsson.gerrit.plugins.eiffel.exceptions.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.git.CommitInformation;
import com.ericsson.gerrit.plugins.eiffel.storage.EventStorage;
import com.ericsson.gerrit.plugins.eiffel.storage.EventStorageFactory;

public class LinkGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkGenerator.class);
    private final EiffelPluginConfiguration pluginConfig;

    private static final String LINK_TYPE_PREVIOUS_VERSION = "PREVIOUS_VERSION";
    private static final String LINK_TYPE_BASE = "BASE";
    private static final String LINK_TYPE_CHANGE = "CHANGE";
    private final ArrayList<Link> links;
    private final CommitInformation commitInformation;

    public LinkGenerator(final EiffelPluginConfiguration pluginConfig,
            final CommitInformation commitInformation) {
        this.pluginConfig = pluginConfig;
        this.commitInformation = commitInformation;
        links = new ArrayList<>();
    }

    public void addSccPreviousVersion(final String changeId) {
        final String previousEvent = getPreviousEiffelEventId(EventType.SCC_EVENT, changeId);

        addIfNotEmpty(LINK_TYPE_PREVIOUS_VERSION, previousEvent);
    }

    public void addScsPreviousVersion(final String commitId) {
        final List<String> parentsSHAs = commitInformation.getParentsSHAs(commitId, pluginConfig.getProject());
        final String previousEvent = getPreviousEiffelEventId(EventType.SCS_EVENT, parentsSHAs);

        addIfNotEmpty(LINK_TYPE_PREVIOUS_VERSION, previousEvent);
    }

    public void addSccBase(final String commitId) {
        final List<String> parentsSHAs = commitInformation.getParentsSHAs(commitId, pluginConfig.getProject());
        final String previousEvent = getPreviousEiffelEventId(EventType.SCS_EVENT, parentsSHAs);

        addIfNotEmpty(LINK_TYPE_BASE, previousEvent);
    }

    public void addScsChange(final String changeId) {
        final String previousEvent = getPreviousEiffelEventId(EventType.SCC_EVENT, changeId);

        addIfNotEmpty(LINK_TYPE_CHANGE, previousEvent);
    }

    public ArrayList<Link> generateLinks() {
        return links;
    }

    private void addIfNotEmpty(final String eventType, final String previousEvent) {
        if (!StringUtils.isEmpty(previousEvent)) {
            final Link link = new Link();
            link.type = eventType;
            link.target = previousEvent;
            links.add(link);
        }
    }


    /**
     * Will for a given list of search criteria return the first found event
     */
    private String getPreviousEiffelEventId(final String linkedEiffelEventType,
            final List<String> searchCriterias) {
        for (final String searchCriteria : searchCriterias) {
            final String eiffelEventId = getPreviousEiffelEventId(linkedEiffelEventType,
                    searchCriteria);
            if (!StringUtils.isEmpty(eiffelEventId)) {
                return eiffelEventId;
            }
        }
        return "";
    }
    private String getPreviousEiffelEventId(final String linkedEiffelEventType,
            final String searchCriteria) {
        try {
            final EventStorage eventStorage = EventStorageFactory.getEventStorage(
                    pluginConfig, linkedEiffelEventType);
            return eventStorage.getEventId(pluginConfig.getProject(), searchCriteria);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Could not get previous Eiffel event.", e);
            return "";
        } catch (final NoSuchElementException e) {
            LOGGER.debug("Event Storage did not return any value for this query.", e);
            return "";
        } catch (final Exception e) {
            LOGGER.error("Could not get last submitted eiffel event id.", e);
            return "";
        }
    }

}
