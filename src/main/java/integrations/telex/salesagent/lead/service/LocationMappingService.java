package integrations.telex.salesagent.lead.service;

import org.springframework.stereotype.Service;

import java.util.Map;

import static java.util.Map.entry;

@Service
public class LocationMappingService {
    private static final Map<String, String> LOCATION_TO_GEOURN = Map.of(
            "lagos", "104197452",
            "atlanta", "103644278",
            "new york", "100288700",
            "abuja", "101711968",
            "port harcourt", "114378074",
            "london", "90009496",
            "kaduna", "103668447"
    );

    public String getGeoUrnForLocation(String locationName) {
        if (locationName == null) return null;
        return LOCATION_TO_GEOURN.get(locationName.toLowerCase());
    }
}
