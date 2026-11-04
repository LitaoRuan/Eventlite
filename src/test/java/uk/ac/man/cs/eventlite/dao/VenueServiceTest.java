package uk.ac.man.cs.eventlite.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import uk.ac.man.cs.eventlite.EventLite;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class VenueServiceTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	private VenueService venueService;

	@Autowired
	private EventService eventService;

	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	public void setUp() {
		deleteFromTables("events");
		deleteFromTables("venues");
	}

	private Venue buildVenue(String name) {
		Venue v = new Venue();
		v.setName(name);
		v.setAddress("Oxford Road M13 9PL");
		v.setCapacity(100);
		return v;
	}

	private Event buildFutureEvent(String name, Venue venue) {
		Event e = new Event();
		e.setName(name);
		e.setDate(LocalDate.now().plusDays(1));
		e.setTime(LocalTime.of(12, 0));
		e.setVenue(venue);
		return e;
	}

	// --- count ---

	@Test
	public void countReturnsNumberOfVenues() {
		long before = venueService.count();
		venueService.save(buildVenue("Count Venue"));
		assertThat(venueService.count(), equalTo(before + 1));
	}

	// --- save / findAll ---

	@Test
	public void savedVenueAppearsInFindAll() {
		venueService.save(buildVenue("Saved Venue"));
		Iterable<Venue> all = venueService.findAll();
		assertThat(all, hasItem(hasProperty("name", equalTo("Saved Venue"))));
	}

	@Test
	public void findAllReturnsVenuesOrderedByName() {
		venueService.save(buildVenue("Zara Hall"));
		venueService.save(buildVenue("Apex Room"));
		venueService.save(buildVenue("Main Theatre"));

		List<Venue> all = (List<Venue>) venueService.findAll();
		assertThat(all.get(0).getName(), equalTo("Apex Room"));
		assertThat(all.get(1).getName(), equalTo("Main Theatre"));
		assertThat(all.get(2).getName(), equalTo("Zara Hall"));
	}

	// --- findById ---

	@Test
	public void findByIdReturnsSavedVenue() {
		Venue saved = venueService.save(buildVenue("Find Me Venue"));
		Venue found = venueService.findById(saved.getId());
		assertNotNull(found);
		assertThat(found.getName(), equalTo("Find Me Venue"));
	}

	@Test
	public void findByIdThrowsWhenNotFound() {
		assertThrows(VenueNotFoundException.class, () -> venueService.findById(999999L));
	}

	// --- searchAll ---

	@Test
	public void searchAllReturnsMatchingVenues() {
		venueService.save(buildVenue("Kilburn Building"));
		venueService.save(buildVenue("Crawford House"));
		Iterable<Venue> results = venueService.searchAll("Kilburn");
		assertThat(results, hasItem(hasProperty("name", equalTo("Kilburn Building"))));
		assertThat(results, not(hasItem(hasProperty("name", equalTo("Crawford House")))));
	}

	@Test
	public void searchAllIsCaseInsensitive() {
		venueService.save(buildVenue("Kilburn Building"));
		Iterable<Venue> results = venueService.searchAll("kilburn");
		assertThat(results, hasItem(hasProperty("name", equalTo("Kilburn Building"))));
	}

	@Test
	public void searchAllReturnsEmptyWhenNoMatch() {
		venueService.save(buildVenue("Kilburn Building"));
		Iterable<Venue> results = venueService.searchAll("xyznotexist");
		assertThat(results, iterableWithSize(0));
	}

	@Test
	public void searchAllMatchesWholeWordsOnly() {
		venueService.save(buildVenue("Apple Hall"));
		venueService.save(buildVenue("AppleHall"));
		Iterable<Venue> results = venueService.searchAll("Apple");
		assertThat(results, hasItem(hasProperty("name", equalTo("Apple Hall"))));
		assertThat(results, not(hasItem(hasProperty("name", equalTo("AppleHall")))));
	}

	// --- update ---

	@Test
	public void updateChangesVenueName() {
		Venue saved = venueService.save(buildVenue("Original Name"));
		Venue updated = buildVenue("Updated Name");
		venueService.update(saved.getId(), updated);
		Venue found = venueService.findById(saved.getId());
		assertThat(found.getName(), equalTo("Updated Name"));
	}

	// --- deleteById ---

	@Test
	public void deleteByIdRemovesVenue() {
		Venue saved = venueService.save(buildVenue("To Delete"));
		long id = saved.getId();
		venueService.deleteById(id);
		assertThrows(VenueNotFoundException.class, () -> venueService.findById(id));
	}

	@Test
	public void deleteByIdThrowsWhenVenueNotFound() {
		assertThrows(VenueNotFoundException.class, () -> venueService.deleteById(999999L));
	}

	@Test
	public void deleteByIdThrowsWhenVenueHasEvents() {
		Venue saved = venueService.save(buildVenue("Busy Venue"));
		eventService.save(buildFutureEvent("Attached Event", saved));
		entityManager.flush();
		entityManager.clear();
		assertThrows(IllegalStateException.class, () -> venueService.deleteById(saved.getId()));
	}

	// --- findTop3VenuesByEventCount ---

	@Test
	public void findTop3VenuesByEventCountReturnsEmptyWhenNoVenues() {
		entityManager.flush();
		entityManager.clear();
		List<Venue> top3 = venueService.findTop3VenuesByEventCount();
		assertThat(top3.size(), equalTo(0));
	}

	@Test
	public void findTop3VenuesByEventCountReturnsAtMost3() {
		Venue v1 = venueService.save(buildVenue("Venue 1"));
		Venue v2 = venueService.save(buildVenue("Venue 2"));
		Venue v3 = venueService.save(buildVenue("Venue 3"));
		Venue v4 = venueService.save(buildVenue("Venue 4"));
		eventService.save(buildFutureEvent("E1", v1));
		eventService.save(buildFutureEvent("E2", v2));
		eventService.save(buildFutureEvent("E3", v3));
		eventService.save(buildFutureEvent("E4", v4));
		entityManager.flush();
		entityManager.clear();
		List<Venue> top3 = venueService.findTop3VenuesByEventCount();
		assertThat(top3.size(), equalTo(3));
	}

	@Test
	public void findTop3VenuesByEventCountOrdersByMostEvents() {
		Venue v1 = venueService.save(buildVenue("Busy Venue"));
		Venue v2 = venueService.save(buildVenue("Quiet Venue"));
		eventService.save(buildFutureEvent("E1", v1));
		eventService.save(buildFutureEvent("E2", v1));
		eventService.save(buildFutureEvent("E3", v2));
		entityManager.flush();
		entityManager.clear();
		List<Venue> top3 = venueService.findTop3VenuesByEventCount();
		assertThat(top3.get(0).getName(), equalTo("Busy Venue"));
		assertThat(top3.get(1).getName(), equalTo("Quiet Venue"));
	}

	@Test
	public void findTop3VenuesByEventCountIgnoresVenuesWithNoEvents() {
		// 3 venues with events fill all 3 top spots, pushing the empty venue out
		Venue v1 = venueService.save(buildVenue("Busy Venue 1"));
		Venue v2 = venueService.save(buildVenue("Busy Venue 2"));
		Venue v3 = venueService.save(buildVenue("Busy Venue 3"));
		venueService.save(buildVenue("Empty Venue"));
		eventService.save(buildFutureEvent("E1", v1));
		eventService.save(buildFutureEvent("E2", v2));
		eventService.save(buildFutureEvent("E3", v3));
		entityManager.flush();
		entityManager.clear();
		List<Venue> top3 = venueService.findTop3VenuesByEventCount();
		assertThat(top3.size(), equalTo(3));
		assertThat(top3, not(hasItem(hasProperty("name", equalTo("Empty Venue")))));
	}
}
