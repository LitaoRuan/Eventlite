package uk.ac.man.cs.eventlite.dao;

import java.util.List;

import uk.ac.man.cs.eventlite.entities.Event;

public interface EventService {

	public long count();

	public Event save(Event event);
	
	public Iterable<Event> findAll();
	
	public Iterable<Event> searchAll(String search_query);
	
	public Event findById(long id);
	
    public void update(long id, Event event);
    
    public void deleteById(long id);
    
    public List<Event> findNext3UpcomingEvents();
}

