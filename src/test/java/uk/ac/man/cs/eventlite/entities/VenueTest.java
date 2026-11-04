package uk.ac.man.cs.eventlite.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class VenueTest {

	private static Validator validator;

	@BeforeAll
	public static void setUpValidator() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	private Venue validVenue() {
		Venue v = new Venue();
		v.setName("Kilburn Building");
		v.setAddress("Oxford Road M13 9PL");
		v.setCapacity(100);
		return v;
	}

	// --- Valid ---

	@Test
	public void validVenueHasNoViolations() {
		Set<ConstraintViolation<Venue>> violations = validator.validate(validVenue());
		assertTrue(violations.isEmpty());
	}

	// --- Name ---

	@Test
	public void nameCannotBeNull() {
		Venue v = validVenue();
		v.setName(null);
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void nameCannotBeBlank() {
		Venue v = validVenue();
		v.setName("");
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void nameCannotBeWhitespaceOnly() {
		Venue v = validVenue();
		v.setName("   ");
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void nameMustBeLessThan256Characters() {
		Venue v = validVenue();
		v.setName("a".repeat(256));
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void nameOf255CharactersIsValid() {
		Venue v = validVenue();
		v.setName("a".repeat(255));
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertTrue(violations.isEmpty());
	}

	// --- Address ---

	@Test
	public void addressCannotBeNull() {
		Venue v = validVenue();
		v.setAddress(null);
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void addressCannotBeBlank() {
		Venue v = validVenue();
		v.setAddress("");
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void addressWithoutUKPostcodeFails() {
		Venue v = validVenue();
		v.setAddress("Oxford Road");
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void addressWithValidUKPostcodeIsAccepted() {
		Venue v = validVenue();
		v.setAddress("123 Some Street M1 1AA");
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertTrue(violations.isEmpty());
	}

	@Test
	public void addressMustBeLessThan300Characters() {
		Venue v = validVenue();
		// 293 filler chars + " M13 9PL" = 301 total
		v.setAddress("a".repeat(293) + " M13 9PL");
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void addressOf299CharactersWithPostcodeIsValid() {
		Venue v = validVenue();
		// 291 filler chars + " M13 9PL" = 299 total
		v.setAddress("a".repeat(291) + " M13 9PL");
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertTrue(violations.isEmpty());
	}

	// --- Capacity ---

	@Test
	public void capacityCannotBeZero() {
		Venue v = validVenue();
		v.setCapacity(0);
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void capacityCannotBeNegative() {
		Venue v = validVenue();
		v.setCapacity(-1);
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertFalse(violations.isEmpty());
	}

	@Test
	public void capacityOfOneIsValid() {
		Venue v = validVenue();
		v.setCapacity(1);
		Set<ConstraintViolation<Venue>> violations = validator.validate(v);
		assertTrue(violations.isEmpty());
	}
}
