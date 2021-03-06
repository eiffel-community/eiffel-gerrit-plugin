package com.ericsson.gerrit.plugins.eiffel.integrationtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.eiffelcommons.utils.HttpExecutor;
import com.ericsson.eiffelcommons.utils.HttpRequest;
import com.ericsson.eiffelcommons.utils.HttpRequest.HttpMethod;
import com.ericsson.eiffelcommons.utils.ResponseEntity;
import com.ericsson.gerrit.plugins.eiffel.exceptions.RegexMatchFailedException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class ServiceIntegrationSteps {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceIntegrationSteps.class);

    private final static String RABBITMQ_HOSTNAME = "localhost";
    private final static int RABBITMQ_PORT = 5672;
    private final static String RABBITMQ_USERNAME = "myuser";
    private final static String RABBITMQ_PASSWORD = "myuser";
    private final static String RABBITMQ_QUEUENAME = "gerrit-test-queue";
    private final static String RABBITMQ_EXCHANGE_NAME = "eiffel-exchange";
    private final static String GERRIT_BASE_URL = "http://localhost:8080";

    private String projectName;
    private String gerritAccountCookie;
    private String gerritXAuthToken;

    private String requestBodyString;
    private String endPoint;
    private HttpMethod httpMethod;

    private List<String> messages;

    private String uniqueChangeId;

    @Before
    public void setUp() throws Exception {
        declareQueues();
        cleanUp();
        generateGerritAccountCookie();
        generateGerritXAuthToken();
    }

    @Given("^a project is created$")
    public void aProjectIsCreated()
            throws ClientProtocolException, URISyntaxException, IOException {
        this.projectName = UUID.randomUUID().toString();
        final ResponseEntity response = createProject(projectName);

        final int expected = 201;
        final int actual = response.getStatusCode();
        assertEquals("Failed to create project", expected, actual);
    }

    @And("^the project is configured to send eiffel events and submit type is \"([^\"]*)\" and publish url \"([^\"]*)\"$")
    public void theProjectIsConfiguredToSendEiffelEventsWithPublishUrl(final String submitType,
            final String publishUrl)
            throws ClientProtocolException, URISyntaxException, IOException {
        final ResponseEntity response = updateProjectConfig(submitType, publishUrl);
        final JsonObject responseBody = new JsonParser().parse(response.getBody()).getAsJsonObject();
        final String actualSubmitTtype = responseBody.get("submit_type").getAsString();
        assertEquals("Could not set the submit type", submitType, actualSubmitTtype);

        final int expected = 200;
        final int actual = response.getStatusCode();
        assertEquals("Failed to configure project", expected, actual);
    }

    @When("^a change is created$")
    public void aChangeIsCreated()
            throws ClientProtocolException, URISyntaxException, IOException {
        final ResponseEntity response = createChange();
        final int expected = 201;
        final int actual = response.getStatusCode();

        assertEquals("Change is created", expected, actual);
        final JsonParser jsonParser = new JsonParser();
        final JsonObject body = jsonParser.parse(response.getBody()).getAsJsonObject();
        uniqueChangeId = body.get("id").getAsString();
        assertNotEquals("Did not get a change id", "", uniqueChangeId);

    }

    @When("^a change is submitted$")
    public void aChangeIsSubmitted() throws Throwable {
        final int expected = 200;

        ResponseEntity response = reviewChange();
        int actual = response.getStatusCode();
        assertEquals("Fail to review", expected, actual);

        response = submitChange();
        actual = response.getStatusCode();
        assertEquals("Failed to submit", expected, actual);

    }

    private ResponseEntity reviewChange()
            throws URISyntaxException, ClientProtocolException, IOException {
        requestBodyString = "{\"labels\":{\"Code-Review\":2}}";
        httpMethod = HttpMethod.POST;
        endPoint = "/changes/" + uniqueChangeId + "/revisions/current/review";

        final ResponseEntity response = setHeaderAndPerformRequest(endPoint, requestBodyString,
                httpMethod);
        return response;
    }

    @Then("^I should find an eiffel event in rabbitmq$")
    public void iShouldFindAnEiffelEventInRabbitmq() throws IOException, TimeoutException {
        final int minMessageCount = 1;
        final long responseWaitTimeoutInMillis = 10000;
        messages = consumeMessages(minMessageCount, responseWaitTimeoutInMillis);
        assertEquals("Expecting one message in queue but found" + messages.size(), 1,
                messages.size());
    }

    @And("\"([^\"]*)\" links are found in the event")
    public void linksAreFoundInTheEvent(final int numberOfLinks) {
        assertEquals("Found more/less messages than expected ", 1, messages.size());
        final String firstMessage = messages.get(0);
        final JsonArray links = parseLinksFor(firstMessage);

        assertEquals("Number of links did not match", numberOfLinks, links.size());
    }

    /**
     * Generates a gerrit XAuth token. Since we are not using HTTP password for the REST API we need
     * to simulate what the browser is doing when signing in to gerrit. Gerrit XAuth token is one of
     * the needed cookies that needs to be set in the http header to access the REST api without
     * using HTTP password.
     *
     * @throws Exception
     */
    private void generateGerritXAuthToken() throws Exception {
        try {
            final HttpRequest request = new HttpRequest(HttpMethod.GET);
            ResponseEntity response;
            response = request.setBaseUrl(GERRIT_BASE_URL)
                              .setHeader("Cookie", gerritAccountCookie)
                              .setHeader("Referer", GERRIT_BASE_URL + "/login")
                              .setEndpoint("/")
                              .performRequest();

            final Header[] headers = response.getHeaders();
            final Header gerritAccountHeader = headers[1];

            final String gerritXAuthHeader = gerritAccountHeader.getValue();
            final String regexp = "XSRF_TOKEN=(.+);";
            this.gerritXAuthToken = getFirstMatchGroupFromText(gerritXAuthHeader, regexp);
        } catch (final URISyntaxException e) {
            final String stacktrace = ExceptionUtils.getStackTrace(e);
            throw new URISyntaxException("Failed to generate gerritXAuthToken", stacktrace);
        } catch (final IOException e) {
            throw new IOException("Failed to generate gerritXAuthToken", e);
        }
    }

    /**
     * Generates a gerrit account token. Since we are not using HTTP password for the REST API we
     * need to simulate what the browser is doing when signing in to gerrit. Gerrit account token is
     * one of the needed cookies that needs to be set in the http header to access the REST api
     * without using HTTP password.
     *
     * @throws Exception
     */
    private void generateGerritAccountCookie() throws Exception {
        try {
            final CloseableHttpClient httpClientNoRedirect = HttpClientBuilder.create()
                                                                        .disableRedirectHandling()
                                                                        .build();
            final HttpExecutor httpExecutor = new HttpExecutor(httpClientNoRedirect);
            final HttpRequest requestLogin = new HttpRequest(HttpMethod.GET, httpExecutor);

            // Account ID 1000000 is the default value for the admin account in gerrit.
            final ResponseEntity response = requestLogin.setBaseUrl(GERRIT_BASE_URL)
                                                  .setEndpoint("/login/?account_id=1000000")
                                                  .addParameter("http.protocol.handle-redirects",
                                                          "false")
                                                  .performRequest();

            final Header[] headers = response.getHeaders();
            final Header gerritAccountHeader = headers[4];
            this.gerritAccountCookie = gerritAccountHeader.getValue();
        } catch (final URISyntaxException e) {
            final String stacktrace = ExceptionUtils.getStackTrace(e);
            throw new URISyntaxException("Failed to generate Gerrit account cookie", stacktrace);
        } catch (final IOException e) {
            throw new IOException("Failed to generate Gerrit account cokkie", e);
        }
    }

    private String getFirstMatchGroupFromText(final String text, final String regexp) throws Exception {
        final Pattern pattern = Pattern.compile(regexp);
        final Matcher matcher = pattern.matcher(text);

        matcher.find();
        final String matchedGroup = matcher.group(1);

        if (matchedGroup == null) {
            throw new RegexMatchFailedException(
                    "Failed to parse the text: \"" + text + "\" with regexp: \"" + regexp + "\"");
        }

        return matchedGroup;
    }

    private ResponseEntity createProject(final String projectName)
            throws URISyntaxException, ClientProtocolException, IOException {
        final JsonObject generateBody = new JsonObject();
        generateBody.addProperty("description", "Auto generated project");
        generateBody.addProperty("submit_type", "INHERIT");
        generateBody.addProperty("create_empty_commit", true);

        requestBodyString = generateBody.toString();
        endPoint = "/projects/" + projectName;
        httpMethod = HttpMethod.PUT;

        final ResponseEntity response = setHeaderAndPerformRequest(endPoint, requestBodyString,
                httpMethod);

        return response;
    }

    private ResponseEntity updateProjectConfig(final String submitType, final String publishUrl)
            throws URISyntaxException, ClientProtocolException, IOException {
        final String jsonBodyString = "{\"description\":\"Auto generated project\","
                + "\"use_contributor_agreements\":\"INHERIT\",\"use_content_merge\":\"INHERIT\","
                + "\"use_signed_off_by\":\"INHERIT\",\"require_change_id\":\"INHERIT\","
                + "\"create_new_change_for_all_not_in_target\":\"INHERIT\","
                + "\"reject_implicit_merges\":\"INHERIT\",\"submit_type\":\"" + submitType + "\","
                + "\"state\":\"ACTIVE\",\"plugin_config_values\":{\"Eiffel-Integration\":"
                + "{\"enabled\":{\"value\":\"true\"},\"filter\":{},\"flow-context\":{},"
                + "\"remrem-password\":{},\"remrem-publish-url\":{\"value\":\"" + publishUrl
                + "\"},"
                + "\"remrem-username\":{}},\"uploadvalidator\":{\"binaryTypes\":{\"values\":[]},"
                + "\"blockedContentType\":{\"values\":[]},\"blockedContentTypeWhitelist\":"
                + "{\"value\":\"false\"},\"blockedFileExtension\":{\"values\":[]},"
                + "\"blockedKeywordPattern\":{\"values\":[]},\"invalidFilenamePattern\":"
                + "{\"values\":[]},\"maxPathLength\":{\"value\":\"0\"},\"project\":"
                + "{\"values\":[]},\"ref\":{\"values\":[]},\"rejectDuplicatePathnames\":"
                + "{\"value\":\"false\"},\"rejectDuplicatePathnamesLocale\":{\"value\":\"en\"},"
                + "\"rejectSubmodule\":{\"value\":\"false\"},\"rejectSymlink\":"
                + "{\"value\":\"false\"},\"rejectWindowsLineEndings\":{\"value\":\"false\"},"
                + "\"requiredFooter\":{\"values\":[]}}}}";

        requestBodyString = jsonBodyString;
        endPoint = "/projects/" + projectName + "/config";
        httpMethod = HttpMethod.PUT;

        final ResponseEntity response = setHeaderAndPerformRequest(endPoint, requestBodyString,
                httpMethod);

        return response;
    }

    private ResponseEntity createChange()
            throws URISyntaxException, ClientProtocolException, IOException {
        final String jsonBodyString = "{\"project\":\"" + projectName + "\" ,"
                + "\"subject\" : \"Let's support 100% Gerrit workflow direct in browser "
                + UUID.randomUUID() + "\", "
                + "\"topic\" : \"create-change-in-browser\", " + "\"status\" : \"NEW\", "
                + "\"branch\":\"master\"}";

        requestBodyString = jsonBodyString;
        endPoint = "/changes/";
        httpMethod = HttpMethod.POST;

        final ResponseEntity response = setHeaderAndPerformRequest(endPoint, requestBodyString,
                httpMethod);

        return response;
    }

    private ResponseEntity submitChange()
            throws ClientProtocolException, URISyntaxException, IOException {
        requestBodyString = "{}";
        endPoint = "/changes/" + uniqueChangeId + "/submit";
        httpMethod = HttpMethod.POST;

        final ResponseEntity response = setHeaderAndPerformRequest(endPoint, requestBodyString,
                httpMethod);

        return response;
    }

    protected List<String> consumeMessages(final int messageCount, final long timeout)
            throws IOException, TimeoutException {
        final List<String> messages = new ArrayList<>();
        final ConnectionFactory factory = createAndSetConnectionFactory();
        final Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        final long stopTime = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() < stopTime) {
            try {
                final GetResponse response = channel.basicGet(RABBITMQ_QUEUENAME, true);
                if (response != null) {
                    messages.add(new String(response.getBody(), "UTF-8"));
                }

                if (messages.size() == messageCount) {
                    return messages;
                }
            } catch (final Exception e) {
                LOGGER.error("RabbitMQ failed to get from queue", e);
            }
        }

        return messages;
    }

    protected void declareQueues() throws IOException, TimeoutException {
        final ConnectionFactory factory = createAndSetConnectionFactory();
        final Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        final AMQP.Exchange.DeclareOk exchangeOK = channel.exchangeDeclare(RABBITMQ_EXCHANGE_NAME,
                "topic", true);
        final AMQP.Queue.DeclareOk queueOK = channel.queueDeclare(RABBITMQ_QUEUENAME, true, false,
                false, null);
        final AMQP.Queue.BindOk bindOK = channel.queueBind(RABBITMQ_QUEUENAME,
                RABBITMQ_EXCHANGE_NAME, "#");
        assertEquals("Expecting non-null  but RabbitMQ exchange is " + exchangeOK, true,
                exchangeOK != null);
        assertEquals("Expecting non-null but RabbitMQ queue is " + queueOK, true, queueOK != null);
        assertEquals("Expecting non-null but RabbitMQ binding key is " + bindOK, true,
                bindOK != null);
    }

    private ResponseEntity setHeaderAndPerformRequest(final String endPoint, final String setBodyString,
            final HttpMethod httpMethod)
            throws URISyntaxException, ClientProtocolException, IOException {
        final HttpRequest request = new HttpRequest(httpMethod);
        ResponseEntity response;
        response = request.setHeader("Cookie", gerritAccountCookie)
                          .setHeader("Content-Type", "application/json; charset=UTF-8")
                          .setHeader("Accept", "application/json")
                          .setHeader("X-Gerrit-Auth", gerritXAuthToken)
                          .setBaseUrl(GERRIT_BASE_URL)
                          .setEndpoint(endPoint)
                          .setBody(setBodyString)
                          .performRequest();

        return response;
    }

    private ConnectionFactory createAndSetConnectionFactory() {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOSTNAME);
        factory.setPort(RABBITMQ_PORT);
        factory.setUsername(RABBITMQ_USERNAME);
        factory.setPassword(RABBITMQ_PASSWORD);
        return factory;
    }

    private JsonArray parseLinksFor(final String responseBody) {
        /**
         * Recorded Request looks like this
         *
         * <pre>
            {
              "meta": {
                "id": "8b4de93d-53ca-4e21-81fd-823b634b461d",
                "type": "EiffelSourceChangeSubmittedEvent",
                "version": "3.0.0",
                "time": 1574685135261,
                "tags": [],
                "source": {
                  "host": "d70d29e72a58",
                  "name": "Eiffel Gerrit Plugin",
                  "serializer": "pkg:maven/com.github.eiffel-community/eiffel-remrem-semantics@2.0.5",
                  "uri": "http://localhost:8080/25"
                }
              },
              "data": {
                "submitter": {
                  "name": "Administrator",
                  "email": "admin@example.com",
                  "id": "admin"
                },
                "gitIdentifier": {
                  "commitId": "ab5705d6151b133cb20dd7c6df78ae19b5522191",
                  "branch": "master",
                  "repoName": "6489be07-836f-4437-be2f-0e06f483a53a",
                  "repoUri": "ssh://localhost:29418/"
                },
                "customData": []
              },
              "links": [
                {
                  "type": "CHANGE",
                  "target": "bbf400b0-4cbb-4325-bf9a-5b7c5a2c9ab1"
                },
                {
                  "type": "PREVIOUS_VERSION",
                  "target": "8d7a6361-e504-47c9-ad14-2bf613b3a9e0"
                }
              ]
            }
         * </pre>
         */
        final JsonObject reponse = new JsonParser().parse(responseBody).getAsJsonObject();
        return reponse.get("links").getAsJsonArray();

    }

    @After
    public void cleanUp() throws Exception {

        consumeMessages(100, 2000);
    }
}
