package com.ericsson.gerrit.plugins.eiffel.integrationtest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.ericsson.eiffelcommons.utils.HttpExecutor;
import com.ericsson.eiffelcommons.utils.HttpRequest;
import com.ericsson.eiffelcommons.utils.HttpRequest.HttpMethod;
import com.ericsson.eiffelcommons.utils.ResponseEntity;
import com.google.gson.JsonObject;

import cucumber.api.PendingException;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

public class ServiceIntegrationSteps {
    private String projectName;
    private String gerritAccountCookie;
    private String gerritXAuthToken;
    private String gerritHttpPassword;

    @Before
    public void setUp() throws Exception {
        generateGerritAccountCookie();
        generateGerritXAuthToken();
        //generateGerritHttpPassword();
    }

    @Given("^a project is created$")
    public void a_project_is_created() throws ClientProtocolException, URISyntaxException, IOException {
        this.projectName = UUID.randomUUID().toString();
        ResponseEntity response = createProject(projectName);

        int expected = 201;
        int actual = response.getStatusCode();
        assertEquals("Failed to create project", expected , actual);
    }



    @Given("^the project is configured to send eiffel events with publish url \"([^\"]*)\"$")
    public void the_project_is_configured_to_send_eiffel_events_with_publish_url(String publishUrl) throws ClientProtocolException, URISyntaxException, IOException {
        enableEiffelMessaging(publishUrl);

    }

    @Given("^a change is submitted$")
    public void a_change_is_submitted() {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Then("^I should find an eiffel event in rabbitmq$")
    public void i_should_find_an_eiffel_event_in_rabbitmq() {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    private void generateGerritXAuthToken() throws Exception {
        try {
            HttpRequest request = new HttpRequest(HttpMethod.GET);
            ResponseEntity response;
            response = request
                    .setBaseUrl("http://localhost:8080")
                    .setHeader("Cookie", gerritAccountCookie)
                    .setHeader("Referer", "http://localhost:8080/login")
                    .setEndpoint("/")
                    .performRequest();

            Header[] headers = response.getHeaders();
            Header gerritAccountHeader = headers[1];

            String gerritXAuthHeader = gerritAccountHeader.getValue();
            String regexp = "XSRF_TOKEN=(.+);";
            gerritXAuthToken = getFirstMatchGroupFromText(gerritXAuthHeader, regexp);
        } catch (URISyntaxException | IOException e) {
            throw new Exception("Failed to generate gerritXAuthToken", e);
        }
    }

    private void generateGerritAccountCookie() throws Exception {
        try {
            CloseableHttpClient httpClientNoRedirect = HttpClientBuilder.create().disableRedirectHandling().build();
            HttpExecutor httpExecutor = new HttpExecutor(httpClientNoRedirect);
            HttpRequest requestLogin = new HttpRequest(HttpMethod.GET, httpExecutor);

            ResponseEntity response = //request.setBody(jsonBody.toString())
                    requestLogin.setBaseUrl("http://localhost:8080/")
                    .setEndpoint("/login/?account_id=1000000")
                    .addParameter("http.protocol.handle-redirects", "false")
                    .performRequest();

            Header[] headers = response.getHeaders();
            Header gerritAccountHeader = headers[4];
            gerritAccountCookie = gerritAccountHeader.getValue();
        } catch (Exception e) {
            throw new Exception("Failed to generate gerrit account cookie", e);
        }
    }

    /*private void generateGerritHttpPassword() throws Exception {
        try {
            JsonObject generateBody = new JsonObject();
            generateBody.addProperty("generate", true);

            HttpRequest request = new HttpRequest(HttpMethod.PUT);
            ResponseEntity response;
                response = request
                        .setHeader("Cookie", gerritAccountCookie)
                        .setHeader("Content-Type", "application/json; charset=UTF-8")
                        .setHeader("Accept", "application/json")
                        .setHeader("X-Gerrit-Auth", gerritXAuthToken)
                        .setBaseUrl("http://localhost:8080")
                        .setEndpoint("/accounts/self/password.http")
                        .setBody(generateBody.toString())
                        .performRequest();

            //RegExp that will get the password from the following string.
            //)]}'\r\n"FujbLS5uPhMcfXnGzhaGYxlfIkHRWXf47++Dko7ieQ"
            String regexp = "[\\s\\S]+\"(.+)\"";
            String gerritHttpPassword = getFirstMatchGroupFromText(response.getBody(), regexp);

            this.gerritHttpPassword = gerritHttpPassword;
        } catch (URISyntaxException | IOException e) {
            throw new Exception("Error while generating the gerrit http password", e);
        }

    }*/

    private String getFirstMatchGroupFromText(String text, String regexp) throws Exception {
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(text);

        while(matcher.find()) {
            String matchedGroup = matcher.group(1);

            if (matchedGroup == null) {
                throw new Exception("Failed to parse the text: \"" + text + "\" with regexp: \"" + regexp + "\"");
            }

            return matchedGroup;
        }

        throw new Exception("Could not find any match in the text: \""+ text + "\" with regexp: \"" + regexp + "\"");
    }

    private ResponseEntity createProject(String projectName)
            throws URISyntaxException, ClientProtocolException, IOException {
        JsonObject generateBody = new JsonObject();
        generateBody.addProperty("description", "Auto generated project");

        HttpRequest request = new HttpRequest(HttpMethod.PUT);
        ResponseEntity response;
        response = request
                .setHeader("Cookie", gerritAccountCookie)
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setHeader("Accept", "application/json")
                .setHeader("X-Gerrit-Auth", gerritXAuthToken)
                .setBaseUrl("http://localhost:8080")
                .setEndpoint("/projects/" + projectName)
                .setBody(generateBody.toString())
                .performRequest();

        return response;
    }

    private ResponseEntity enableEiffelMessaging(String publishUrl)
            throws URISyntaxException, ClientProtocolException, IOException {
        String jsonBodyString = "{\"description\":\"Auto generated project\","
                + "\"use_contributor_agreements\":\"INHERIT\",\"use_content_merge\":\"INHERIT\","
                + "\"use_signed_off_by\":\"INHERIT\",\"require_change_id\":\"INHERIT\","
                + "\"create_new_change_for_all_not_in_target\":\"INHERIT\","
                + "\"reject_implicit_merges\":\"INHERIT\",\"submit_type\":\"MERGE_IF_NECESSARY\","
                + "\"state\":\"ACTIVE\",\"plugin_config_values\":{\"Eiffel-Integration\":"
                + "{\"enabled\":{\"value\":\"true\"},\"filter\":{},\"flow-context\":{},"
                + "\"remrem-password\":{},\"remrem-publish-url\":{\"value\":\"" + publishUrl + "\"},"
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

        HttpRequest request = new HttpRequest(HttpMethod.PUT);
        ResponseEntity response;
        response = request
                .setHeader("Cookie", gerritAccountCookie)
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setHeader("Accept", "application/json")
                .setHeader("X-Gerrit-Auth", gerritXAuthToken)
                .setBaseUrl("http://localhost:8080")
                .setEndpoint("/projects/" + projectName + "/config")
                .setBody(jsonBodyString)
                .performRequest();

        return response;
    }
}
