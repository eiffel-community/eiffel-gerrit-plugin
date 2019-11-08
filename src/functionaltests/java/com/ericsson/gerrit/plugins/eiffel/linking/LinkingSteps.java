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
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Format;

import com.ericsson.gerrit.plugins.eiffel.GerritModule;
import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.listeners.AbstractEventListener;
import com.ericsson.gerrit.plugins.eiffel.listeners.ChangeMergedEventListener;
import com.ericsson.gerrit.plugins.eiffel.listeners.PatchsetCreatedEventListener;
import com.google.common.base.Supplier;
import com.google.gerrit.common.EventDispatcher;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.reviewdb.client.Change.Key;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
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
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class LinkingSteps {

    private static final String PROJECT_NAME = "Test-project";
    private static final int PORT = 7777;
    private static final String BASE_URL = "localhost";
    private MockServerClient mockClient;
    private ClientAndServer server;

    private JsonArray linksFromLastEvent;
    private ArrayList<AbstractEventListener> listeners;
    DynamicItem<EventDispatcher> instance;
    private PatchSetCreatedEvent eventToSend;
    private Path tempDirPath;

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


        Path dbPath = tempDirPath.resolve(PROJECT_NAME + ".db");
        Files.delete(dbPath);
        Files.delete(tempDirPath);

        assertThat(String.format("Tempfolder %s is removed", tempDirPath.toString()),
                !Files.exists(tempDirPath), is(true));
    }

    @Given("^a SCS event with id \"([^\"]*)\" was sent on \"([^\"]*)\"$")
    public void aSCSEventWithIdWasSentOn(String id, String branch) throws Throwable {
        eventToSend = buildPatchSetCreatedEvent(PROJECT_NAME, "changeId", branch);
        JSONObject body = buildResponse(id);
        mockClient.when(request().withMethod("POST"))
                  .respond(response().withBody(body.toString()).withStatusCode(200));
        callOnEvent(eventToSend);
        mockClient.verify(request().withPath("/generateAndPublish/"));

    }

    @When("^user \"([^\"]*)\" creates a new change on \"([^\"]*)\"$")
    public void userCreatesANewChangeOn(String user, String branch) throws Throwable {
        eventToSend = buildPatchSetCreatedEvent(PROJECT_NAME, "changeId", branch);
    }

    @Then("^a SCC event with id \"([^\"]*)\" is sent$")
    public void aSCCEventWithIdIsSent(String id) throws Throwable {
        JSONObject body = buildResponse(id);
        mockClient.when(request().withMethod("POST"))
                  .respond(response().withBody(body.toString()).withStatusCode(200));

        assertThat(server.isRunning(), is(true));
        assertThat(mockClient.isRunning(), is(true));
        callOnEvent(eventToSend);

        Thread.sleep(1000); //TODO: get the threadpool excutor and wait for the sending
        mockClient.verify(request().withPath("/generateAndPublish/"));
        String retrieveRecordedRequests = mockClient.retrieveRecordedRequests(null,
                Format.JSON);
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
         *      "string": "{\"eventParams\":{\"data\":{\"author\":{},...,\"links\":[]}...",
         *      "contentType": "application/json"
         *    }
         *  }
         *]
         * </pre>
         */
        System.out.println("retrieveRecordedRequests: " + retrieveRecordedRequests);
        JsonArray requests = new JsonParser().parse(retrieveRecordedRequests).getAsJsonArray();
        assertThat(requests.size(), is(not(0)));

        JsonObject request = requests.get(0).getAsJsonObject();
        JsonObject requestBody = request.get("body").getAsJsonObject();
        String requestString = requestBody.get("string").getAsString();
        JsonObject parsedBody = new JsonParser().parse(requestString).getAsJsonObject();
        JsonObject eventParams = parsedBody.get("eventParams").getAsJsonObject();
        linksFromLastEvent = eventParams.get("links").getAsJsonArray();

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

    @And("^BASE links to event \"([^\"]*)\"$")
    public void baseLinksToEvent(String id) throws Throwable {
        JsonObject link = new JsonObject();
        for (JsonElement jsonElement : linksFromLastEvent) {
            link = jsonElement.getAsJsonObject();
            if (link.has("BASE")) {
                break;
            }
        }

        assertThat(link.get("BASE"), is(equalTo(id)));
    }

    @When("^user \"([^\"]*)\" submits the change to \"([^\"]*)\"$")
    public void userSubmitsTheChangeTo(String arg1, String arg2) throws Throwable {
    }

    @Then("^a SCS event with id \"([^\"]*)\" is sent$")
    public void aSCSEventWithIdIsSent(String arg1) throws Throwable {
    }

    @And("^CHANGE links to event \"([^\"]*)\"$")
    public void changeLinksToEvent(String arg1) throws Throwable {
    }

    @And("^PREVIOUS_VERSION links to event \"([^\"]*)\"$")
    public void previous_versionLinksToEvent(String arg1) throws Throwable {
    }

    @Given("^no SCS event was sent on \"([^\"]*)\"$")
    public void noSCSEventWasSentOn(String arg1) throws Throwable {
//        setup();
    }

    @And("^no BASE link set$")
    public void noBASELinkSet() throws Throwable {
    }

    @When("^user \"([^\"]*)\" uploads a new patchset$")
    public void userUploadsANewPatchset(String arg1) throws Throwable {
    }

