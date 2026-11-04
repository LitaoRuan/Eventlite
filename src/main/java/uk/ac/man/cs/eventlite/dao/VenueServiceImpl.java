package uk.ac.man.cs.eventlite.dao;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

@Service
public class VenueServiceImpl implements VenueService {

	private final static Logger log = LoggerFactory.getLogger(VenueServiceImpl.class);

	@Autowired
	private VenueRepository venueRepository;

	@Override
	public long count() {
		return venueRepository.count();
	}

	@Override
	public Iterable<Venue> findAll() {
	    return venueRepository.findAllByOrderByNameAsc();
	}
	
	@Override
	public Iterable<Venue> searchAll(String search_query) {
		return venueRepository.findByNameContainingWordIgnoreCase(search_query);
	}

	@Override
	public Venue save(Venue venue) {
		return venueRepository.save(venue);
	}
	
	@Override
	public Venue findById(long id) {
	    return venueRepository.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
	}
	

	@Override
	public void update(long id, Venue venue) {
	    venue.setId(id); 
	    venueRepository.save(venue);
	}

	@Override
	public void deleteById(long id) {
	    Venue venue = venueRepository.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
	    // Cannot delete if venue has events
	    if (venue.getEvents() != null && !venue.getEvents().isEmpty()) {
	        throw new IllegalStateException("Cannot delete a venue that has events.");
	    }
	    venueRepository.deleteById(id);
	}
	
	@Override
	public List<Venue> findTop3VenuesByEventCount() {
	    // Get all venues and convert Iterable to List, cause Iterable is not able to stream
	    List<Venue> allVenues = new ArrayList<>();
	    venueRepository.findAll().forEach(allVenues::add);
	    
	    return allVenues.stream()
	        // Ignore venues with no events list
	        .filter(v -> v.getEvents() != null)
	        // Sort
	        .sorted((a, b) -> b.getEvents().size() - a.getEvents().size())
	        .limit(3)
	        .collect(Collectors.toList());
	}
}
