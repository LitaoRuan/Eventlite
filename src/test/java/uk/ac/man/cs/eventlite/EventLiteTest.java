package uk.ac.man.cs.eventlite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // non default
class EventLiteTest {

    @Test
    void contextLoads() {
        // just tests if it does not crash
    }
}