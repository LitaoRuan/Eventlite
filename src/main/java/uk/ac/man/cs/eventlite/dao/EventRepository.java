package uk.ac.man.cs.eventlite.dao;

import org.springframework.data.repository.CrudRepository;

import uk.ac.man.cs.eventlite.entities.Event;

import java.time.LocalDate;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface EventRepository extends CrudRepository<Event, Long>{

    Iterable<Event> findAllByOrderByDateAscNameAsc();

    @Query(value = "SELECT * FROM events WHERE LOWER(name) REGEXP CONCAT('(^|[^a-z0-9])', LOWER(:query), '($|[^a-z0-9])') ORDER BY date ASC, name ASC", nativeQuery = true)
    Iterable<Event> findByNameContainingWordIgnoreCase(@Param("query") String query);
    

    Iterable<Event> findByVenueId(Long venueId);

    List<Event> findByVenueIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(Long id, LocalDate date, Pageable pageable);
    

}