//    @And("^BASE links to event \"([^\"]*)\"$")
//    public void BASE_links_to_event(String id) throws Throwable {
//    }

    private PatchSetCreatedEvent buildPatchSetCreatedEvent(String projectName, String changeId,
            String branch) {
        ChangeAttribute changeAttribute = new ChangeAttribute();
        changeAttribute.project = projectName;
        changeAttribute.branch = branch;
        @SuppressWarnings("unchecked")
        Supplier<ChangeAttribute> changeAttributeSupplier = mock(Supplier.class);
        when(changeAttributeSupplier.get()).thenReturn(changeAttribute);

        PatchSetAttribute patchSetAttribute = mock(PatchSetAttribute.class);
        patchSetAttribute.author = new AccountAttribute();
        Supplier<PatchSetAttribute> patchSetAttributeSupplier = mock(Supplier.class);
        when(patchSetAttributeSupplier.get()).thenReturn(patchSetAttribute);

        Key changeKey = mock(Key.class);
        when(changeKey.toString()).thenReturn(changeId);

        PatchSetCreatedEvent patchSetCreatedEvent = mock(PatchSetCreatedEvent.class);
        patchSetCreatedEvent.change = changeAttributeSupplier;
        patchSetCreatedEvent.patchSet = patchSetAttributeSupplier;
        patchSetCreatedEvent.changeKey = changeKey;

        when(patchSetCreatedEvent.getProjectNameKey()).thenReturn(mock(NameKey.class));
        return patchSetCreatedEvent;
    }

    private void callOnEvent(PatchSetCreatedEvent patchSetCreatedEvent) {
        for (AbstractEventListener abstractEventListener : listeners) {
            abstractEventListener.onEvent(patchSetCreatedEvent);
        }
    }

    private class ModuleDependencis extends AbstractModule {

        private final String PLUGIN_NAME = "plugin";
        private final String[] FILTER = null;
        private final String[] FLOW_CONTEXT = null;
        private final boolean ENABLED_TRUE = true;
        private final boolean ENABLED_FALSE = false;
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
//                when(pluginConfigFactory.getFromProjectConfig(Mockito.any(NameKey.class),
//                        Mockito.any(String.class))).thenThrow(new RuntimeException("wweeee"));
            } catch (NoSuchProjectException e) {
                throw new RuntimeException(e);
            }
            bind(PluginConfigFactory.class).toInstance(pluginConfigFactory);
            bind(String.class).annotatedWith(CanonicalWebUrl.class).toInstance("web-url");

//            bind(ProjectCache.class).toInstance(mock(ProjectCache.class));
//            bind(ProjectState.Factory.class).toInstance(mock(ProjectState.Factory.class));
//            bind(SecureStore.class).toInstance(mock(SecureStore.class));
//            bind(Config.class).annotatedWith(GerritServerConfig.class)
//                              .toInstance(mock(Config.class));
//            bind(Path.class).annotatedWith(SitePath.class).toInstance(mock(Path.class));
//            bind(SitePaths.class).toInstance(mock(SitePaths.class));

        }

    }
}
