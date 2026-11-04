package uk.ac.man.cs.eventlite.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;

@Controller
@RequestMapping(value = "/", produces = { MediaType.TEXT_HTML_VALUE })
public class HomeController {

    @Autowired
    private EventService eventService;

    @Autowired
    private VenueService venueService;

    @GetMapping
    public String home(Model model) {
        model.addAttribute("upcomingEvents", eventService.findNext3UpcomingEvents());
        model.addAttribute("topVenues", venueService.findTop3VenuesByEventCount());
        return "home";
    }
}
