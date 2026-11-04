package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

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
import uk.ac.man.cs.eventlite.dao.MastodonService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventsController.class)
@Import(Security.class)
public class EventsControllerTest {

	@Autowired
	private MockMvc mvc;

	@Mock
	private Event event;

	@Mock
	private Venue venue;

	@MockitoBean
	private EventService eventService;

	@MockitoBean
	private VenueService venueService;

	@MockitoBean
	private MastodonService mastodonService;
	

	@Test
	public void getIndexWhenNoEvents() throws Exception {
		when(eventService.findAll()).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/index"))
				.andExpect(handler().methodName("getAllEvents"))
				.andExpect(model().attributeExists("upcoming"))
				.andExpect(model().attributeExists("previous"));

		verify(eventService).findAll();
		verifyNoInteractions(event);
		verifyNoInteractions(venueService); 
	}

	@Test
	public void getIndexWithEvents() throws Exception {
		when(event.getDate()).thenReturn(LocalDate.now().plusDays(1));
		when(event.getTime()).thenReturn(LocalTime.of(12, 0));
		
		when(eventService.findAll()).thenReturn(Collections.<Event>singletonList(event));

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/index"))
				.andExpect(handler().methodName("getAllEvents"))
				.andExpect(model().attributeExists("upcoming"))
				.andExpect(model().attributeExists("previous"));

		verify(eventService).findAll();
		verifyNoInteractions(venueService); 
	}

	@Test
	public void getEventNotFound() throws Exception {
		when(eventService.findById(99L)).thenThrow(new EventNotFoundException(99L));

		mvc.perform(get("/events/99").accept(MediaType.TEXT_HTML))
				.andExpect(status().isNotFound())
				.andExpect(view().name("events/not_found"))
				.andExpect(handler().methodName("getEvent"));
	}
	

	
	@Test
	public void getEventFound() throws Exception {
		when(eventService.findById(1L)).thenReturn(event);
 
		mvc.perform(get("/events/1").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/detail"))
				.andExpect(handler().methodName("getEvent"));
 
		verify(eventService).findById(1L);
	}

	@Test
	public void getIndexWithPreviousEvent() throws Exception {
		when(event.getDate()).thenReturn(LocalDate.now().minusDays(1));
		when(event.getTime()).thenReturn(LocalTime.of(12, 0));
		when(eventService.findAll()).thenReturn(Collections.<Event>singletonList(event));

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/index"))
				.andExpect(handler().methodName("getAllEvents"))
				.andExpect(model().attributeExists("previous"));

		verify(eventService).findAll();
	}

	@Test
	public void getIndexWithNullTimeEvent() throws Exception {
		when(event.getDate()).thenReturn(LocalDate.now().plusDays(1));
		when(event.getTime()).thenReturn(null);
		when(eventService.findAll()).thenReturn(Collections.<Event>singletonList(event));

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/index"))
				.andExpect(handler().methodName("getAllEvents"));

		verify(eventService).findAll();
	}

	@Test
	public void getIndexWithSearchQuery() throws Exception {
		when(event.getDate()).thenReturn(LocalDate.now().plusDays(1));
		when(event.getTime()).thenReturn(LocalTime.of(12, 0));
		when(eventService.searchAll("concert")).thenReturn(Collections.<Event>singletonList(event));
 
		mvc.perform(get("/events").param("search_query", "concert").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/index"))
				.andExpect(handler().methodName("getAllEvents"));
 
		verify(eventService).searchAll("concert");
		verify(eventService, never()).findAll();
	}
 
	@Test
	public void getNewEventForm() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));
 
		mvc.perform(get("/events/new")
				.with(user("admin").roles("ADMINISTRATOR"))
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/new"))
				.andExpect(handler().methodName("newEvent"))
				.andExpect(model().attributeExists("event"))
				.andExpect(model().attributeExists("venues"));
 
		verify(venueService).findAll();
	}
 
	@Test
	public void postNewEventValid() throws Exception {
		mvc.perform(post("/events")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test Event")
				.param("date", LocalDate.now().plusDays(1).toString())
				.param("time", LocalTime.of(12, 0).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/events"))
				.andExpect(handler().methodName("createEvent"));
 
		verify(eventService).save(any(Event.class));
	}
 
	@Test
	public void postNewEventInvalidNoName() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));
 
		mvc.perform(post("/events")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "")
				.param("date", LocalDate.now().plusDays(1).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/new"))
				.andExpect(handler().methodName("createEvent"));
 
		verify(eventService, never()).save(any(Event.class));
	}
 
