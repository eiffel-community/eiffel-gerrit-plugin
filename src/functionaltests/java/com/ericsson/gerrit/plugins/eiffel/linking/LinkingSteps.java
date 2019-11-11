package com.ericsson.gerrit.plugins.eiffel.linking;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Format;
import org.mockserver.model.HttpRequest;

import com.ericsson.gerrit.plugins.eiffel.GerritModule;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.listeners.AbstractEventListener;
import com.ericsson.gerrit.plugins.eiffel.listeners.ChangeMergedEventListener;
import com.ericsson.gerrit.plugins.eiffel.listeners.PatchsetCreatedEventListener;
import com.google.common.base.Supplier;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Change.Key;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.PatchSetEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class LinkingSteps {

    private static final String PROJECT_NAME = "Test-project";
    private static final int PORT = 7777;
    private static final String BASE_URL = "localhost";
    private static final HttpRequest ALL_REQUESTS = null;
    private MockServerClient mockClient;
    private ClientAndServer server;

    private JsonArray linksFromLastEvent;
    private ArrayList<AbstractEventListener> listeners;
    private PatchSetEvent eventToSend;
    private Path tempDirPath;
    private Map<String, String> eventTypes = getEventMap();
    private Map<String, String> changeIdLookup = new HashMap<String, String>();

    @Before()
    public void setup() throws IOException, InterruptedException {

        /*
         * I would like to use org.junit.rules.TemporaryFolder but can't get the @Rule to trigger
         * with cucumber
         */
        tempDirPath = Files.createTempDirectory("pluginFetureTest");
        File tempDirLocation = tempDirPath.toFile();

        Injector injector = Guice.createInjector(new ModuleDependencis(tempDirLocation),
                new GerritModule());

        listeners = new ArrayList<AbstractEventListener>();
        listeners.add(injector.getInstance(ChangeMergedEventListener.class));
        listeners.add(injector.getInstance(PatchsetCreatedEventListener.class));

        server = ClientAndServer.startClientAndServer(PORT);
        mockClient = new MockServerClient(BASE_URL, PORT);
    }

    @After()
    public void afterScenario() throws IOException, InterruptedException {
        server.stop();
        mockClient.close();

        changeIdLookup.clear();

        Path dbPath = tempDirPath.resolve(PROJECT_NAME + ".db");
        Files.delete(dbPath);
        Files.delete(tempDirPath);
        assertThat(String.format("Tempfolder %s is removed", tempDirPath.toString()),
                !Files.exists(tempDirPath), is(true));
    }

    @Given("^a SCS event with id \"([^\"]*)\" was sent on \"([^\"]*)\"$")
    public void aSCSEventWithIdWasSentOn(String id, String branch) throws Throwable {
        eventToSend = buildChangeMergedEvent(PROJECT_NAME, "noUserchangeId", branch);
        prepareResponse(id);
        callOnEvent(eventToSend);
        Thread.sleep(1000); // TODO: get the threadpool excutor and wait for the sending
        mockClient.verify(request().withPath("/generateAndPublish/"));

    }

    @Given("^no SCS event was sent on \"([^\"]*)\"$")
    public void noSCSEventWasSentOn(String branch) throws Throwable {
        server.verifyZeroInteractions();
        mockClient.verifyZeroInteractions();
    }

    @When("^user \"([^\"]*)\" creates a new change on \"([^\"]*)\"$")
    public void userCreatesANewChangeOn(String user, String branch) throws Throwable {
        clearTestStates();
        String changeId = getChangeIdForUser(user);
        eventToSend = buildPatchSetCreatedEvent(PROJECT_NAME, changeId, branch);
    }

    @When("^user \"([^\"]*)\" submits the change to \"([^\"]*)\"$")
    public void userSubmitsTheChangeTo(String user, String branch) throws Throwable {
        clearTestStates();
        String changeId = getChangeIdForUser(user);
        eventToSend = buildChangeMergedEvent(PROJECT_NAME, changeId, branch);
    }

    @When("^user \"([^\"]*)\" uploads a new patchset to \"([^\"]*)\"$$")
    public void userUploadsANewPatchset(String user, String branch) throws Throwable {
        clearTestStates();
        String changeId = getChangeIdForUser(user);
        eventToSend = buildPatchSetCreatedEvent(PROJECT_NAME, changeId, branch);
    }

    @Then("^a \"([^\"]*)\" event with id \"([^\"]*)\" is sent$")
    public void aEventWithIdIsSent(String eventType, String id) throws Throwable {
        prepareResponse(id);
        callOnEvent(eventToSend);
        Thread.sleep(1000); // TODO: get the threadpool excutor and wait for the sending
        linksFromLastEvent = getLinksFromRequest(eventType);
    }

    @Then("^BASE links to event \"([^\"]*)\"$")
    public void baseLinksToEvent(String id) throws Throwable {
        assertLinksHasTypeWithId("BASE", id, linksFromLastEvent);
    }

    @Then("^CHANGE links to event \"([^\"]*)\"$")
    public void changeLinksToEvent(String id) throws Throwable {
        assertLinksHasTypeWithId("CHANGE", id, linksFromLastEvent);
    }

    @Then("^PREVIOUS_VERSION links to event \"([^\"]*)\"$")
    public void previous_versionLinksToEvent(String id) throws Throwable {
        assertLinksHasTypeWithId("PREVIOUS_VERSION", id, linksFromLastEvent);
    }

    @Then("^no BASE link set$")
    public void noBASELinkSet() throws Throwable {
        JsonObject link = getLink("BASE", linksFromLastEvent);
        assertThat("No BASE link found", link.size(), is(0));
    }

    private PatchSetCreatedEvent buildPatchSetCreatedEvent(String projectName, String changeId,
            String branch) {
        Supplier<ChangeAttribute> changeAttributeSupplier = getChangeAttribute(projectName, branch);
        Supplier<PatchSetAttribute> patchSetAttributeSupplier = getPatSetAttribute();
        Key changeKey = getChangeKey(changeId);
    
        PatchSetCreatedEvent patchSetCreatedEvent = mock(PatchSetCreatedEvent.class);
        patchSetCreatedEvent.change = changeAttributeSupplier;
        patchSetCreatedEvent.patchSet = patchSetAttributeSupplier;
        patchSetCreatedEvent.changeKey = changeKey;
    
        when(patchSetCreatedEvent.getProjectNameKey()).thenReturn(mock(NameKey.class));
        return patchSetCreatedEvent;
    }

    private ChangeMergedEvent buildChangeMergedEvent(String projectName, String changeId,
            String branch) {
        Supplier<ChangeAttribute> changeAttributeSupplier = getChangeAttribute(projectName, branch);
        Supplier<PatchSetAttribute> patchSetAttributeSupplier = getPatSetAttribute();
        Key changeKey = getChangeKey(changeId);
    
        ChangeMergedEvent changeMergeEvent = mock(ChangeMergedEvent.class);
        changeMergeEvent.change = changeAttributeSupplier;
        changeMergeEvent.patchSet = patchSetAttributeSupplier;
        changeMergeEvent.changeKey = changeKey;
    
        when(changeMergeEvent.getProjectNameKey()).thenReturn(mock(NameKey.class));
        return changeMergeEvent;
    }

    private JSONObject buildResponse(String id) {
        JSONObject event = new JSONObject();
        event.put("id", id);
        JSONArray events = new JSONArray();
        events.put(event);
        JSONObject body = new JSONObject();
        body.put("events", events);
        return body;
    }

    private void assertLinksHasTypeWithId(String type, String id, JsonArray links) {
        JsonObject link = getLink(type, links);

        assertThat(String.format("The event has a %s link", type), link.has("target"), is(true));
        assertThat("We are linking to correct event", link.get("target").getAsString(), is(equalTo(id)));
    }

    private JsonObject getLink(String type, JsonArray links) {
        /*
         * Links look like this: [{\"type\":\"BASE\",\"target\":\"SCS1\"}]
         */
        JsonObject link = new JsonObject();
        for (JsonElement jsonElement : links) {
            JsonObject possibleLink = jsonElement.getAsJsonObject();
            if (possibleLink.get("type").getAsString().equals(type)) {
                link = possibleLink;
                break;
            }
        }
        return link;
    }

    private void clearTestStates() {
        eventToSend = null;
        linksFromLastEvent = new JsonArray();
    }

    private JsonArray getLinksFromRequest(String eventTypeShort)
            throws InterruptedException, AssertionError {
        mockClient.verify(request().withPath("/generateAndPublish/"));
        String retrieveRecordedRequests = mockClient.retrieveRecordedRequests(ALL_REQUESTS,
                Format.JSON);
        return parseLinksFor(retrieveRecordedRequests, eventTypes.get(eventTypeShort));
    }

    private void prepareResponse(String id) {
        JSONObject body = buildResponse(id);
        mockClient.clear(ALL_REQUESTS);
        mockClient.when(request().withMethod("POST"))
                  .respond(response().withBody(body.toString()).withStatusCode(200));
    }

    private String getChangeIdForUser(String user) {
        if (changeIdLookup.containsKey(user)) {
            return changeIdLookup.get(user);
        } else {
            String changeId = "change-id-" + changeIdLookup.size();
            changeIdLookup.put(user, changeId);
            return changeId;
        }
    }

    private Key getChangeKey(String changeId) {
        Key changeKey = mock(Key.class);
        when(changeKey.toString()).thenReturn(changeId);
        return changeKey;
    }

    private Supplier<PatchSetAttribute> getPatSetAttribute() {
        PatchSetAttribute patchSetAttribute = mock(PatchSetAttribute.class);
        patchSetAttribute.author = new AccountAttribute();
        @SuppressWarnings("unchecked")
        Supplier<PatchSetAttribute> patchSetAttributeSupplier = mock(Supplier.class);
        when(patchSetAttributeSupplier.get()).thenReturn(patchSetAttribute);
        return patchSetAttributeSupplier;
    }

    private Supplier<ChangeAttribute> getChangeAttribute(String projectName, String branch) {
        ChangeAttribute changeAttribute = new ChangeAttribute();
        changeAttribute.project = projectName;
        changeAttribute.branch = branch;
        @SuppressWarnings("unchecked")
        Supplier<ChangeAttribute> changeAttributeSupplier = mock(Supplier.class);
        when(changeAttributeSupplier.get()).thenReturn(changeAttribute);
        return changeAttributeSupplier;
    }

    private void callOnEvent(PatchSetEvent patchSetEvent) {
        for (AbstractEventListener abstractEventListener : listeners) {
            abstractEventListener.onEvent(patchSetEvent);
        }
    }

    private JsonArray parseLinksFor(String retrieveRecordedRequests, String eventType) {
        /**
         * Recorded Request looks like this
         * 
         * <pre>
         * [
         *  {
         *    "method": "POST",
         *    "path": "/generateAndPublish/",
         *    "queryStringParameters": {... },
         *    "headers": { ... },
         *    "keepAlive": true,
         *    "secure": false,
         *    "body": {
         *      "type": "STRING",
         *      "string": "{\"eventParams\":{\"data\":{\"author\":{},...,\"links\":[]},\"msgParams\":{\"meta\":{\"type\":\"EiffelSourceChangeCreatedEvent\"...",
         *      "contentType": "application/json"
         *    }
         *  }
         *]
         * </pre>
         */
        JsonArray requests = new JsonParser().parse(retrieveRecordedRequests).getAsJsonArray();
        assertThat(requests.size(), is(not(0)));

        JsonObject request = requests.get(0).getAsJsonObject();
        JsonObject requestBody = request.get("body").getAsJsonObject();
        String requestString = requestBody.get("string").getAsString();
        JsonObject parsedBody = new JsonParser().parse(requestString).getAsJsonObject();

        JsonObject msgParams = parsedBody.get("msgParams").getAsJsonObject();
        JsonObject meta = msgParams.get("meta").getAsJsonObject();
        String type = meta.get("type").getAsString();
        assertThat("We got the expected message type", type, is(equalTo(eventType)));

        JsonObject eventParams = parsedBody.get("eventParams").getAsJsonObject();
        return eventParams.get("links").getAsJsonArray();
    }

    private static Map<String, String> getEventMap() {
        Map<String, String> myMap = new HashMap<String, String>();
        myMap.put("SCC", "EiffelSourceChangeCreatedEvent");
        myMap.put("SCS", "EiffelSourceChangeSubmittedEvent");
        return myMap;
    }

    private class ModuleDependencis extends AbstractModule {

        private final String[] FILTER = null;
        private final String[] FLOW_CONTEXT = null;
        private final boolean ENABLED_TRUE = true;
        private final String REMREM_PUBLISH_URL = "http://" + BASE_URL + ":" + PORT;
        private final String REMREM_USERNAME = "dummyUser";
        private final String REMREM_PASSWORD = "dummypassword";
        private File file;

        public ModuleDependencis(File file) {
            this.file = file;
        }

        @Override
        protected void configure() {
            String pluginName = "Eiffel-Gerrit-Plugin";

            bind(String.class).annotatedWith(PluginName.class).toInstance(pluginName);
            bind(File.class).annotatedWith(PluginData.class).toInstance(file);

            PluginConfigFactory pluginConfigFactory = mock(PluginConfigFactory.class);
            PluginConfig pluginConfig = mock(PluginConfig.class);

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

            try {
                when(pluginConfigFactory.getFromProjectConfig(Mockito.any(NameKey.class),
                        Mockito.any(String.class))).thenReturn(pluginConfig);
            } catch (NoSuchProjectException e) {
                throw new RuntimeException(e);
            }
            bind(PluginConfigFactory.class).toInstance(pluginConfigFactory);
            bind(String.class).annotatedWith(CanonicalWebUrl.class).toInstance("web-url");

        }

    }
}
