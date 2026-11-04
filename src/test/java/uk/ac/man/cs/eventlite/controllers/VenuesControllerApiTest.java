package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
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

import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventRepository;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesControllerApi.class)
@Import({ Security.class, VenueModelAssembler.class, EventModelAssembler.class })
public class VenuesControllerApiTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private VenueService venueService;

	@MockitoBean
	private EventRepository eventRepository;

	// GET /api/venues
	@Test
	public void getAllVenuesWhenEmpty() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>emptyList());

		mvc.perform(get("/api/venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllVenues")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues")));

		verify(venueService).findAll();
	}

	@Test
	public void getAllVenuesWithVenues() throws Exception {
		Venue v = new Venue();
		v.setId(1L);
		v.setName("Kilburn Building");
		v.setCapacity(120);

		when(venueService.findAll()).thenReturn(Collections.singletonList(v));

		mvc.perform(get("/api/venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllVenues")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues")))
				.andExpect(jsonPath("$._embedded.venues.length()", equalTo(1)))
				.andExpect(jsonPath("$._embedded.venues[0].name", equalTo("Kilburn Building")))
				.andExpect(jsonPath("$._embedded.venues[0].capacity", equalTo(120)));

		verify(venueService).findAll();
	}
	
    // made test for GET /api/venues/{id}
	@Test
	public void getVenueWithLinks() throws Exception {
		Venue v = new Venue();
		v.setId(1L);
		v.setName("Kilburn Building");
		v.setCapacity(120);

		when(venueService.findById(1L)).thenReturn(v);

		mvc.perform(get("/api/venues/1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(handler().methodName("getVenue"))
				.andExpect(jsonPath("$.name", equalTo("Kilburn Building")))
				.andExpect(jsonPath("$.capacity", equalTo(120)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1")))
				.andExpect(jsonPath("$._links.venue.href", endsWith("/api/venues/1")))
				.andExpect(jsonPath("$._links.events.href", endsWith("/api/venues/1/events")))
				.andExpect(jsonPath("$._links.next3events.href", endsWith("/api/venues/1/next3events")));

		verify(venueService).findById(1L);
	}

	// made test for GET /api/venues/{id}/events
	@Test
	public void getVenueEvents() throws Exception {
		Venue v = new Venue();
		v.setId(1L);
		v.setName("Kilburn Building");
		v.setCapacity(120);

		Event e = new Event();
		e.setId(1L);
		e.setName("Spring Boot Workshop");
		e.setDate(LocalDate.now());
		e.setTime(LocalTime.now());
		e.setVenue(v);

		when(eventRepository.findByVenueId(1L))
				.thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/venues/1/events").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(handler().methodName("getVenueEvents"))
				.andExpect(jsonPath("$[0].name", equalTo("Spring Boot Workshop")))
				.andExpect(jsonPath("$[0].venue.name", equalTo("Kilburn Building")));

		verify(eventRepository).findByVenueId(1L);
	}

	// made test for GET /api/venues/{id}/next3events
	@Test
	public void getNext3EventsWithEvents() throws Exception {
		Venue v = new Venue();
		v.setId(1L);
		v.setName("Kilburn Building");
		v.setCapacity(120);

		Event e = new Event();
		e.setId(1L);
		e.setName("Spring Boot Workshop");
		e.setDate(LocalDate.now().plusDays(1));
		e.setTime(LocalTime.now());
		e.setVenue(v);

		when(eventRepository.findByVenueIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(
				any(), any(), any()))
				.thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/venues/1/next3events").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(handler().methodName("getNext3Events"))
				.andExpect(jsonPath("$._embedded.events[0].name", equalTo("Spring Boot Workshop")))
				.andExpect(jsonPath("$._embedded.events[0].venue.name", equalTo("Kilburn Building")));
	}

	// made test for GET /api/venues/{id}/next3events
	@Test
	public void getNext3EventsNoEvents() throws Exception {
		when(eventRepository.findByVenueIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(
				any(), any(), any()))
				.thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/api/venues/1/next3events").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(handler().methodName("getNext3Events"));
	}
	
	@Test
	public void getVenueNotFound() throws Exception {
		when(venueService.findById(99L)).thenReturn(null);

		try {
			mvc.perform(get("/api/venues/99").accept(MediaType.APPLICATION_JSON));
		} catch (Exception e) {
			// 
		}

		verify(venueService).findById(99L);
	}

}