package uk.ac.man.cs.eventlite;

import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;


@SpringBootApplication
public class EventLite {

	public static void main(String[] args) {
		SpringApplication.run(EventLite.class, args);
	}

}
