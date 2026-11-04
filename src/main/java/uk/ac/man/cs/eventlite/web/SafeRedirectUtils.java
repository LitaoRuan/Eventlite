package uk.ac.man.cs.eventlite.web;

/**
 * Validates same-origin relative paths passed via query parameters (e.g. returnTo) to avoid open redirects.
 */
public final class SafeRedirectUtils {

	private SafeRedirectUtils() {
	}

	public static String sanitizeReturnTo(String candidate, String defaultPath) {
		String safe = sanitizeReturnToOptional(candidate);
		return safe != null ? safe : defaultPath;
	}

	/**
	 * @return a safe relative path, or {@code null} if missing or unsafe
	 */
	public static String sanitizeReturnToOptional(String candidate) {
		if (candidate == null || candidate.isBlank()) {
			return null;
		}
		String t = candidate.trim();
		if (!t.startsWith("/")) {
			return null;
		}
		if (t.startsWith("//")) {
			return null;
		}
		if (t.contains("..")) {
			return null;
		}
		if (t.indexOf('\r') >= 0 || t.indexOf('\n') >= 0) {
			return null;
		}
		return t;
	}
}
