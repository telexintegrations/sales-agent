package integrations.telex.salesagent.lead.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GoogleSearchResponse {
    private List<SearchItem> items;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class SearchItem {
        private String title;
        private String link;
        private String snippet;


    }
}
