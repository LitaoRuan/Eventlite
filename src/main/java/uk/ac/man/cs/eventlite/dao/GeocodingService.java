package uk.ac.man.cs.eventlite.dao; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import retrofit2.Response;
import uk.ac.man.cs.eventlite.entities.Venue;
import java.util.List;

@Service
public class GeocodingService {

    private final static Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private final String MAPBOX_TOKEN = "YOUR_MAPBOX_PUBLIC_TOKEN";

    public void geocodeVenue(Venue venue) {
        if (venue.getAddress() == null || venue.getAddress().isEmpty()) {
            return;
        }

        try {
            MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
                    .accessToken(MAPBOX_TOKEN)
                    .query(venue.getAddress())
                    .build();

           
            Response<GeocodingResponse> response = mapboxGeocoding.executeCall();

            if (response.isSuccessful() && response.body() != null) {
                List<CarmenFeature> results = response.body().features();
                if (!results.isEmpty()) {
                    Point firstResultPoint = results.get(0).center();
                    venue.setLongitude(firstResultPoint.longitude());
                    venue.setLatitude(firstResultPoint.latitude());
                }
            }
        } catch (Exception e) {
            log.error("Mapbox Geocoding Failed: " + e.getMessage());
        }
    }
}