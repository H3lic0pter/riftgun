package dev.riftgun.client.render;

import java.util.List;

public record PortalVisualOptions(
    String sectionTitleKey,
    String resetTooltipKey,
    List<PortalVisualOption> entries
) {
    public static final PortalVisualOptions NONE = new PortalVisualOptions("", "", List.of());

    public PortalVisualOptions {
        entries = List.copyOf(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void reset() {
        entries.forEach(PortalVisualOption::reset);
    }
}
