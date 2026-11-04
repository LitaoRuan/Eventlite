package uk.ac.man.cs.eventlite.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class SafeRedirectUtilsTest {

	// --- sanitizeReturnToOptional ---

	@Test
	public void nullCandidateReturnsNull() {
		assertNull(SafeRedirectUtils.sanitizeReturnToOptional(null));
	}

	@Test
	public void blankCandidateReturnsNull() {
		assertNull(SafeRedirectUtils.sanitizeReturnToOptional("   "));
	}

	@Test
	public void emptyCandidateReturnsNull() {
		assertNull(SafeRedirectUtils.sanitizeReturnToOptional(""));
	}

	@Test
	public void candidateNotStartingWithSlashReturnsNull() {
		assertNull(SafeRedirectUtils.sanitizeReturnToOptional("events"));
	}

	@Test
	public void candidateStartingWithDoubleSlashReturnsNull() {
		assertNull(SafeRedirectUtils.sanitizeReturnToOptional("//evil.com"));
	}

	@Test
	public void candidateContainingDotDotReturnsNull() {
		assertNull(SafeRedirectUtils.sanitizeReturnToOptional("/events/../admin"));
	}

	@Test
	public void candidateContainingCarriageReturnReturnsNull() {
		assertNull(SafeRedirectUtils.sanitizeReturnToOptional("/events\r/injected"));
	}

	@Test
	public void candidateContainingNewlineReturnsNull() {
		assertNull(SafeRedirectUtils.sanitizeReturnToOptional("/events\n/injected"));
	}

	@Test
	public void validRelativePathIsReturned() {
		assertEquals("/events", SafeRedirectUtils.sanitizeReturnToOptional("/events"));
	}

	@Test
	public void validPathWithQueryStringIsReturned() {
		assertEquals("/events?search=jazz", SafeRedirectUtils.sanitizeReturnToOptional("/events?search=jazz"));
	}

	@Test
	public void validPathIsTrimmed() {
		assertEquals("/events", SafeRedirectUtils.sanitizeReturnToOptional("  /events  "));
	}

	// --- sanitizeReturnTo ---

	@Test
	public void validCandidateReturnsCandidate() {
		assertEquals("/venues", SafeRedirectUtils.sanitizeReturnTo("/venues", "/events"));
	}

	@Test
	public void invalidCandidateReturnsDefault() {
		assertEquals("/events", SafeRedirectUtils.sanitizeReturnTo(null, "/events"));
	}

	@Test
	public void unsafeCandidateReturnsDefault() {
		assertEquals("/events", SafeRedirectUtils.sanitizeReturnTo("//evil.com", "/events"));
	}
}