	@Test
	public void postNewEventInvalidNoDate() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));
 
		mvc.perform(post("/events")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test Event")
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/new"))
				.andExpect(handler().methodName("createEvent"));
 
		verify(eventService, never()).save(any(Event.class));
	}
 
	@Test
	public void getEditEventForm() throws Exception {
		when(eventService.findById(1L)).thenReturn(event);
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));
 
		mvc.perform(get("/events/1/edit")
				.with(user("admin").roles("ADMINISTRATOR"))
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/edit"))
				.andExpect(handler().methodName("editEvent"))
				.andExpect(model().attributeExists("event"))
				.andExpect(model().attributeExists("venues"));
 
		verify(eventService).findById(1L);
		verify(venueService).findAll();
	}
 
	@Test
	public void patchEventValid() throws Exception {
		mvc.perform(patch("/events/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Event")
				.param("date", LocalDate.now().plusDays(2).toString())
				.param("time", LocalTime.of(14, 0).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/events"))
				.andExpect(handler().methodName("updateEvent"));
 
		verify(eventService).update(eq(1L), any(Event.class));
	}
 
	@Test
	public void patchEventInvalidNoName() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));
 
		mvc.perform(patch("/events/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "")
				.param("date", LocalDate.now().plusDays(2).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/edit"))
				.andExpect(handler().methodName("updateEvent"));
 
		verify(eventService, never()).update(eq(1L), any(Event.class));
	}
 
	@Test
	public void deleteEvent() throws Exception {
		mvc.perform(delete("/events/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/events"))
				.andExpect(handler().methodName("deleteEvent"));

		verify(eventService).deleteById(1L);
	}

	// --- POST /events validation ---

	@Test
	public void postNewEventInvalidNoVenue() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(post("/events")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test Event")
				.param("date", LocalDate.now().plusDays(1).toString())
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/new"))
				.andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postNewEventInvalidNameTooLong() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(post("/events")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "a".repeat(256))
				.param("date", LocalDate.now().plusDays(1).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/new"))
				.andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postNewEventInvalidDescriptionTooLong() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(post("/events")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test Event")
				.param("date", LocalDate.now().plusDays(1).toString())
				.param("venue.id", "1")
				.param("description", "a".repeat(500))
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/new"))
				.andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postNewEventWithoutDescriptionIsValid() throws Exception {
		mvc.perform(post("/events")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test Event")
				.param("date", LocalDate.now().plusDays(1).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/events"))
				.andExpect(handler().methodName("createEvent"));

		verify(eventService).save(any(Event.class));
	}

	@Test
	public void postNewEventInvalidPastDate() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(post("/events")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test Event")
				.param("date", LocalDate.now().minusDays(1).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/new"))
				.andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postNewEventInvalidTodaysDate() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(post("/events")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test Event")
				.param("date", LocalDate.now().toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/new"))
				.andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postNewEventUnauthorised() throws Exception {
		mvc.perform(post("/events")
				.with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test Event")
				.param("date", LocalDate.now().plusDays(1).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection());

		verify(eventService, never()).save(any(Event.class));
	}

	// --- PATCH /events/{id} validation ---

	@Test
	public void patchEventInvalidNoDate() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(patch("/events/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Event")
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/edit"))
				.andExpect(handler().methodName("updateEvent"));

		verify(eventService, never()).update(eq(1L), any(Event.class));
	}

	@Test
	public void patchEventInvalidNoVenue() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(patch("/events/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Event")
				.param("date", LocalDate.now().plusDays(1).toString())
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/edit"))
				.andExpect(handler().methodName("updateEvent"));

		verify(eventService, never()).update(eq(1L), any(Event.class));
	}

	@Test
	public void patchEventInvalidNameTooLong() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(patch("/events/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "a".repeat(256))
				.param("date", LocalDate.now().plusDays(1).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/edit"))
				.andExpect(handler().methodName("updateEvent"));

		verify(eventService, never()).update(eq(1L), any(Event.class));
	}

	@Test
	public void patchEventInvalidDescriptionTooLong() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(patch("/events/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Event")
				.param("date", LocalDate.now().plusDays(1).toString())
				.param("venue.id", "1")
				.param("description", "a".repeat(500))
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/edit"))
				.andExpect(handler().methodName("updateEvent"));

		verify(eventService, never()).update(eq(1L), any(Event.class));
	}

	@Test
	public void patchEventWithoutDescriptionIsValid() throws Exception {
		mvc.perform(patch("/events/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Event")
				.param("date", LocalDate.now().plusDays(1).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/events"))
				.andExpect(handler().methodName("updateEvent"));

		verify(eventService).update(eq(1L), any(Event.class));
	}

	@Test
	public void patchEventInvalidPastDate() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(patch("/events/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Event")
				.param("date", LocalDate.now().minusDays(1).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("events/edit"))
				.andExpect(handler().methodName("updateEvent"));

		verify(eventService, never()).update(eq(1L), any(Event.class));
	}

	@Test
	public void patchEventUnauthorised() throws Exception {
		mvc.perform(patch("/events/1")
				.with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Event")
				.param("date", LocalDate.now().plusDays(1).toString())
				.param("venue.id", "1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection());

		verify(eventService, never()).update(eq(1L), any(Event.class));
	}

	// --- DELETE /events/{id} ---

	@Test
	public void deleteEventUnauthorised() throws Exception {
		mvc.perform(delete("/events/1")
				.with(csrf())
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection());

		verify(eventService, never()).deleteById(1L);
	}

	// --- POST /events/{id}/share ---

	@Test
	public void shareEvent() throws Exception {
		when(mastodonService.shareEvent("Check out this event!")).thenReturn("Your post was posted");

		mvc.perform(post("/events/1/share")
				.with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("message", "Check out this event!")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/events/1"))
				.andExpect(handler().methodName("shareEvent"));

		verify(mastodonService).shareEvent("Check out this event!");
	}

}