package integrations.telex.salesagent.user.dto.request;

public record Setting(
        String label,
        String type,
        Boolean required,
        String defaultValue
) {
}
