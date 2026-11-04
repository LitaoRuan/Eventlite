package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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
import uk.ac.man.cs.eventlite.dao.GeocodingService;


import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesController.class)
@Import(Security.class)
public class VenuesControllerTest {

	@Autowired
	private MockMvc mvc;

	@Mock
	private Venue venue;

	@Mock
	private Event event;

	@MockitoBean
	private VenueService venueService;
	
	@MockitoBean
	private GeocodingService geocodingService;

	@Test
	public void getAllVenues() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.singletonList(venue));

		mvc.perform(get("/venues").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/index"))
				.andExpect(model().attributeExists("venues"));

		verify(venueService).findAll();
	}

	@Test
	public void searchVenues() throws Exception {
		when(venueService.searchAll("Kilburn")).thenReturn(Collections.singletonList(venue));

		mvc.perform(get("/venues").param("search_query", "Kilburn").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/index"))
				.andExpect(model().attributeExists("venues"));

		verify(venueService).searchAll("Kilburn");
	}

	@Test
	public void getVenueFound() throws Exception {
		Venue testVenue = new Venue();
		testVenue.setId(1L);
		testVenue.setName("Test Venue");
		testVenue.setAddress("123 Test Road, M13 9PL");
		testVenue.setCapacity(100);


		Event testEvent = new Event();
		testEvent.setName("Test Event");
		testEvent.setDate(LocalDate.now().plusDays(1)); 
		testEvent.setTime(LocalTime.of(12, 0));
        
		testEvent.setVenue(testVenue); 
		
		testVenue.setEvents(Collections.singletonList(testEvent));

		when(venueService.findById(1L)).thenReturn(testVenue);

	
		mvc.perform(get("/venues/1")
				.with(user("admin").roles("ADMINISTRATOR"))
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/detail"))
				.andExpect(model().attributeExists("venue"))
				.andExpect(model().attributeExists("events"));

		verify(venueService).findById(1L);
	}

	@Test
	public void getVenueNotFound() throws Exception {
		when(venueService.findById(99L)).thenThrow(new VenueNotFoundException(99L));

		mvc.perform(get("/venues/99").accept(MediaType.TEXT_HTML))
				.andExpect(status().isNotFound())
				.andExpect(view().name("venues/not_found"));
	}

	@Test
	public void getVenueNull() throws Exception {
		when(venueService.findById(99L)).thenReturn(null);

		mvc.perform(get("/venues/99").accept(MediaType.TEXT_HTML))
				.andExpect(status().isNotFound())
				.andExpect(view().name("venues/not_found"));
	}

	@Test
	public void editVenueNull() throws Exception {
		when(venueService.findById(99L)).thenReturn(null);

		mvc.perform(get("/venues/99/edit")
				.with(user("admin").roles("ADMINISTRATOR"))
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isNotFound())
				.andExpect(view().name("venues/not_found"));
	}


	@Test
	public void getNewVenueFormTest() throws Exception {
		mvc.perform(get("/venues/new")
				.with(user("admin").roles("ADMINISTRATOR"))
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/new"))
				.andExpect(model().attributeExists("venue"));
	}

	@Test
	public void createValidVenueTest() throws Exception {
		mvc.perform(post("/venues")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn Building")
				.param("address", "Oxford Road M13 9PL")
				.param("capacity", "120")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/venues"));

		verify(venueService).save(any(Venue.class));
	}

	@Test
	public void createVenueInvalidNoName() throws Exception {
		mvc.perform(post("/venues")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "")
				.param("address", "Oxford Road M13 9PL")
				.param("capacity", "120")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/new"))
				.andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}



	@Test
	public void getEditVenueFormTest() throws Exception {
		when(venueService.findById(1L)).thenReturn(venue);

		mvc.perform(get("/venues/1/edit")
				.with(user("admin").roles("ADMINISTRATOR"))
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/edit"))
				.andExpect(handler().methodName("editVenue"))
				.andExpect(model().attributeExists("venue"));
	}

	@Test
	public void updateValidVenueTest() throws Exception {
		mvc.perform(post("/venues/1") 
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Kilburn")
				.param("address", "Oxford Road M13 9PL")
				.param("capacity", "150")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/venues"));

		verify(venueService).save(any(Venue.class));
	}

	@Test
	public void updateVenueInvalidNoName() throws Exception {
		mvc.perform(post("/venues/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "")
				.param("address", "Oxford Road M13 9PL")
				.param("capacity", "150")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/edit"))
				.andExpect(handler().methodName("updateVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}



	@Test
	public void deleteVenueSuccess() throws Exception {
		mvc.perform(delete("/venues/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/venues"));

		verify(venueService).deleteById(1L);
	}

	@Test
	public void deleteVenueWithEventsFails() throws Exception {

		doThrow(new IllegalStateException("Cannot delete a venue that has events."))
				.when(venueService).deleteById(1L);

		mvc.perform(delete("/venues/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/venues/1"))
				.andExpect(flash().attributeExists("error"));
	}

	@Test
	public void createVenueUnauthorised() throws Exception {
		mvc.perform(post("/venues")
				.with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn Building")
				.param("address", "Oxford Road M13 9PL")
				.param("capacity", "120")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection());

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void createVenueInvalidNoAddress() throws Exception {
		mvc.perform(post("/venues")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn Building")
				.param("address", "")
				.param("capacity", "120")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/new"))
				.andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void createVenueInvalidNoPostcode() throws Exception {
		mvc.perform(post("/venues")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn Building")
				.param("address", "Oxford Road")
				.param("capacity", "120")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/new"))
				.andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void createVenueInvalidNegativeCapacity() throws Exception {
		mvc.perform(post("/venues")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn Building")
				.param("address", "Oxford Road M13 9PL")
				.param("capacity", "-1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/new"))
				.andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void createVenueInvalidNameTooLong() throws Exception {
		String longName = "a".repeat(256);

		mvc.perform(post("/venues")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", longName)
				.param("address", "Oxford Road M13 9PL")
				.param("capacity", "120")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/new"))
				.andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void createVenueInvalidAddressTooLong() throws Exception {
		String longAddress = "a".repeat(293) + " M13 9PL";

		mvc.perform(post("/venues")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Kilburn Building")
				.param("address", longAddress)
				.param("capacity", "120")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/new"))
				.andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void updateVenueUnauthorised() throws Exception {
		mvc.perform(post("/venues/1")
				.with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Kilburn")
				.param("address", "Oxford Road M13 9PL")
				.param("capacity", "150")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection());

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void updateVenueInvalidNoAddress() throws Exception {
		mvc.perform(post("/venues/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Kilburn")
				.param("address", "")
				.param("capacity", "150")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/edit"))
				.andExpect(handler().methodName("updateVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void updateVenueInvalidNoPostcode() throws Exception {
		mvc.perform(post("/venues/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Kilburn")
				.param("address", "Oxford Road")
				.param("capacity", "150")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/edit"))
				.andExpect(handler().methodName("updateVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void updateVenueInvalidNegativeCapacity() throws Exception {
		mvc.perform(post("/venues/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Kilburn")
				.param("address", "Oxford Road M13 9PL")
				.param("capacity", "-1")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/edit"))
				.andExpect(handler().methodName("updateVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void updateVenueInvalidNameTooLong() throws Exception {
		String longName = "a".repeat(256);

		mvc.perform(post("/venues/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", longName)
				.param("address", "Oxford Road M13 9PL")
				.param("capacity", "150")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/edit"))
				.andExpect(handler().methodName("updateVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void updateVenueInvalidAddressTooLong() throws Exception {
		String longAddress = "a".repeat(293) + " M13 9PL";

		mvc.perform(post("/venues/1")
				.with(user("admin").roles("ADMINISTRATOR")).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Updated Kilburn")
				.param("address", longAddress)
				.param("capacity", "150")
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("venues/edit"))
				.andExpect(handler().methodName("updateVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void deleteVenueUnauthorised() throws Exception {
		mvc.perform(delete("/venues/1")
				.with(csrf())
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection());

		verify(venueService, never()).deleteById(1L);
	}
}