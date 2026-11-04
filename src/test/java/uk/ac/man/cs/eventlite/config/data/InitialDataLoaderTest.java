package uk.ac.man.cs.eventlite.config.data;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(MockitoExtension.class)
public class InitialDataLoaderTest {

	@Mock
	private EventService eventService;

	@Mock
	private VenueService venueService;

	@InjectMocks
	private InitialDataLoader initialDataLoader;

	@Test
	public void testInitDatabaseWhenEmpty() throws Exception {
		when(venueService.count()).thenReturn(0L);
		when(eventService.count()).thenReturn(0L);

		CommandLineRunner runner = initialDataLoader.initDatabase();
		runner.run();

		verify(venueService, times(2)).save(any(Venue.class));
		verify(eventService, times(2)).save(any(Event.class));
	}

	@Test
	public void testInitDatabaseWhenPopulated() throws Exception {
		when(venueService.count()).thenReturn(5L);
		when(eventService.count()).thenReturn(5L);

		CommandLineRunner runner = initialDataLoader.initDatabase();
		runner.run();

		verify(venueService, never()).save(any(Venue.class));
		verify(eventService, never()).save(any(Event.class));
	}
}