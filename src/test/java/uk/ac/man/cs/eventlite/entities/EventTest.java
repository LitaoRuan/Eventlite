package uk.ac.man.cs.eventlite.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class EventTest {

	private static Validator validator;

	@BeforeAll
	public static void setUpValidator() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	private Event validEvent() {
		Venue v = new Venue();
		v.setName("Kilburn Building");
		v.setAddress("Oxford Road M13 9PL");
		v.setCapacity(100);

		Event e = new Event();
		e.setName("Test Event");
		e.setDate(LocalDate.now().plusDays(1));
		e.setTime(LocalTime.of(12, 0));
		e.setVenue(v);
		return e;
	}

	// --- Valid ---

	@Test
	public void validEventHasNoViolations() {
		Set<ConstraintViolation<Event>> violations = validator.validate(validEvent());
		assertTrue(violations.isEmpty());
	}

	@Test
	public void validEventWithoutDescriptionHasNoViolations() {
		Event e = validEvent();
		e.setDescription(null);
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertTrue(violations.isEmpty());
	}

	@Test
	public void validEventWithEmptyDescriptionHasNoViolations() {
		Event e = validEvent();
		e.setDescription("");
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertTrue(violations.isEmpty());
	}

	@Test
	public void validEventWithoutTimeHasNoViolations() {
		Event e = validEvent();
		e.setTime(null);
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertTrue(violations.isEmpty());
	}

	// --- Name ---

	@Test
	public void nameCannotBeNull() {
		Event e = validEvent();
		e.setName(null);
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void nameCannotBeBlank() {
		Event e = validEvent();
		e.setName("");
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void nameCannotBeWhitespaceOnly() {
		Event e = validEvent();
		e.setName("   ");
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void nameMustBeLessThan256Characters() {
		Event e = validEvent();
		e.setName("a".repeat(256));
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void nameOf255CharactersIsValid() {
		Event e = validEvent();
		e.setName("a".repeat(255));
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertTrue(violations.isEmpty());
	}

	// --- Description (optional) ---

	@Test
	public void descriptionOf499CharactersIsValid() {
		Event e = validEvent();
		e.setDescription("a".repeat(499));
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertTrue(violations.isEmpty());
	}

	@Test
	public void descriptionMustBeLessThan500Characters() {
		Event e = validEvent();
		e.setDescription("a".repeat(500));
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertFalse(violations.isEmpty());
	}

	// --- Date ---

	@Test
	public void dateCannotBeNull() {
		Event e = validEvent();
		e.setDate(null);
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void dateMustBeInTheFuture() {
		Event e = validEvent();
		e.setDate(LocalDate.now().minusDays(1));
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void todaysDateIsNotValid() {
		Event e = validEvent();
		e.setDate(LocalDate.now());
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void tomorrowsDateIsValid() {
		Event e = validEvent();
		e.setDate(LocalDate.now().plusDays(1));
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertTrue(violations.isEmpty());
	}

	// --- Venue ---

	@Test
	public void venueCannotBeNull() {
		Event e = validEvent();
		e.setVenue(null);
		Set<ConstraintViolation<Event>> violations = validator.validate(e);
		assertFalse(violations.isEmpty());
	}
}
