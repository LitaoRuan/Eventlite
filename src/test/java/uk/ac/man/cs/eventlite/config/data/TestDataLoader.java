package uk.ac.man.cs.eventlite.config.data;

import java.time.LocalDate;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@Configuration
@Profile("test")
public class TestDataLoader {

	private final static Logger log = LoggerFactory.getLogger(TestDataLoader.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			Venue v1 = new Venue();
			v1.setName("Kilburn Building");
			v1.setAddress("Oxford Road M13 9PL");
			v1.setCapacity(120);
			venueService.save(v1);

			Venue v2 = new Venue();
			v2.setName("Crawford Building");
			v2.setAddress("Oxford Road M13 9SS");
			v2.setCapacity(100);
			venueService.save(v2);

			Event e1 = new Event();
			e1.setName("Spring Boot Workshop");
			e1.setDate(LocalDate.of(2027, 3, 10));
			e1.setTime(LocalTime.of(14, 0));
			e1.setVenue(v1);
			eventService.save(e1);

			Event e2 = new Event();
			e2.setName("Software Engineering Talk");
			e2.setDate(LocalDate.of(2027, 4, 5));
			e2.setTime(LocalTime.of(10, 30));
			e2.setVenue(v2);
			eventService.save(e2);

			Event e3 = new Event();
			e3.setName("Manchester Tech Meetup");
			e3.setDate(LocalDate.of(2027, 5, 15));
			e3.setTime(LocalTime.of(18, 0));
			e3.setVenue(v1);
			eventService.save(e3);
		};
	}
}
