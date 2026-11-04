package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static uk.ac.man.cs.eventlite.testutil.FormUtil.getCsrfToken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import uk.ac.man.cs.eventlite.EventLite;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class VenuesControllerIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

	private static final String SESSION_KEY = "JSESSIONID";
	private static final String LOGIN_PATH = "/sign-in";

	@LocalServerPort
	private int port;

	private String baseUri;
	private WebTestClient client;

	@Autowired
	private VenueService venueService;

	@BeforeEach
	public void setup() {
		baseUri = "http://localhost:" + port;
		client = WebTestClient.bindToServer().baseUrl(baseUri).build();
	}

	// --- Helper: login and return session cookie ---
	private String login() {
		EntityExchangeResult<String> result = client.get().uri(LOGIN_PATH).accept(MediaType.TEXT_HTML)
				.exchange().expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());
		String cookie = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("username", "Rob");
		form.add("password", "Haines");

		result = client.post().uri(LOGIN_PATH).accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isFound()
				.expectBody(String.class).returnResult();

		return result.getResponseCookies().getFirst(SESSION_KEY).getValue();
	}

	// --- GET Tests ---

	@Test
	public void testGetAllVenues() {
		client.get().uri("/venues").accept(MediaType.TEXT_HTML).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("Kilburn Building"));
					assertThat(result.getResponseBody(), containsString("Crawford Building"));
				});
	}

	@Test
	public void testGetVenue() {
		Venue first = venueService.findAll().iterator().next();
		long id = first.getId();

		client.get().uri("/venues/" + id).accept(MediaType.TEXT_HTML).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString(first.getName()));
				});
	}

	@Test
	public void testGetVenueNotFound() {
		client.get().uri("/venues/99").accept(MediaType.TEXT_HTML).exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("Not Found"));
				});
	}

	@Test
	public void testGetNewVenueForm() {
		String cookie = login();

		client.get().uri("/venues/new").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
	}

	@Test
	public void testGetEditVenueForm() {
		String cookie = login();
		Venue first = venueService.findAll().iterator().next();
		long id = first.getId();

		client.get().uri("/venues/" + id + "/edit").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString(first.getName()));
				});
	}

	// --- POST Tests: Create Venue ---

	@Test
	public void testCreateVenueWithSensibleData() {
		String cookie = login();

		EntityExchangeResult<String> result = client.get().uri("/venues/new").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "Integration Test Venue");
		form.add("address", "123 Test Street M1 1AA");
		form.add("capacity", "200");

		client.post().uri("/venues").accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", equalTo(baseUri + "/venues"));
	}

	@Test
	public void testCreateVenueWithMissingData() {
		String cookie = login();

		EntityExchangeResult<String> result = client.get().uri("/venues/new").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		// Missing name, address, and capacity
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);

		client.post().uri("/venues").accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isOk()
				.expectBody(String.class).consumeWith(r -> {
					assertThat(r.getResponseBody(), containsString("text-danger"));
				});
	}

	@Test
	public void testCreateVenueWithInvalidData() {
		String cookie = login();

		EntityExchangeResult<String> result = client.get().uri("/venues/new").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		// Name too long (> 255 chars), address without valid postcode, negative capacity
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "A".repeat(256));
		form.add("address", "No postcode here");
		form.add("capacity", "-1");

		client.post().uri("/venues").accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isOk()
				.expectBody(String.class).consumeWith(r -> {
					assertThat(r.getResponseBody(), containsString("text-danger"));
				});
	}

	@Test
	public void testCreateVenueUnauthorised() {
		EntityExchangeResult<String> result = client.get().uri(LOGIN_PATH).accept(MediaType.TEXT_HTML)
				.exchange().expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());
		String cookie = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "Unauthorised Venue");
		form.add("address", "123 Test Street M1 1AA");
		form.add("capacity", "200");

		// Unauthenticated POST should redirect to sign-in
		client.post().uri("/venues").accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", containsString("/sign-in"));
	}

	// --- POST Tests: Update Venue ---

	@Test
	public void testUpdateVenueWithSensibleData() {
		String cookie = login();
		Venue first = venueService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri("/venues/" + id + "/edit").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "Updated Venue Name");
		form.add("address", "456 New Street M2 2BB");
		form.add("capacity", "300");

		client.post().uri("/venues/" + id).accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", equalTo(baseUri + "/venues"));
	}

	@Test
	public void testUpdateVenueWithMissingData() {
		String cookie = login();
		Venue first = venueService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri("/venues/" + id + "/edit").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		// Missing name, address, and capacity
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);

		client.post().uri("/venues/" + id).accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isOk()
				.expectBody(String.class).consumeWith(r -> {
					assertThat(r.getResponseBody(), containsString("text-danger"));
				});
	}

	@Test
	public void testUpdateVenueWithInvalidData() {
		String cookie = login();
		Venue first = venueService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri("/venues/" + id + "/edit").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		// Name too long, negative capacity
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "A".repeat(256));
		form.add("address", "No postcode here");
		form.add("capacity", "-1");

		client.post().uri("/venues/" + id).accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isOk()
				.expectBody(String.class).consumeWith(r -> {
					assertThat(r.getResponseBody(), containsString("text-danger"));
				});
	}

	@Test
	public void testUpdateVenueUnauthorised() {
		Venue first = venueService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri(LOGIN_PATH).accept(MediaType.TEXT_HTML)
				.exchange().expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());
		String cookie = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "Unauthorised Update");
		form.add("address", "123 Test Street M1 1AA");
		form.add("capacity", "200");

		client.post().uri("/venues/" + id).accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", containsString("/sign-in"));
	}

	// --- DELETE Tests ---

	@Test
	public void testDeleteVenueWithNoEvents() {
		String cookie = login();

		// Create a venue with no events to safely delete
		EntityExchangeResult<String> result = client.get().uri("/venues/new").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "Venue To Delete");
		form.add("address", "1 Delete Street M1 1AA");
		form.add("capacity", "50");

		client.post().uri("/venues").accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isFound();

		// Find the newly created venue
		Venue toDelete = null;
		for (Venue v : venueService.findAll()) {
			if ("Venue To Delete".equals(v.getName())) {
				toDelete = v;
				break;
			}
		}
		long id = toDelete.getId();

		// Get CSRF token from the venue detail page
		result = client.get().uri("/venues/" + id).accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		csrfToken = getCsrfToken(result.getResponseBody());

		client.delete().uri("/venues/" + id + "?_csrf=" + csrfToken).accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", equalTo(baseUri + "/venues"));
	}

	@Test
	public void testDeleteVenueWithEvents() {
		String cookie = login();
		// The test data venues have events, so deleting should fail
		Venue first = venueService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri("/venues/" + id).accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		// Should redirect back to venue detail with error
		client.delete().uri("/venues/" + id + "?_csrf=" + csrfToken).accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", equalTo(baseUri + "/venues/" + id));
	}

	@Test
	public void testDeleteVenueUnauthorised() {
		Venue first = venueService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri(LOGIN_PATH).accept(MediaType.TEXT_HTML)
				.exchange().expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());
		String cookie = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

		client.delete().uri("/venues/" + id + "?_csrf=" + csrfToken).accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", containsString("/sign-in"));
	}
}
