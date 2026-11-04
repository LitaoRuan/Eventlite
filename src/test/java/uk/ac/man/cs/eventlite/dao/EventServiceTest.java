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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import uk.ac.man.cs.eventlite.EventLite;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class EventServiceTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Venue savedVenue;

	@BeforeEach
	public void setUp() {
		deleteFromTables("events");
		deleteFromTables("venues");

		Venue v = new Venue();
		v.setName("Test Venue");
		v.setAddress("Oxford Road M13 9PL");
		v.setCapacity(100);
		savedVenue = venueService.save(v);
	}

	private Event buildFutureEvent(String name) {
		Event e = new Event();
		e.setName(name);
		e.setDate(LocalDate.now().plusDays(1));
		e.setTime(LocalTime.of(12, 0));
		e.setVenue(savedVenue);
		return e;
	}

	// --- count ---

	@Test
	public void countReturnsNumberOfEvents() {
		long before = eventService.count();
		eventService.save(buildFutureEvent("Count Test"));
		assertThat(eventService.count(), equalTo(before + 1));
	}

	// --- save / findAll ---

	@Test
	public void savedEventAppearsInFindAll() {
		eventService.save(buildFutureEvent("Saved Event"));
		Iterable<Event> all = eventService.findAll();
		assertThat(all, hasItem(hasProperty("name", equalTo("Saved Event"))));
	}

	@Test
	public void findAllReturnsEventsOrderedByDateThenName() {
		Event e1 = buildFutureEvent("B Event");
		e1.setDate(LocalDate.now().plusDays(2));
		Event e2 = buildFutureEvent("A Event");
		e2.setDate(LocalDate.now().plusDays(2));
		Event e3 = buildFutureEvent("C Event");
		e3.setDate(LocalDate.now().plusDays(1));
		eventService.save(e1);
		eventService.save(e2);
		eventService.save(e3);

		List<Event> all = (List<Event>) eventService.findAll();
		assertThat(all.get(0).getName(), equalTo("C Event"));
		assertThat(all.get(1).getName(), equalTo("A Event"));
		assertThat(all.get(2).getName(), equalTo("B Event"));
	}

	// --- findById ---

	@Test
	public void findByIdReturnsSavedEvent() {
		Event saved = eventService.save(buildFutureEvent("Find Me"));
		Event found = eventService.findById(saved.getId());
		assertNotNull(found);
		assertThat(found.getName(), equalTo("Find Me"));
	}

	@Test
	public void findByIdThrowsWhenNotFound() {
		assertThrows(EventNotFoundException.class, () -> eventService.findById(999999L));
	}

	// --- searchAll ---

	@Test
	public void searchAllReturnsMatchingEvents() {
		eventService.save(buildFutureEvent("Jazz Concert"));
		eventService.save(buildFutureEvent("Tech Talk"));
		Iterable<Event> results = eventService.searchAll("Jazz");
		assertThat(results, hasItem(hasProperty("name", equalTo("Jazz Concert"))));
		assertThat(results, not(hasItem(hasProperty("name", equalTo("Tech Talk")))));
	}

	@Test
	public void searchAllIsCaseInsensitive() {
		eventService.save(buildFutureEvent("Jazz Concert"));
		Iterable<Event> results = eventService.searchAll("jazz");
		assertThat(results, hasItem(hasProperty("name", equalTo("Jazz Concert"))));
	}

	@Test
	public void searchAllReturnsEmptyWhenNoMatch() {
		eventService.save(buildFutureEvent("Jazz Concert"));
		Iterable<Event> results = eventService.searchAll("xyznotexist");
		assertThat(results, iterableWithSize(0));
	}

	@Test
	public void searchAllMatchesWholeWordsOnly() {
		eventService.save(buildFutureEvent("Apple Event"));
		eventService.save(buildFutureEvent("AppleEvent"));
		Iterable<Event> results = eventService.searchAll("Apple");
		assertThat(results, hasItem(hasProperty("name", equalTo("Apple Event"))));
		assertThat(results, not(hasItem(hasProperty("name", equalTo("AppleEvent")))));
	}

	// --- update ---

	@Test
	public void updateChangesEventName() {
		Event saved = eventService.save(buildFutureEvent("Original Name"));
		Event updated = buildFutureEvent("Updated Name");
		eventService.update(saved.getId(), updated);
		Event found = eventService.findById(saved.getId());
		assertThat(found.getName(), equalTo("Updated Name"));
	}

	// --- deleteById ---

	@Test
	public void deleteByIdRemovesEvent() {
		Event saved = eventService.save(buildFutureEvent("To Delete"));
		long id = saved.getId();
		eventService.deleteById(id);
		assertThrows(EventNotFoundException.class, () -> eventService.findById(id));
	}

	// --- findNext3UpcomingEvents ---

	@Test
	public void findNext3UpcomingEventsReturnsOnlyFutureEvents() {
		eventService.save(buildFutureEvent("Future 1"));
		eventService.save(buildFutureEvent("Future 2"));
		List<Event> upcoming = eventService.findNext3UpcomingEvents();
		assertThat(upcoming.size(), equalTo(2));
	}

	@Test
	public void findNext3UpcomingEventsReturnsAtMost3() {
		eventService.save(buildFutureEvent("Future 1"));
		eventService.save(buildFutureEvent("Future 2"));
		eventService.save(buildFutureEvent("Future 3"));
		eventService.save(buildFutureEvent("Future 4"));
		List<Event> upcoming = eventService.findNext3UpcomingEvents();
		assertThat(upcoming.size(), equalTo(3));
	}

	@Test
	public void findNext3UpcomingEventsReturnsEmptyWhenNone() {
		List<Event> upcoming = eventService.findNext3UpcomingEvents();
		assertThat(upcoming.size(), equalTo(0));
	}

	@Test
	public void findNext3UpcomingEventsExcludesPastEvents() {
		// Insert a past event directly via JDBC to bypass @Future bean validation
		jdbcTemplate.update(
			"INSERT INTO events (id, name, date, time, venue_id) VALUES (?, ?, ?, ?, ?)",
			999999L, "Past Event", LocalDate.now().minusDays(1).toString(), "12:00:00", savedVenue.getId()
		);
		List<Event> upcoming = eventService.findNext3UpcomingEvents();
		assertThat(upcoming.size(), equalTo(0));
	}
}
