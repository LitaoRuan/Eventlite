package uk.ac.man.cs.eventlite.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PathVariable;

import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.dao.EventRepository;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Venue;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.dao.EventRepository;

import uk.ac.man.cs.eventlite.entities.Event;

@RestController
@RequestMapping(value = "/api/venues", produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
public class VenuesControllerApi {

    @Autowired
    private VenueService venueService;

    @Autowired
    private VenueModelAssembler venueAssembler;
    
    @Autowired
    private EventRepository eventRepository;

    
    @Autowired
    private EventModelAssembler eventAssembler;


    // GET /api/venues
    @GetMapping
    public CollectionModel<EntityModel<Venue>> getAllVenues() {
        return venueAssembler.toCollectionModel(venueService.findAll())
                .add(linkTo(methodOn(VenuesControllerApi.class).getAllVenues()).withSelfRel());
    }
    
    // GET/api/venues/{id}
    @GetMapping("/{id}")
    public EntityModel<Venue> getVenue(@PathVariable("id") Long id) {
        Venue venue = venueService.findById(id);

        if (venue == null) {
            throw new RuntimeException("Venue not found");
        }

        return venueAssembler.toModel(venue);
    }
    //Get/api/venues/{id}/events
    @GetMapping("/{id}/events")
    public Iterable<Event> getVenueEvents(@PathVariable("id") Long id) {
        return eventRepository.findByVenueId(id);
    }

    
    @GetMapping("/{id}/next3events")
    public CollectionModel<EntityModel<Event>> getNext3Events(@PathVariable("id") long id) {
    	
        List<Event> events = eventRepository.findByVenueIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(id, LocalDate.now(), PageRequest.of(0,3));

        return eventAssembler.toCollectionModel(events)
                .add(linkTo(methodOn(VenuesControllerApi.class).getNext3Events(id)).withSelfRel());
    }
}