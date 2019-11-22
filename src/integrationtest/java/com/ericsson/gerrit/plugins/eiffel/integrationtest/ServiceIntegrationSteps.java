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
import com.google.gson.JsonObject;
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

	@Before
	public void setUp() throws Exception {
		declareQueues();
		cleanUp();
		generateGerritAccountCookie();
		generateGerritXAuthToken();
	}

	@Given("^a project is created$")
	public void aProjectIsCreated() throws ClientProtocolException, URISyntaxException, IOException {
		this.projectName = UUID.randomUUID().toString();
		ResponseEntity response = createProject(projectName);

		int expected = 201;
		int actual = response.getStatusCode();
		assertEquals("Failed to create project", expected, actual);
	}

	@And("^the project is configured to send eiffel events with publish url \"([^\"]*)\"$")
	public void theProjectIsConfiguredToSendEiffelEventsWithPublishUrl(String publishUrl)
			throws ClientProtocolException, URISyntaxException, IOException {
		ResponseEntity response = enableEiffelMessaging(publishUrl);
		int expected = 200;
		int actual = response.getStatusCode();
		assertEquals("Failed to configure project", expected, actual);
	}

	@When("^a change is created$")
	public void aChangeIsSubmitted() throws ClientProtocolException, URISyntaxException, IOException {
		ResponseEntity response = submitChange();
		int expected = 201;
		int actual = response.getStatusCode();
		assertEquals("Change is submitted", expected, actual);
	}

	@Then("^I should find an eiffel event in rabbitmq$")
	public void iShouldFindAnEiffelEventInRabbitmq() throws IOException, TimeoutException {
		int minMessageCount = 1;
		long responseWaitTimeoutInMillis = 10000;
		List<String> messages = consumeMessages(minMessageCount, responseWaitTimeoutInMillis);
		assertEquals("Expecting one message in queue but found" + messages.size(), 1, messages.size());
	}

	 /**
     * Generates a gerrit XAuth token. Since we are not using HTTP password for the REST API
     * we need to simulate what the browser is doing when signing in to gerrit. Gerrit XAuth
     * token is one of the needed cookies that needs to be set in the http header to access
     * the REST api without using HTTP password.
     *
     * @throws Exception
     */
	private void generateGerritXAuthToken() throws Exception {
		try {
			HttpRequest request = new HttpRequest(HttpMethod.GET);
			ResponseEntity response;
			response = request.setBaseUrl(GERRIT_BASE_URL).setHeader("Cookie", gerritAccountCookie)
					.setHeader("Referer", GERRIT_BASE_URL + "/login").setEndpoint("/").performRequest();

			Header[] headers = response.getHeaders();
			Header gerritAccountHeader = headers[1];

			String gerritXAuthHeader = gerritAccountHeader.getValue();
			String regexp = "XSRF_TOKEN=(.+);";
			this.gerritXAuthToken = getFirstMatchGroupFromText(gerritXAuthHeader, regexp);
		} catch (URISyntaxException e) {
			String stacktrace = ExceptionUtils.getStackTrace(e);
			throw new URISyntaxException("Failed to generate gerritXAuthToken", stacktrace);
		} catch (IOException e) {
			throw new IOException("Failed to generate gerritXAuthToken", e);
		}
	}

	/**
	 * Generates a gerrit account token. Since we are not using HTTP password for the REST API
	 * we need to simulate what the browser is doing when signing in to gerrit. Gerrit account
	 * token is one of the needed cookies that needs to be set in the http header to access
     * the REST api without using HTTP password.
     *
	 * @throws Exception
	 */
	private void generateGerritAccountCookie() throws Exception {
		try {
			CloseableHttpClient httpClientNoRedirect = HttpClientBuilder.create().disableRedirectHandling().build();
			HttpExecutor httpExecutor = new HttpExecutor(httpClientNoRedirect);
			HttpRequest requestLogin = new HttpRequest(HttpMethod.GET, httpExecutor);

			// Account ID 1000000 is the default value for the admin account in gerrit.
			ResponseEntity response = requestLogin.setBaseUrl(GERRIT_BASE_URL).setEndpoint("/login/?account_id=1000000")
					.addParameter("http.protocol.handle-redirects", "false").performRequest();

			Header[] headers = response.getHeaders();
			Header gerritAccountHeader = headers[4];
			this.gerritAccountCookie = gerritAccountHeader.getValue();
		} catch (URISyntaxException e) {
			String stacktrace = ExceptionUtils.getStackTrace(e);
			throw new URISyntaxException("Failed to generate Gerrit account cookie", stacktrace);
		} catch (IOException e) {
			throw new IOException("Failed to generate Gerrit account cokkie", e);
		}
	}

	private String getFirstMatchGroupFromText(String text, String regexp) throws Exception {
		Pattern pattern = Pattern.compile(regexp);
		Matcher matcher = pattern.matcher(text);

		matcher.find();
		String matchedGroup = matcher.group(1);

		if (matchedGroup == null) {
			throw new RegexMatchFailedException(
					"Failed to parse the text: \"" + text + "\" with regexp: \"" + regexp + "\"");
		}

		return matchedGroup;
	}

	private ResponseEntity createProject(String projectName)
			throws URISyntaxException, ClientProtocolException, IOException {
		JsonObject generateBody = new JsonObject();
		generateBody.addProperty("description", "Auto generated project");
		generateBody.addProperty("submit_type", "INHERIT");
		generateBody.addProperty("create_empty_commit", true);

		requestBodyString = generateBody.toString();
		endPoint = "/projects/" + projectName;
		httpMethod = HttpMethod.PUT;

		ResponseEntity response = setHeaderAndPerformRequest(endPoint, requestBodyString, httpMethod);

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

		requestBodyString = jsonBodyString;
		endPoint = "/projects/" + projectName + "/config";
		httpMethod = HttpMethod.PUT;

		ResponseEntity response = setHeaderAndPerformRequest(endPoint, requestBodyString, httpMethod);

		return response;
	}

	private ResponseEntity submitChange() throws URISyntaxException, ClientProtocolException, IOException {
		String jsonBodyString = "{\"project\":\"" + projectName + "\" ,"
				+ "\"subject\" : \"Let's support 100% Gerrit workflow direct in browser\", "
				+ "\"topic\" : \"create-change-in-browser\", " + "\"status\" : \"DRAFT\", " + "\"branch\":\"master\"}";

		requestBodyString = jsonBodyString;
		endPoint = "/changes/";
		httpMethod = HttpMethod.POST;

		ResponseEntity response = setHeaderAndPerformRequest(endPoint, requestBodyString, httpMethod);

		return response;
	}

	protected List<String> consumeMessages(int messageCount, long timeout) throws IOException, TimeoutException {
		List<String> messages = new ArrayList<String>();
		ConnectionFactory factory = createAndSetConnectionFactory();
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
		ConnectionFactory factory = createAndSetConnectionFactory();
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		final AMQP.Exchange.DeclareOk exchangeOK = channel.exchangeDeclare(RABBITMQ_EXCHANGE_NAME, "topic", true);
		final AMQP.Queue.DeclareOk queueOK = channel.queueDeclare(RABBITMQ_QUEUENAME, true, false, false, null);
		final AMQP.Queue.BindOk bindOK = channel.queueBind(RABBITMQ_QUEUENAME, RABBITMQ_EXCHANGE_NAME, "#");
		assertEquals("Expecting non-null  but RabbitMQ exchange is " + exchangeOK, true, exchangeOK != null);
		assertEquals("Expecting non-null but RabbitMQ queue is " + queueOK, true, queueOK != null);
		assertEquals("Expecting non-null but RabbitMQ binding key is " + bindOK, true, bindOK != null);
	}

	private ResponseEntity setHeaderAndPerformRequest(String endPoint, String setBodyString, HttpMethod httpMethod)
			throws URISyntaxException, ClientProtocolException, IOException {
		HttpRequest request = new HttpRequest(httpMethod);
		ResponseEntity response;
		response = request.setHeader("Cookie", gerritAccountCookie)
				.setHeader("Content-Type", "application/json; charset=UTF-8").setHeader("Accept", "application/json")
				.setHeader("X-Gerrit-Auth", gerritXAuthToken).setBaseUrl(GERRIT_BASE_URL).setEndpoint(endPoint)
				.setBody(setBodyString).performRequest();
		return response;
	}

	private ConnectionFactory createAndSetConnectionFactory() {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(RABBITMQ_HOSTNAME);
		factory.setPort(RABBITMQ_PORT);
		factory.setUsername(RABBITMQ_USERNAME);
		factory.setPassword(RABBITMQ_PASSWORD);
		return factory;
	}

	@After
	public void cleanUp() throws Exception {
		consumeMessages(100, 2000);
	}
}
