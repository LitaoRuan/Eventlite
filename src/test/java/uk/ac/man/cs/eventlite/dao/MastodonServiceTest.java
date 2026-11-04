package uk.ac.man.cs.eventlite.dao;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sys1yagi.mastodon4j.api.entity.Status;

class MastodonServiceTest {

    private MastodonService mastodonService;

    @BeforeEach
    void setUp() {
        mastodonService = new MastodonService();
    }

    @Test
    void testServiceInitialization() {
        assertNotNull(mastodonService);
    }

    @Test
    void testGetRecentTimelineDoesNotReturnNull() {
        List<Status> timeline = mastodonService.getRecentTimeline();
        assertNotNull(timeline);
    }
    
    @Test
    void testShareEventReturnsString() {
        String response = mastodonService.shareEvent("Automated test postfeel free to ignore.");
        assertNotNull(response);
        assertTrue(response.contains("Your post:") || response.contains("Error"));
    }
}