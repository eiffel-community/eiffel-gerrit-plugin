package com.ericsson.gerrit.plugins.eiffel.integrationtest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import cucumber.api.PendingException;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

public class ServiceIntegrationSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceIntegrationSteps.class);

	private final static String RABBITMQ_HOSTNAME = "172.18.0.2";
	private final static int RABBITMQ_PORT = 5672;
	private final static String RABBITMQ_USERNAME = "myuser";
	private final static String RABBITMQ_PASSWORD = "myuser";
	private final static String RABBITMQ_QUEUENAME = "gerrit-test-queue";
	private final static String RABBITMQ_EXCHANGE_NAME = "eiffel.poc";
	private final static String GERRIT_BASE_URL = "http://172.18.0.2:8080";

	private String projectName;
	private String gerritAccountCookie;
	private String gerritXAuthToken;

	@Before
	public void setUp() throws Exception {
		cleanUp();
		generateGerritAccountCookie();
		generateGerritXAuthToken();
		declareQueues();
	}

	@Given("^a project is created$")
	public void a_project_is_created() throws ClientProtocolException, URISyntaxException, IOException {
		this.projectName = UUID.randomUUID().toString();
		ResponseEntity response = createProject(projectName);

		int expected = 201;
		int actual = response.getStatusCode();
		assertEquals("Failed to create project", expected, actual);
	}

	@Given("^the project is configured to send eiffel events with publish url \"([^\"]*)\"$")
	public void the_project_is_configured_to_send_eiffel_events_with_publish_url(String publishUrl)
			throws ClientProtocolException, URISyntaxException, IOException {
		ResponseEntity response = enableEiffelMessaging(publishUrl);
		int expected = 200;
		int actual = response.getStatusCode();
		assertEquals("Failed to configure project", expected, actual);
	}

	@Given("^a change is created$")
	public void a_change_is_submitted() throws ClientProtocolException, URISyntaxException, IOException {
		ResponseEntity response = submitChange();
		int expected = 201;
		int actual = response.getStatusCode();
		assertEquals("Change is submitted", expected, actual);
	}

	@Then("^I should find an eiffel event in rabbitmq$")
	public void i_should_find_an_eiffel_event_in_rabbitmq() throws IOException, TimeoutException {
		List<String> messages = consumeMessages(1, 10000);
		assertEquals(1, messages.size());
	}

	private void generateGerritXAuthToken() throws Exception {
		try {
			HttpRequest request = new HttpRequest(HttpMethod.GET);
			ResponseEntity response;
			response = request.setBaseUrl(GERRIT_BASE_URL).setHeader("Cookie", gerritAccountCookie)
					.setHeader("Referer", "http://localhost:8080/login").setEndpoint("/").performRequest();

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

			ResponseEntity response = requestLogin.setBaseUrl(GERRIT_BASE_URL).setEndpoint("/login/?account_id=1000000")
					.addParameter("http.protocol.handle-redirects", "false").performRequest();

			Header[] headers = response.getHeaders();
			Header gerritAccountHeader = headers[4];
			gerritAccountCookie = gerritAccountHeader.getValue();
		} catch (Exception e) {
			throw new Exception("Failed to generate gerrit account cookie", e);
		}
	}

	private String getFirstMatchGroupFromText(String text, String regexp) throws Exception {
		Pattern pattern = Pattern.compile(regexp);
		Matcher matcher = pattern.matcher(text);

		while (matcher.find()) {
			String matchedGroup = matcher.group(1);

			if (matchedGroup == null) {
				throw new Exception("Failed to parse the text: \"" + text + "\" with regexp: \"" + regexp + "\"");
			}

			return matchedGroup;
		}

		throw new Exception("Could not find any match in the text: \"" + text + "\" with regexp: \"" + regexp + "\"");
	}

	private ResponseEntity createProject(String projectName)
			throws URISyntaxException, ClientProtocolException, IOException {
		JsonObject generateBody = new JsonObject();
		generateBody.addProperty("description", "Auto generated project");
		generateBody.addProperty("submit_type", "INHERIT");
		generateBody.addProperty("create_empty_commit", true);

		HttpRequest request = new HttpRequest(HttpMethod.PUT);
		ResponseEntity response;
		response = request.setHeader("Cookie", gerritAccountCookie)
				.setHeader("Content-Type", "application/json; charset=UTF-8").setHeader("Accept", "application/json")
				.setHeader("X-Gerrit-Auth", gerritXAuthToken).setBaseUrl(GERRIT_BASE_URL)
				.setEndpoint("/projects/" + projectName).setBody(generateBody.toString()).performRequest();

		return response;
	}

	private ResponseEntity enableEiffelMessaging(String publishUrl)
			throws URISyntaxException, ClientProtocolException, IOException {
		String fullPublishUrl = "http://" + publishUrl;

		String jsonBodyString = "{\"description\":\"Auto generated project\","
				+ "\"use_contributor_agreements\":\"INHERIT\",\"use_content_merge\":\"INHERIT\","
				+ "\"use_signed_off_by\":\"INHERIT\",\"require_change_id\":\"INHERIT\","
				+ "\"create_new_change_for_all_not_in_target\":\"INHERIT\","
				+ "\"reject_implicit_merges\":\"INHERIT\",\"submit_type\":\"MERGE_IF_NECESSARY\","
				+ "\"state\":\"ACTIVE\",\"plugin_config_values\":{\"Eiffel-Integration\":"
				+ "{\"enabled\":{\"value\":\"true\"},\"filter\":{},\"flow-context\":{},"
				+ "\"remrem-password\":{},\"remrem-publish-url\":{\"value\":\"" + fullPublishUrl + "\"},"
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
		response = request.setHeader("Cookie", gerritAccountCookie)
				.setHeader("Content-Type", "application/json; charset=UTF-8").setHeader("Accept", "application/json")
				.setHeader("X-Gerrit-Auth", gerritXAuthToken).setBaseUrl(GERRIT_BASE_URL)
				.setEndpoint("/projects/" + projectName + "/config").setBody(jsonBodyString).performRequest();

		return response;
	}

	private ResponseEntity submitChange() throws URISyntaxException, ClientProtocolException, IOException {
		String jsonBodyString = "{\"project\":\"" + projectName + "\" ,"
				+ "\"subject\" : \"Let's support 100% Gerrit workflow direct in browser\", "
				+ "\"topic\" : \"create-change-in-browser\", " + "\"status\" : \"DRAFT\", " + "\"branch\":\"master\"}";

		HttpRequest request = new HttpRequest(HttpMethod.POST);
		ResponseEntity response;
		response = request.setHeader("Cookie", gerritAccountCookie)
				.setHeader("Content-Type", "application/json; charset=UTF-8").setHeader("Accept", "application/json")
				.setHeader("X-Gerrit-Auth", gerritXAuthToken).setBaseUrl(GERRIT_BASE_URL).setEndpoint("/changes/")
				.setBody(jsonBodyString).performRequest();

		return response;
	}

	protected List<String> consumeMessages(int messageCount, long timeout) throws IOException, TimeoutException {
		List<String> messages = new ArrayList<String>();
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(RABBITMQ_HOSTNAME);
		factory.setPort(RABBITMQ_PORT);
		factory.setUsername(RABBITMQ_USERNAME);
		factory.setPassword(RABBITMQ_PASSWORD);
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		long stopTime = System.currentTimeMillis() + timeout;

		while (System.currentTimeMillis() < stopTime) {
			try {
				GetResponse response = channel.basicGet(RABBITMQ_QUEUENAME, true);
				if (response != null) {
					messages.add(new String(response.getBody(), "UTF-8"));
				}

				if (messages.size() == messageCount) {
					return messages;
				}
			} catch (Exception e) {
				LOGGER.error("RabbitMQ failed to get from queue", e);
			}
		}

		return messages;
	}

	protected void declareQueues() throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(RABBITMQ_HOSTNAME);
		factory.setPort(RABBITMQ_PORT);
		factory.setUsername(RABBITMQ_USERNAME);
		factory.setPassword(RABBITMQ_PASSWORD);
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		final AMQP.Exchange.DeclareOk exchangeOK = channel.exchangeDeclare(RABBITMQ_EXCHANGE_NAME, "topic", true);
		final AMQP.Queue.DeclareOk queueOK = channel.queueDeclare(RABBITMQ_QUEUENAME, true, false, false, null);
		final AMQP.Queue.BindOk bindOK = channel.queueBind(RABBITMQ_QUEUENAME, RABBITMQ_EXCHANGE_NAME, "#");
		assertEquals(true, exchangeOK != null);
		assertEquals(true, queueOK != null);
		assertEquals(true, bindOK != null);
	}

	@After
	public void cleanUp() throws Exception {
		consumeMessages(100, 2000);
	}
}
