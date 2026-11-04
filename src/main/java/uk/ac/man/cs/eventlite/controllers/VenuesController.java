package uk.ac.man.cs.eventlite.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;

import jakarta.validation.Valid;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.dao.GeocodingService; 
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.web.SafeRedirectUtils;

@Controller
@RequestMapping(value = "/venues", produces = { MediaType.TEXT_HTML_VALUE })
public class VenuesController {

    @Autowired
    private VenueService venueService;
    
    @Autowired
    private GeocodingService geocodingService;
    
    @ExceptionHandler(VenueNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String venueNotFoundHandler(VenueNotFoundException ex, Model model) {
        model.addAttribute("not_found_id", ex.getId());
        return "venues/not_found";
    }

    @GetMapping
    public String getAllVenues(@RequestParam(name = "search_query", required = false) String search_query,
            Model model) {
        
        Iterable<Venue> venues;
        if (search_query == null || search_query.isEmpty()) {
            venues = venueService.findAll();
        } else {
            venues = venueService.searchAll(search_query);
        }
        
        model.addAttribute("venues", venues);
        model.addAttribute("search_query", search_query);
        return "venues/index";
    }
    
    @GetMapping("/{id}")
    public String getVenue(@PathVariable("id") long id,
            @RequestParam(name = "returnTo", required = false) String returnTo,
            Model model) {
        Venue venue = venueService.findById(id);
        
        if (venue == null) {
            throw new VenueNotFoundException(id);
        }
        
        model.addAttribute("venue", venue);
        model.addAttribute("backUrl", SafeRedirectUtils.sanitizeReturnTo(returnTo, "/venues"));
        
        List<Event> upcomingEvents = venue.getEvents().stream()
            .filter(e -> {
                LocalTime time = e.getTime() != null ? e.getTime() : LocalTime.MIDNIGHT;
                return LocalDateTime.of(e.getDate(), time).isAfter(LocalDateTime.now());
            })
            .sorted(Comparator.comparing(Event::getDate)
                .thenComparing(e -> e.getTime() != null ? e.getTime() : LocalTime.MIDNIGHT))
            .collect(Collectors.toList());
        
        model.addAttribute("events", upcomingEvents);
        return "venues/detail";
    }

    @GetMapping("/new")
    public String newVenue(Model model) {
        model.addAttribute("venue", new Venue());
        return "venues/new";
    }

    @PostMapping
    public String createVenue(@Valid @ModelAttribute Venue venue, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "venues/new";
        }
        
        geocodingService.geocodeVenue(venue);
        
        venueService.save(venue);
        return "redirect:/venues";
    }
    
    @DeleteMapping("/{id}")
    public String deleteVenue(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            venueService.deleteById(id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete a venue that has events.");
            return "redirect:/venues/" + id;
        }
        return "redirect:/venues";
    }
    
    @GetMapping("/{id}/edit")
    public String editVenue(@PathVariable("id") long id,
            @RequestParam(name = "returnTo", required = false) String returnTo,
            Model model) {
     
        Venue venue = venueService.findById(id);
        
        if (venue == null) {
            throw new VenueNotFoundException(id);
        }
   
        model.addAttribute("venue", venue);
        model.addAttribute("cancelReturnTo", SafeRedirectUtils.sanitizeReturnToOptional(returnTo));
        return "venues/edit";
    }

    @PostMapping("/{id}")
    public String updateVenue(@PathVariable("id") long id, @Valid @ModelAttribute("venue") Venue venue, BindingResult result, Model model,
            @RequestParam(name = "returnTo", required = false) String returnTo) {
     
        if (result.hasErrors()) {
            model.addAttribute("cancelReturnTo", SafeRedirectUtils.sanitizeReturnToOptional(returnTo));
            return "venues/edit";
        }
        
        venue.setId(id);
        
        geocodingService.geocodeVenue(venue);

        venueService.save(venue);
        return "redirect:/venues";
    }
}