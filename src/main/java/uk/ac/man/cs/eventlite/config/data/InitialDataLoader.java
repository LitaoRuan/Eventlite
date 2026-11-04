package uk.ac.man.cs.eventlite.config.data;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import retrofit2.Response;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@Configuration
@Profile("default")

public class InitialDataLoader {

	private final static Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

	// Mapbox token used to populate seeded venue coordinates.
	private static final String MAPBOX_TOKEN = "YOUR_MAPBOX_PUBLIC_TOKEN";

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	
	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			
			Venue v1 = null;
			Venue v2 = null;
			
			if (venueService.count() > 0) {
				log.info("Database already populated with venues. Skipping venue initialization.");
			} else {
				log.info("Seeding");

	            v1 = new Venue();
	            v1.setName("Kilburn Building");
	            v1.setAddress("Oxford Road M13 9PL");
	            v1.setCapacity(120);
	            populateCoordinates(v1);
	            venueService.save(v1);

	            v2 = new Venue();
	            v2.setName("Crawford Building");
	            v2.setAddress("Oxford Road M13 9SS");
	            v2.setCapacity(100);
	            populateCoordinates(v2);
	            venueService.save(v2);

	            log.info("Venue seeding complete.");
			}

			if (eventService.count() > 0) {
				log.info("Database already populated with events. Skipping event initialization.");
			} else {
				// Build and save initial events here.
				Event e1 = new Event();
				e1.setName("Spring Boot Workshop");
				e1.setDate(java.time.LocalDate.of(2026, 3, 10));
				e1.setTime(java.time.LocalTime.of(14, 0));
				e1.setVenue(v1);
				eventService.save(e1);

				Event e2 = new Event();
				e2.setName("Software Engineering Talk");
				e2.setDate(java.time.LocalDate.of(2026, 7, 5));
				e2.setTime(java.time.LocalTime.of(10, 30));
				e2.setVenue(v2);
				eventService.save(e2);

				log.info("Initial events loaded.");
			}
		};
	}

	private void populateCoordinates(Venue venue) {
		if (venue.getAddress() == null || venue.getAddress().isBlank()) {
			return;
		}
		try {
			MapboxGeocoding geocoding = MapboxGeocoding.builder()
					.accessToken(MAPBOX_TOKEN)
					.query(venue.getAddress())
					.build();

			Response<GeocodingResponse> response = geocoding.executeCall();
			if (!response.isSuccessful() || response.body() == null) {
				return;
			}

			List<CarmenFeature> results = response.body().features();
			if (results == null || results.isEmpty() || results.get(0).center() == null) {
				return;
			}

			Point point = results.get(0).center();
			venue.setLongitude(point.longitude());
			venue.setLatitude(point.latitude());
		} catch (IOException ex) {
			log.debug("Seed venue geocoding failed: {}", ex.getMessage());
		} catch (RuntimeException ex) {
			log.debug("Seed venue geocoding unexpected error: {}", ex.getMessage());
		}
	}
}
