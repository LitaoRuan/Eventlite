package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(HomeController.class)
@Import(Security.class)
public class HomeControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private EventService eventService;

	@MockitoBean
	private VenueService venueService;

	@Mock
	private Event event;

	@Mock
	private Venue venue;

	@Test
	public void getHomeReturnsHomeView() throws Exception {
		when(eventService.findNext3UpcomingEvents()).thenReturn(Collections.emptyList());
		when(venueService.findTop3VenuesByEventCount()).thenReturn(Collections.emptyList());

		mvc.perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("home"))
				.andExpect(handler().methodName("home"))
				.andExpect(model().attributeExists("upcomingEvents"))
				.andExpect(model().attributeExists("topVenues"));

		verify(eventService).findNext3UpcomingEvents();
		verify(venueService).findTop3VenuesByEventCount();
	}

	@Test
	public void getHomeWithUpcomingEventsAndTopVenues() throws Exception {
		when(event.getName()).thenReturn("Jazz Night");
		when(event.getDate()).thenReturn(LocalDate.now().plusDays(1));
		when(event.getTime()).thenReturn(LocalTime.of(19, 0));
		when(event.getVenue()).thenReturn(venue);
		when(venue.getName()).thenReturn("Kilburn Building");

		when(eventService.findNext3UpcomingEvents()).thenReturn(List.of(event));
		when(venueService.findTop3VenuesByEventCount()).thenReturn(List.of(venue));

		mvc.perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("home"))
				.andExpect(handler().methodName("home"))
				.andExpect(model().attributeExists("upcomingEvents"))
				.andExpect(model().attributeExists("topVenues"));

		verify(eventService).findNext3UpcomingEvents();
		verify(venueService).findTop3VenuesByEventCount();
	}
}
