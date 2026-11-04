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
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class EventsControllerIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

	private static final String SESSION_KEY = "JSESSIONID";
	private static final String LOGIN_PATH = "/sign-in";

	@LocalServerPort
	private int port;

	private String baseUri;
	private WebTestClient client;

	@Autowired
	private EventService eventService;

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
	public void testGetAllEvents() {
		client.get().uri("/events").accept(MediaType.TEXT_HTML).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("Spring Boot Workshop"));
					assertThat(result.getResponseBody(), containsString("Software Engineering Talk"));
					assertThat(result.getResponseBody(), containsString("Manchester Tech Meetup"));
				});
	}

	@Test
	public void testGetEvent() {
		Event first = eventService.findAll().iterator().next();
		long id = first.getId();

		client.get().uri("/events/" + id).accept(MediaType.TEXT_HTML).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString(first.getName()));
				});
	}

	@Test
	public void testGetEventNotFound() {
		client.get().uri("/events/99").accept(MediaType.TEXT_HTML).exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});
	}

	@Test
	public void testGetNewEventForm() {
		String cookie = login();

		client.get().uri("/events/new").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
	}

	@Test
	public void testGetEditEventForm() {
		String cookie = login();
		Event first = eventService.findAll().iterator().next();
		long id = first.getId();

		client.get().uri("/events/" + id + "/edit").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString(first.getName()));
				});
	}

	// --- POST Tests: Create Event ---

	@Test
	public void testCreateEventWithSensibleData() {
		String cookie = login();

		// Get the new event form to extract CSRF token
		EntityExchangeResult<String> result = client.get().uri("/events/new").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		Venue venue = venueService.findAll().iterator().next();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "Integration Test Event");
		form.add("date", "2027-12-01");
		form.add("time", "10:00");
		form.add("venue", String.valueOf(venue.getId()));

		client.post().uri("/events").accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", equalTo(baseUri + "/events"));
	}

	@Test
	public void testCreateEventWithMissingData() {
		String cookie = login();

		EntityExchangeResult<String> result = client.get().uri("/events/new").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		// Missing name, date, and venue
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);

		client.post().uri("/events").accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isOk()
				.expectBody(String.class).consumeWith(r -> {
					// Should return the form with validation errors, not redirect
					assertThat(r.getResponseBody(), containsString("text-danger"));
				});
	}

	@Test
	public void testCreateEventWithInvalidData() {
		String cookie = login();

		EntityExchangeResult<String> result = client.get().uri("/events/new").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		Venue venue = venueService.findAll().iterator().next();

		// Past date (invalid) and name too long (> 255 chars)
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "A".repeat(256));
		form.add("date", "2020-01-01");
		form.add("time", "10:00");
		form.add("venue", String.valueOf(venue.getId()));

		client.post().uri("/events").accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isOk()
				.expectBody(String.class).consumeWith(r -> {
					// Should return the form with validation errors, not redirect
					assertThat(r.getResponseBody(), containsString("text-danger"));
				});
	}

	@Test
	public void testCreateEventUnauthorised() {
		// Get CSRF token from the login page (which always has a form with CSRF)
		EntityExchangeResult<String> result = client.get().uri(LOGIN_PATH).accept(MediaType.TEXT_HTML)
				.exchange().expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());
		String cookie = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "Unauthorised Event");
		form.add("date", "2027-12-01");
		form.add("time", "10:00");

		// Unauthenticated POST should redirect to sign-in
		client.post().uri("/events").accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", containsString("/sign-in"));
	}

	// --- PATCH Tests: Update Event ---

	@Test
	public void testUpdateEventWithSensibleData() {
		String cookie = login();
		Event first = eventService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri("/events/" + id + "/edit").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		Venue venue = venueService.findAll().iterator().next();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "Updated Event Name");
		form.add("date", "2027-12-01");
		form.add("time", "14:00");
		form.add("venue", String.valueOf(venue.getId()));

		client.patch().uri("/events/" + id).accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", equalTo(baseUri + "/events"));
	}

	@Test
	public void testUpdateEventWithMissingData() {
		String cookie = login();
		Event first = eventService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri("/events/" + id + "/edit").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		// Missing name, date, and venue
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);

		client.patch().uri("/events/" + id).accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isOk()
				.expectBody(String.class).consumeWith(r -> {
					assertThat(r.getResponseBody(), containsString("text-danger"));
				});
	}

	@Test
	public void testUpdateEventWithInvalidData() {
		String cookie = login();
		Event first = eventService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri("/events/" + id + "/edit").accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		Venue venue = venueService.findAll().iterator().next();

		// Past date and name too long
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "A".repeat(256));
		form.add("date", "2020-01-01");
		form.add("time", "10:00");
		form.add("venue", String.valueOf(venue.getId()));

		client.patch().uri("/events/" + id).accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isOk()
				.expectBody(String.class).consumeWith(r -> {
					assertThat(r.getResponseBody(), containsString("text-danger"));
				});
	}

	@Test
	public void testUpdateEventUnauthorised() {
		Event first = eventService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri(LOGIN_PATH).accept(MediaType.TEXT_HTML)
				.exchange().expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());
		String cookie = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", csrfToken);
		form.add("name", "Unauthorised Update");
		form.add("date", "2027-12-01");
		form.add("time", "10:00");

		client.patch().uri("/events/" + id).accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.cookie(SESSION_KEY, cookie).bodyValue(form)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", containsString("/sign-in"));
	}

	// --- DELETE Tests ---

	@Test
	public void testDeleteEvent() {
		String cookie = login();
		Event first = eventService.findAll().iterator().next();
		long id = first.getId();

		// Get CSRF token from the event detail page
		EntityExchangeResult<String> result = client.get().uri("/events/" + id).accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());

		client.delete().uri("/events/" + id + "?_csrf=" + csrfToken).accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", equalTo(baseUri + "/events"));
	}

	@Test
	public void testDeleteEventUnauthorised() {
		Event first = eventService.findAll().iterator().next();
		long id = first.getId();

		EntityExchangeResult<String> result = client.get().uri(LOGIN_PATH).accept(MediaType.TEXT_HTML)
				.exchange().expectStatus().isOk().expectBody(String.class).returnResult();
		String csrfToken = getCsrfToken(result.getResponseBody());
		String cookie = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

		client.delete().uri("/events/" + id + "?_csrf=" + csrfToken).accept(MediaType.TEXT_HTML)
				.cookie(SESSION_KEY, cookie)
				.exchange().expectStatus().isFound()
				.expectHeader().value("Location", containsString("/sign-in"));
	}
}
