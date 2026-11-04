package uk.ac.man.cs.eventlite.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;

public class ContainerTest {

	@Test
	public void customizeUsesDefaultPortWhenEnvNotSet() {
		// EVENTLITE_PORT is not set in test environment, so NumberFormatException
		// branch is taken (System.getenv returns null → parseInt(null) throws NFE)
		Container container = new Container();
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);

		container.customize(factory);

		// Default port 8080 should be set
		verify(factory).setPort(8080);
	}
}
