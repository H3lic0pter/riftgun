package dev.riftgun.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class CoordinateNoteTooltipStyleTest {
    @Test
    void removesLegacyItalicsWithoutChangingLoreColor() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("Coordinate Note"));
        tooltip.add(Component.literal("Position")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        CoordinateNoteTooltipStyle.removeItalics(tooltip);

        assertFalse(tooltip.get(0).getStyle().isItalic());
        assertFalse(tooltip.get(1).getStyle().isItalic());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GRAY),
            tooltip.get(1).getStyle().getColor());
    }
}
