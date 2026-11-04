package uk.ac.man.cs.eventlite.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.man.cs.eventlite.dao.MastodonService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; //share button

import jakarta.validation.Valid;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.web.SafeRedirectUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = "/events", produces = { MediaType.TEXT_HTML_VALUE })
public class EventsController {

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;
	
	@Autowired
	private MastodonService mastodonService;
	
	

	@ExceptionHandler(EventNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String eventNotFoundHandler(EventNotFoundException ex, Model model) {
		model.addAttribute("not_found_id", ex.getId());

		return "events/not_found";
	}

	// displays detailed information for a single event
	@GetMapping("/{id}")
	public String getEvent(@PathVariable("id") long id,
			@RequestParam(name = "returnTo", required = false) String returnTo,
			Model model) {

		Event event = eventService.findById(id);

		model.addAttribute("event", event);
		model.addAttribute("backUrl", SafeRedirectUtils.sanitizeReturnTo(returnTo, "/events"));
		model.addAttribute("venueReturnTo", "/events/" + id);

		return "events/detail";
	}

	// Default events page
	@GetMapping
	public String getAllEvents(@RequestParam(name = "search_query", required = false) String search_query,
			Model model) {
		// If no valid search then just order by date
		Iterable<Event> events;
		if (search_query == null || search_query.isEmpty()) { // <-- Safely check for empty string
			events = eventService.findAll();
			// If valid search then call search function
		} else {
			events = eventService.searchAll(search_query);
		}

		LocalDateTime today = LocalDateTime.now();

		List<Event> upcoming = new ArrayList<>();
		List<Event> previous = new ArrayList<>();

		for (Event event : events) {
			
			
			LocalTime safeTime = event.getTime() != null ? event.getTime() : LocalTime.MIDNIGHT;
			LocalDateTime datetime = LocalDateTime.of(event.getDate(), safeTime);

			if (datetime.isBefore(today)) {
				previous.add(event);
			}else {
				upcoming.add(event);
			}
		}

		model.addAttribute("upcoming", upcoming);
		model.addAttribute("previous", previous);

		// pass back in search query for search box persistence
		model.addAttribute("search_query", search_query);
		model.addAttribute("mastodonPosts", mastodonService.getRecentTimeline());

		return "events/index";
	}



	// Show add form
	@GetMapping("/new")
	public String newEvent(Model model) {
		model.addAttribute("event", new Event());
		model.addAttribute("venues", venueService.findAll());
		return "events/new";
	}

	// Submit add form
	@PostMapping
	public String createEvent(@Valid @ModelAttribute Event event, BindingResult result, Model model) {
		// Go back to the form, if form validation fails
		if (result.hasErrors()) {
			// Read venues so still shown
			model.addAttribute("venues", venueService.findAll());
			return "events/new";
		}
		eventService.save(event);
		return "redirect:/events";
	}

	// Show edit form (basically the same as show add form)
	@GetMapping("/{id}/edit")
	public String editEvent(@PathVariable("id") long id,
			@RequestParam(name = "returnTo", required = false) String returnTo,
			Model model) {
		model.addAttribute("event", eventService.findById(id));
		model.addAttribute("venues", venueService.findAll());
		model.addAttribute("cancelReturnTo", SafeRedirectUtils.sanitizeReturnToOptional(returnTo));
		return "events/edit";
	}

	// Submit edit form (basically the same as submit add form)
	@PatchMapping("/{id}")
	public String updateEvent(@PathVariable("id") long id, // Extract event ID from URL
			@Valid @ModelAttribute Event event, // Use data from form to create a Event object
			BindingResult result, // Store validation errors
			Model model,
			@RequestParam(name = "returnTo", required = false) String returnTo) {
		if (result.hasErrors()) {
			model.addAttribute("venues", venueService.findAll());
			model.addAttribute("cancelReturnTo", SafeRedirectUtils.sanitizeReturnToOptional(returnTo));
			return "events/edit";
		}


		eventService.update(id, event);
		return "redirect:/events";
	}

	// Delete
	@DeleteMapping("/{id}")
	public String deleteEvent(@PathVariable("id") long id) {
		eventService.deleteById(id);
		return "redirect:/events";
	}
	
	@PostMapping("/{id}/share")
	public String shareEvent(@PathVariable("id") long id, 
                             @RequestParam("message") String message, 
                             RedirectAttributes redirectAttributes) {
		
		String result = mastodonService.shareEvent(message);
		
		redirectAttributes.addFlashAttribute("ok_message", result);
		
		return "redirect:/events/" + id;
	}

}