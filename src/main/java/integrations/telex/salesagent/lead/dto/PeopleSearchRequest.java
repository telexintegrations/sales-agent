package integrations.telex.salesagent.lead.dto;

import integrations.telex.salesagent.lead.service.LocationMappingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class PeopleSearchRequest {
    private String keyword;
    private String location;

    private transient LocationMappingService locationMappingService;

    public String buildLinkedInSearchUrl() {
        StringBuilder urlBuilder = new StringBuilder("https://www.linkedin.com/search/results/people/?");

        if (location != null && !location.isEmpty()) {
            String geoUrn = locationMappingService.getGeoUrnForLocation(location);
            if (geoUrn != null) {
                urlBuilder.append("geoUrn=%5B%22").append(geoUrn).append("%22%5D&");
            } else {
                // Handle case where location is not found in the mapping
                System.out.println("Location not found in mapping: " + location);
            }
        }

        urlBuilder.append("origin=FACETED_SEARCH");
        return urlBuilder.toString();
    }
}
