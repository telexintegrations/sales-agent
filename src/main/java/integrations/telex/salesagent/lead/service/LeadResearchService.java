package integrations.telex.salesagent.lead.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.config.AppConfig;
import integrations.telex.salesagent.lead.dto.GoogleSearchResponse;
import integrations.telex.salesagent.lead.model.Lead;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.dto.request.TelexPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeadResearchService {
    private final RestClient restClient = RestClient.create();
    private final AppConfig appConfig;
    private final TelexClient telexClient;
    ObjectMapper objectMapper = new ObjectMapper();

    public void fetchLeadReport(Lead lead, String channelId) throws JsonProcessingException {
    String apiKey = appConfig.getGoogleApiKey();
    String searchEngineId =  appConfig.getGoogleSearchEngineId();
    String baseURL = appConfig.getGoogleUrl();
    String query = lead.getName() + " " + lead.getCompany();
    String url = baseURL + "?key=" + apiKey + "&cx=" + searchEngineId + "&q=" + query;

    GoogleSearchResponse searchResponse = restClient.get()
            .uri(url)
            .retrieve()
            .body(GoogleSearchResponse.class);

    String response  =  formatLeadReport(searchResponse, lead.getName());
    TelexPayload telexPayload = new TelexPayload("Research Analysis", "Sales Agent", "success", response);
    telexClient.sendToTelexChannel(channelId, objectMapper.writeValueAsString(telexPayload));
    }

    private String formatLeadReport(GoogleSearchResponse searchResponse, String leadName) {
        if (searchResponse == null || searchResponse.getItems() == null) {
            return "No results found.";
        }

        StringBuilder report = new StringBuilder();
        report.append("Google Research Report for").append(leadName).append("\n")
                .append("__________________________________________\n\n");

        List<GoogleSearchResponse.SearchItem> items = searchResponse.getItems();

        List<GoogleSearchResponse.SearchItem> linkedinProfiles = new ArrayList<>();
        List<GoogleSearchResponse.SearchItem> socialMedia = new ArrayList<>();
        List<GoogleSearchResponse.SearchItem> newsMentions = new ArrayList<>();
        List<GoogleSearchResponse.SearchItem> otherReferences = new ArrayList<>();

        for (GoogleSearchResponse.SearchItem item : items) {
            String link = item.getLink().toLowerCase();
            if (link.contains("linkedin.com/in/")) {
                linkedinProfiles.add(item);
            } else if (link.contains("twitter.com") || link.contains("github.com") || link.contains("x.com") || link.contains("instagram.com") ) {
                socialMedia.add(item);
            } else if (link.contains("technews") || link.contains("news")) {
                newsMentions.add(item);
            } else {
                otherReferences.add(item);
            }
        }

        appendCategory(report, "Professional Profiles", linkedinProfiles);
        appendCategory(report, "Social Media", socialMedia);
        appendCategory(report, "News Mentions", newsMentions);
        appendCategory(report, "Other References", otherReferences);
        return report.toString();
    }

    private void appendCategory(StringBuilder report, String category, List<GoogleSearchResponse.SearchItem> items) {
        if (!items.isEmpty()) {
            report.append(category).append(":\n");
            report.append("__________________________________________\n\n");
            for (GoogleSearchResponse.SearchItem item : items) {
                report.append("• ").append(item.getTitle()).append("\n");
                report.append(item.getLink()).append("\n");
                if (item.getSnippet() != null) {
                    report.append(item.getSnippet()).append("\n\n");
                }
            }
        }
    }
}