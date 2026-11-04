package uk.ac.man.cs.eventlite.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class Security {

	public static final String ADMIN_ROLE = "ADMINISTRATOR";

	// List the mappings/methods for which no authorisation is required.
	// By default we allow all GETs and full access to the H2 console.
	private static final PathPatternRequestMatcher.Builder mvc = PathPatternRequestMatcher.withDefaults();
	private static final RequestMatcher H2_CONSOLE = mvc.matcher("/h2-console/**");

	private static final RequestMatcher[] NO_AUTH = { mvc.matcher(HttpMethod.GET, "/webjars/**"),
			mvc.matcher(HttpMethod.GET, "/**"), H2_CONSOLE };

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(mvc.matcher("/events/new")).hasRole(ADMIN_ROLE)
						.requestMatchers(mvc.matcher("/events/*/edit")).hasRole(ADMIN_ROLE)
						.requestMatchers(HttpMethod.POST, "/events").hasRole(ADMIN_ROLE)          
						.requestMatchers(HttpMethod.PATCH, "/events/*").hasRole(ADMIN_ROLE)      
						.requestMatchers(HttpMethod.DELETE, "/events/*").hasRole(ADMIN_ROLE)     
						.requestMatchers(HttpMethod.POST, "/events/*/share").permitAll()   
						.requestMatchers(mvc.matcher("/venues/new")).hasRole(ADMIN_ROLE)
						.requestMatchers(mvc.matcher("/venues/*/edit")).hasRole(ADMIN_ROLE)
						.requestMatchers(NO_AUTH).permitAll()
						.anyRequest().hasRole(ADMIN_ROLE)
						)

				// This makes testing easier. Given we're not going into production, that's OK.
				.sessionManagement(session -> session.requireExplicitAuthenticationStrategy(false))

				// Use form login/logout for the Web.
				.formLogin(login -> login.loginPage("/sign-in").permitAll())
				.logout(logout -> logout.logoutUrl("/sign-out").logoutSuccessUrl("/").permitAll())

				// Use HTTP basic for the API.
				.httpBasic(withDefaults()).securityMatcher(mvc.matcher("/api/**"))

				// Only use CSRF for Web requests.
				// Disable CSRF for the API and H2 console.
				.csrf(csrf -> csrf.ignoringRequestMatchers(mvc.matcher("/api/**"), H2_CONSOLE))
				.securityMatcher(mvc.matcher("/**"))

				// Disable X-Frame-Options for the H2 console.
				.headers(headers -> headers.frameOptions(frameOpts -> frameOpts.disable()));

		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

		UserDetails rob = User.withUsername("Rob").password(encoder.encode("Haines")).roles(ADMIN_ROLE).build();
		UserDetails caroline = User.withUsername("Caroline").password(encoder.encode("Jay")).roles(ADMIN_ROLE).build();
		UserDetails duncan = User.withUsername("Duncan").password(encoder.encode("Hull")).roles(ADMIN_ROLE).build();
		UserDetails markel = User.withUsername("Markel").password(encoder.encode("Vigo")).roles(ADMIN_ROLE).build();
		UserDetails mustafa = User.withUsername("Mustafa").password(encoder.encode("Mustafa")).roles(ADMIN_ROLE)
				.build();
		UserDetails tom = User.withUsername("Tom").password(encoder.encode("Carroll")).roles(ADMIN_ROLE).build();

		return new InMemoryUserDetailsManager(rob, caroline, duncan, markel, mustafa, tom);
	}
}