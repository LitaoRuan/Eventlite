package uk.ac.man.cs.eventlite.config.data;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

//I have separated the files because testing with mapbox would mean that if this were in the same file everytime 
//the test is run mapbox would be requested for gps coordinates which isnt sensible

@ExtendWith(MockitoExtension.class)
public class TestDataLoaderTest {

	@Mock
	private EventService eventService;

	@Mock
	private VenueService venueService;

	@InjectMocks
	private TestDataLoader testDataLoader;

	@Test
	public void testInitDatabase() throws Exception {
		CommandLineRunner runner = testDataLoader.initDatabase();
		runner.run();

		verify(venueService, times(2)).save(any(Venue.class));
		verify(eventService, times(3)).save(any(Event.class));
	}
}