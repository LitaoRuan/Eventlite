package uk.ac.man.cs.eventlite.dao;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;


import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;

@Service
@Transactional
public class EventServiceImpl implements EventService {
	
	@Autowired
	private EventRepository eventRepository;

	
	@Override
	public long count() {
		return eventRepository.count();
	}
	
	@Override
	public Event save(Event event) {
		return eventRepository.save(event);
	}

	@Override
	public Iterable<Event> findAll() {
		return eventRepository.findAllByOrderByDateAscNameAsc();
	}
	
	@Override
	public Iterable<Event> searchAll(String search_query) {
		return eventRepository.findByNameContainingWordIgnoreCase(search_query);
	}
	
	@Override
	public Event findById(long id) {
	    return eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
	}

	@Override
	public void update(long id, Event event) {
	    event.setId(id); 
	    eventRepository.save(event);
	}

	@Override
	public void deleteById(long id) {
	    eventRepository.deleteById(id);
	}
	
	@Override
	public List<Event> findNext3UpcomingEvents() {
	    LocalDateTime now = LocalDateTime.now();
	    List<Event> upcoming = new ArrayList<>();
	    for (Event event : eventRepository.findAllByOrderByDateAscNameAsc()) {
	        LocalDateTime eventDateTime = LocalDateTime.of(event.getDate(), event.getTime());
	        if (eventDateTime.isAfter(now)) {
	            upcoming.add(event);
	        }
	        if (upcoming.size() == 3) break;
	    }
	    return upcoming;
	}
}
