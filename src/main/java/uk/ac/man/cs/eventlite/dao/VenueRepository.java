package uk.ac.man.cs.eventlite.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

public interface VenueRepository extends CrudRepository<Venue, Long>{

	Iterable<Venue> findAllByOrderByNameAsc();

	@Query(value = "SELECT * FROM venues WHERE LOWER(name) REGEXP CONCAT('(^|[^a-z0-9])', LOWER(:query), '($|[^a-z0-9])') ORDER BY name ASC", nativeQuery = true)
	Iterable<Venue> findByNameContainingWordIgnoreCase(@Param("query") String query);
}
