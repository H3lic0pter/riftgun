package dev.riftgun.client;

import net.minecraft.network.chat.Component;

import java.util.List;

/** Normalizes legacy coordinate-note tooltip components after vanilla applies its lore style. */
public final class CoordinateNoteTooltipStyle {
    public static void removeItalics(List<Component> tooltip) {
        for (int index = 0; index < tooltip.size(); index++) {
            tooltip.set(index, tooltip.get(index).copy()
                .withStyle(style -> style.withItalic(false)));
        }
    }

    private CoordinateNoteTooltipStyle() {}
}
