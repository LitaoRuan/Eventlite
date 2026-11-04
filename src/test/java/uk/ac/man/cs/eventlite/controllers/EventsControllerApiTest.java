package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventsControllerApi.class)
@Import({ Security.class, EventModelAssembler.class, VenueModelAssembler.class })
public class EventsControllerApiTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private EventService eventService;

	@Test
	public void getIndexWhenNoEvents() throws Exception {
		when(eventService.findAll()).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/api/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllEvents")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events")));

		verify(eventService).findAll();
	}

	@Test
	public void getIndexWithEvents() throws Exception {
		Event e = new Event();
		e.setId(0);
		e.setName("Event");
		e.setDate(LocalDate.now());
		e.setTime(LocalTime.now());
		Venue v = new Venue();
		v.setId(1);
		v.setName("Test Venue");
		e.setVenue(v);
		when(eventService.findAll()).thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));

		verify(eventService).findAll();
	}

	@Test
	public void getEventWithLinks() throws Exception {
		Venue v = new Venue();
		v.setId(1L);
		v.setName("Test Venue");
		v.setCapacity(100);

		Event e = new Event();
		e.setId(1L);
		e.setName("Test Event");
		e.setDate(LocalDate.now().plusDays(1));
		e.setTime(LocalTime.of(12, 0));
		e.setVenue(v);

		when(eventService.findById(1L)).thenReturn(e);

		mvc.perform(get("/api/events/1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(handler().methodName("getEvent"))
				.andExpect(jsonPath("$.name", equalTo("Test Event")))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events/1")))
				.andExpect(jsonPath("$._links.event.href", endsWith("/api/events/1")))
				.andExpect(jsonPath("$._links.venue.href", endsWith("/api/events/1/venue")));

		verify(eventService).findById(1L);
	}

	@Test
	public void getEventVenue() throws Exception {
		Venue v = new Venue();
		v.setId(1L);
		v.setName("Test Venue");
		v.setCapacity(100);

		Event e = new Event();
		e.setId(1L);
		e.setName("Test Event");
		e.setDate(LocalDate.now().plusDays(1));
		e.setTime(LocalTime.of(12, 0));
		e.setVenue(v);

		when(eventService.findById(1L)).thenReturn(e);

		mvc.perform(get("/api/events/1/venue").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(handler().methodName("getEventVenue"))
				.andExpect(jsonPath("$.name", equalTo("Test Venue")))
				.andExpect(jsonPath("$.capacity", equalTo(100)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1")))
				.andExpect(jsonPath("$._links.venue.href", endsWith("/api/venues/1")))
				.andExpect(jsonPath("$._links.events.href", endsWith("/api/venues/1/events")))
				.andExpect(jsonPath("$._links.next3events.href", endsWith("/api/venues/1/next3events")));

		verify(eventService).findById(1L);
	}

	@Test
	public void getEventVenueNotFound() throws Exception {
		when(eventService.findById(99L)).thenReturn(null);

		mvc.perform(get("/api/events/99/venue").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 99")))
				.andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getEventVenue"));

		verify(eventService).findById(99L);
	}

	@Test
	public void getEventNotFound() throws Exception {
		mvc.perform(get("/api/events/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getEvent"));
	}
}
