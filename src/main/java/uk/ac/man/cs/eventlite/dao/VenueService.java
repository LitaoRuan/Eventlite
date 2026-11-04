package uk.ac.man.cs.eventlite.dao;

import java.util.List;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

public interface VenueService {

	public long count();

	public Iterable<Venue> findAll();
	
	public Iterable<Venue> searchAll(String search_query);
	
	public Venue save(Venue venue);
	
	public Venue findById(long id);
	
    public void update(long id, Venue venue);
    
    public void deleteById(long id);
	
    public List<Venue> findTop3VenuesByEventCount();
}
